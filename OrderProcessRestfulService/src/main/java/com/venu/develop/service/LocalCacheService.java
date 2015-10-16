package com.venu.develop.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.LRUMap;
import org.springframework.stereotype.Repository;

/**
 * refer the LRUMap as introduced by 'Apache' which is a special type of cache
 * where we can mention the max map elements. The size selected should be optimal
 * as this memory becomes overhead on the hard ware resources if its too big. 
 * The thread (set as daemon thread) will run once for the time mentioned in the timerInterval;
 *  and identify if aparticular map element (cache obj) is over living by the time specified as
 * mentioned by 'timeToLive' and deletes those map elements from the cache. Max
 * elements are the allowed max cache size at any given point of time.
 * 
 * @param timeToLive --> 36000 sec
 * @param timerInterval --> 180 sec
 * @param maxItems  --> 10
 */

@Repository("lruCache")
public class LocalCacheService<K, T> {

	private long timeToLive;
	private LRUMap cacheMap;

	protected class CacheObject {
		public long lastAccessed = System.currentTimeMillis();
		public T value;

		protected CacheObject(T value) {
			this.value = value;
		}
	}

	/*
	 * base constructor
	 */
	public LocalCacheService() {
		this(36000, 180, 10);
	}

	/**
	 * overloaded constructor. 
	 * The daemon thread will run once for the time
	 * mentioned in the timerInterval. And identify if a particular map
	 * element(cache obj) is over living by the time specified as mentioned by
	 * 'timeToLive' and deletes those map elements from the cache. Max elements
	 * are the allowed max cache size at any given point of time.
	 * 
	 * @param timeToLive
	 *            --> 36000 sec
	 * @param timerInterval
	 *            --> 180 sec
	 * @param maxItems
	 *            --> 10
	 */
	public LocalCacheService(long timeToLive, final long timerInterval, int maxItems) {

		this.timeToLive = timeToLive * 1000;

		cacheMap = new LRUMap(maxItems);

		if (timeToLive > 0 && timerInterval > 0) {

			Thread t = new Thread(new Runnable() {
				public void run() {
					while (true) { // Always run. as this is a daemon thread
						try {
							Thread.sleep(timerInterval * 1000);
						} catch (InterruptedException ex) {
						}
						cleanup();
					}
				}
			});

			t.setDaemon(true);
			t.start();
		}
	}

	/*
	 * insert the Order object to the cache map
	 */
	public void put(K key, T value) {
		synchronized (cacheMap) {
			cacheMap.put(key, new CacheObject(value));
		}
	}

	/*
	 * retrieve the map element
	 */
	@SuppressWarnings("unchecked")
	public T get(K key) {
		synchronized (cacheMap) {
			CacheObject c = (CacheObject) cacheMap.get(key);

			if (c == null)
				return null;
			else {
				c.lastAccessed = System.currentTimeMillis();
				return c.value;
			}
		}
	}

	
	public void remove(K key) {
		synchronized (cacheMap) {
			cacheMap.remove(key);
		}
	}

	public int size() {
		synchronized (cacheMap) {
			return cacheMap.size();
		}
	}

	@SuppressWarnings("unchecked")
	public void cleanup() {

		long now = System.currentTimeMillis();
		ArrayList<K> deleteKey = null;

		synchronized (cacheMap) {
			MapIterator itr = cacheMap.mapIterator();

			deleteKey = new ArrayList<K>((cacheMap.size() / 2) + 1);
			K key = null;
			CacheObject c = null;

			while (itr.hasNext()) {
				key = (K) itr.next();
				c = (CacheObject) itr.getValue();

				if (c != null && (now > (timeToLive + c.lastAccessed))) {
					deleteKey.add(key);
				}
			}
		}

		for (K key : deleteKey) {
			synchronized (cacheMap) {
				cacheMap.remove(key);
			}

			Thread.yield();
		}
	}

	
	/*
	 * returns all the order id values in the cache
	 */
	@SuppressWarnings("unchecked")
	public List<K>  getAll() {

		Set<K> keys = new HashSet<K>();
		List<K> idList = new ArrayList<K>();
		synchronized (cacheMap) {
			if (cacheMap.size() == 0)
				System.out.println(" No elements to print....");
			else {
				System.out.println("printing ....Now size=" + cacheMap.size());
				keys = cacheMap.keySet();
				idList.addAll(keys);
			}

			return idList;
		}
	}
	
	
	
	
	/**
	 * used for debug the app.
	 * @param <V>
	 * 
	 */
	public  void printItems() {

		if (cacheMap.size() == 0)
			System.out.println(" No elements to print....");
		else {
			System.out.println("printing ....Now size=" + cacheMap.size());

			Iterator<K> entries = cacheMap.entrySet().iterator();
			while (entries.hasNext()) {
				Entry thisEntry = (Entry) entries.next();
				Object key = thisEntry.getKey();
				Object value = thisEntry.getValue();
				// System.out.println("key=" + key.toString() + "; value=" +
				// value.toString());
				System.out.print("  order id=" + key.toString());
			}
			System.out.println();
		}
	}
}
