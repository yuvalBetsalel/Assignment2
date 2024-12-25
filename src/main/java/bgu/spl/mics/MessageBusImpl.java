package bgu.spl.mics;

import org.w3c.dom.Node;
import sun.jvm.hotspot.opto.Node_List;

import java.util.LinkedList;
import java.util.List;
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
	private ConcurrentHashMap<MicroService, BlockingQueue<Message>> serviceMap;
	private ConcurrentHashMap<Class<? extends Event>, List<MicroService>> eventSubscribers;
	private ConcurrentHashMap<Class<? extends Broadcast>, List<MicroService>> broadcastSubscribers;
	private ConcurrentHashMap<Class<? extends Event>, AtomicInteger> eventIndex; //AtomicInteger is thread safe
	private ConcurrentHashMap<Event, Future> futureEvents;

	private MessageBusImpl(){
		serviceMap = new ConcurrentHashMap<>();
		eventSubscribers = new ConcurrentHashMap<>();
		broadcastSubscribers = new ConcurrentHashMap<>();
		eventIndex = new ConcurrentHashMap<>();
		futureEvents = new ConcurrentHashMap<>();
	}

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		if (eventSubscribers.containsKey(type)){
			eventSubscribers.get(type).add(m);
		}
		else{
			List<MicroService> newType = new LinkedList<>();
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
			List<MicroService> newType = new LinkedList<>();
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
			serviceMap.get(m).add(b);
		}
	}

	
	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		{
			List<MicroService> subscribers = eventSubscribers.get(e.getClass());
			synchronized (subscribers) {
				if (subscribers == null || subscribers.isEmpty())  //If there is no suitable Micro-Service
					return null;
				AtomicInteger index = eventIndex.get(e.getClass());  //Get last used index from subscribes list
				int subIndex = index.getAndUpdate(i -> (i + 1) % subscribers.size());  //Get the curr index (int) and update next atomic index
				serviceMap.get(subscribers.get(subIndex)).add(e);  //Add the event to the m queue by curr index
			}
			Future<T> future = new Future<>();
			futureEvents.put(e, future);
			return future;
		}
	}

	@Override
	public void register(MicroService m) {
		BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
		serviceMap.put(m, queue);


	}

	@Override
	public void unregister(MicroService m) {
		// TODO Auto-generated method stub

	}

	@Override
	public Message awaitMessage(MicroService m) throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	

}
