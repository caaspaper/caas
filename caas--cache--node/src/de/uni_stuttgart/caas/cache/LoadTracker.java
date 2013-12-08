package de.uni_stuttgart.caas.cache;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * On behalf of a CacheNode, keeps track of how many events occurred within the
 * last `k` milliseconds and based on this provides a metric that estimates the
 * current load/stress situation of a node.
 */
public class LoadTracker {

	private final long LIMIT;
	private final long WINDOW_SIZE_MS;

	/**
	 * List containing time stamps of the most recent queries to calculate the
	 * load on the cache node. Too old entries are removed during getLoad().
	 */
	private final LinkedBlockingQueue<Long> queryProcessTimes;

	LoadTracker(long limit, long windowSizeMs) {
		LIMIT = limit;
		WINDOW_SIZE_MS = windowSizeMs;

		queryProcessTimes = new LinkedBlockingQueue<>();
	}

	/**
	 * Calculates the approximate current load. A higher value means a higher
	 * load, and load values are normalized by the `limit` parameter set during
	 * construction.
	 * 
	 * @return a double representing the load
	 */
	public double getLoad() {
		final long currentTime = System.currentTimeMillis();
		while (!queryProcessTimes.isEmpty()) {
			if (currentTime - queryProcessTimes.peek() > WINDOW_SIZE_MS) {
				queryProcessTimes.poll();
			} else {
				break;
			}
		}
		return (double) queryProcessTimes.size() / LIMIT;
	}

	/** Record an event at the current time */
	public void addEvent() {
		queryProcessTimes.add(System.currentTimeMillis());
	}

	/**
	 * Clear all recorded events. This means the current load estimate becomes 0
	 * again, and only increases as more events come in.
	 */
	public void reset() {
		queryProcessTimes.clear();
	}
}
