package bgu.spl.mics;

//import org.w3c.dom.Node;
//import sun.jvm.hotspot.opto.Node_List;

//import java.util.LinkedList;
//import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The {@link MessageBusImpl class is the implementation of the MessageBus interface.
 * Write your implementation here!
 * Only one public method (in addition to getters which can be public solely for unit testing) may be added to this class
 * All other methods and members you add the class must be private.
 */
public class MessageBusImpl implements MessageBus {
	private static class MessageBusHolder {
		private static MessageBusImpl instance = new MessageBusImpl();
	}
	private ConcurrentHashMap<MicroService, BlockingQueue<Message>> serviceMap;
	private ConcurrentHashMap<Class<? extends Event>, BlockingQueue<MicroService>> eventSubscribers;  //done
	private ConcurrentHashMap<Class<? extends Broadcast>, BlockingQueue<MicroService>> broadcastSubscribers; //done
	private ConcurrentHashMap<Class<? extends Event>, AtomicInteger> eventIndex; //AtomicInteger is thread safe
	private ConcurrentHashMap<Event, Future> futureEvents;

	private MessageBusImpl() {
		serviceMap = new ConcurrentHashMap<>();
		eventSubscribers = new ConcurrentHashMap<>();
		broadcastSubscribers = new ConcurrentHashMap<>();
		eventIndex = new ConcurrentHashMap<>();
		futureEvents = new ConcurrentHashMap<>();
	}

	public static MessageBusImpl getInstance() {
		return MessageBusHolder.instance;
	}

	public ConcurrentHashMap<MicroService, BlockingQueue<Message>> getServiceMap() {
		return serviceMap;
	}

	public ConcurrentHashMap<Class<? extends Broadcast>, BlockingQueue<MicroService>> getBroadcastSubscribers() {
		return broadcastSubscribers;
	}

	public ConcurrentHashMap<Class<? extends Event>, BlockingQueue<MicroService>> getEventSubscribers() {
		return eventSubscribers;
	}
	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		synchronized (eventSubscribers) { //synchronized (with itself) so 2 threads will not make new lists for same type
			eventSubscribers.computeIfAbsent(type, k-> new LinkedBlockingQueue<>()).add(m);
			eventIndex.put(type, new AtomicInteger(0));
		}
	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		synchronized (broadcastSubscribers) {
			if (broadcastSubscribers.containsKey(type)) {
				broadcastSubscribers.get(type).add(m);
			} else {
				BlockingQueue<MicroService> newType = new LinkedBlockingQueue<>();
				newType.add(m);
				broadcastSubscribers.put(type, newType);
			}
		}
	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		Future<T> currFuture = futureEvents.get(e);
		currFuture.resolve(result); //resolved according to the result given as a parameter
	}

	@Override
	public void sendBroadcast(Broadcast b) {
		BlockingQueue<MicroService> subscribers = broadcastSubscribers.get(b.getClass());
		synchronized (subscribers) { //synchronized with unregister
			for (MicroService m : subscribers) {
				if(serviceMap.get(m) != null)
					serviceMap.get(m).add(b);
			}
		}
	}

	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		BlockingQueue<MicroService> subscribers = eventSubscribers.get(e.getClass());
		synchronized (subscribers) {  //synchronized only with unregister
			if (subscribers == null || subscribers.isEmpty())  //If there is no suitable Micro-Service
				return null;
			MicroService m = subscribers.poll();
			if(m == null){
				return null;
			}
			subscribers.add(m);
			serviceMap.get(m).add(e); //Add the event to the m queue by curr index
		}
		Future<T> future = new Future<>();
		futureEvents.put(e, future);
		return future;
	}


	@Override
	public void register(MicroService m) {
		BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
		serviceMap.put(m, messageQueue);
		synchronized (m){
			m.notifyAll();
		}
	}

	@Override
	public void unregister(MicroService m) { //should be all synchronized?
		synchronized (m) {
			serviceMap.remove(m);  //deletes the broadcast queue
		}
		//delete m in eventSubscribers
		for (BlockingQueue<MicroService> subscribers : eventSubscribers.values()) {
			synchronized (subscribers) {  //synchronized only with send event
				if (subscribers.contains(m)) {
					subscribers.remove(m);
				}
			}
		}
		//delete m in broadcastSubscribers
		for (BlockingQueue<MicroService> subscribers : broadcastSubscribers.values()) {
			synchronized (subscribers) {
				if (subscribers.contains(m)) {
					subscribers.remove(m);
				}
			}
		}
	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		try {
			return serviceMap.get(m).take();
		}catch (InterruptedException e){
			Thread.currentThread().interrupt();
			throw e;
		}
	}
}