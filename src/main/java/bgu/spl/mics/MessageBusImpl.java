package bgu.spl.mics;

//import org.w3c.dom.Node;
//import sun.jvm.hotspot.opto.Node_List;

//import java.util.LinkedList;
//import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only one public method (in addition to getters which can be public solely for unit testing) may be added to this class
 * All other methods and members you add the class must be private.
 */
public class MessageBusImpl implements MessageBus {
	private static MessageBusImpl instance;
	private ConcurrentHashMap<MicroService, BlockingQueue<Event>> eventServiceMap;
	private ConcurrentHashMap<MicroService, BlockingQueue<Broadcast>> broadcastServiceMap;
	private ConcurrentHashMap<Class<? extends Event>, CopyOnWriteArrayList<MicroService>> eventSubscribers;
	private ConcurrentHashMap<Class<? extends Broadcast>, CopyOnWriteArrayList <MicroService>> broadcastSubscribers;
	private ConcurrentHashMap<Class<? extends Event>, AtomicInteger> eventIndex; //AtomicInteger is thread safe
	private ConcurrentHashMap<Event, Future> futureEvents;


	private MessageBusImpl(){
		eventServiceMap = new ConcurrentHashMap<>();
		broadcastServiceMap = new ConcurrentHashMap<>();
		eventSubscribers = new ConcurrentHashMap<>();
		broadcastSubscribers = new ConcurrentHashMap<>();
		eventIndex = new ConcurrentHashMap<>();
		futureEvents = new ConcurrentHashMap<>();
	}

	public static synchronized MessageBusImpl getInstance() {
		if (instance==null)
			instance = new MessageBusImpl();
		return instance;
	}

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		if (eventSubscribers.containsKey(type)){
			eventSubscribers.get(type).add(m);
		}
		else{
			CopyOnWriteArrayList<MicroService> newType = new CopyOnWriteArrayList<>();
			newType.add(m);
			eventSubscribers.put(type,newType);
			eventIndex.put(type,new AtomicInteger(0));
		}

	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		if (broadcastSubscribers.containsKey(type)){
			broadcastSubscribers.get(type).add(m);
		}
		else{
			CopyOnWriteArrayList<MicroService> newType = new CopyOnWriteArrayList<>();
			newType.add(m);
			broadcastSubscribers.put(type,newType);
		}

	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		Future<T> currFuture = futureEvents.get(e);
		currFuture.resolve(result); //resolved according to the result given as a parameter
		//remove future and event from futureEvents?
	}

	@Override
	public void sendBroadcast(Broadcast b) {
		for (MicroService m : broadcastSubscribers.get(b.getClass())){
			broadcastServiceMap.get(m).add(b);
		}
	}

	
	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		CopyOnWriteArrayList<MicroService> subscribers;
		synchronized (eventSubscribers){
			subscribers = eventSubscribers.get(e.getClass());
				if (subscribers == null || subscribers.isEmpty())  //If there is no suitable Micro-Service
					return null;
			}
			AtomicInteger index = eventIndex.get(e.getClass());  //Get last used index from subscribes list
			int subIndex = index.getAndUpdate(i -> (i + 1) % subscribers.size());  //Get the curr index (int) and update next atomic index
			eventServiceMap.get(subscribers.get(subIndex)).add(e);  //Add the event to the m queue by curr index
			Future<T> future = new Future<>();
			futureEvents.put(e, future);
			return future;
	}


	@Override
	public void register(MicroService m) {
		BlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
		BlockingQueue<Broadcast> broadcastQueue = new LinkedBlockingQueue<>();
		eventServiceMap.put(m, eventQueue);
		broadcastServiceMap.put(m,broadcastQueue);
	}

	@Override
	public void unregister(MicroService m) {
		broadcastServiceMap.remove(m);  //deletes the broadcast queue
		synchronized (eventServiceMap) {
			if (!eventServiceMap.get(m).isEmpty()) {
				for (Event e : eventServiceMap.get(m)) {  //transfer the events to the related microservice
					sendEvent(e);
				}
			}
			eventServiceMap.remove(m);  //deletes the event queue
		}
		//delete m in eventSubscribers
		for (CopyOnWriteArrayList<MicroService> subscribers : eventSubscribers.values()) {
			synchronized (subscribers){  //should be before for?
				if (subscribers.contains(m)) {
					subscribers.remove(m);
				}
			}
		}
		//delete m in broadcastSubscribers
		for (CopyOnWriteArrayList<MicroService> subscribers : broadcastSubscribers.values()) {
			synchronized (subscribers) {
				if (subscribers.contains(m)) {
					subscribers.remove(m);
				}
			}
		}
	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		while (eventServiceMap.get(m).isEmpty() && broadcastServiceMap.get(m).isEmpty()) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
		if (!broadcastServiceMap.get(m).isEmpty()) {  //should be synchronized
			return broadcastServiceMap.get(m).take();
		}
		else
			return eventServiceMap.get(m).take();
	}
}
