package rega.genotype.ui.framework.async;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * General Job scheduler. 
 * Execution order = FIFO.
 * Currently only 1 job can be executed at any time.
 * Used for long NGS jobs that need many resources.
 * Note: this is a singleton 
 * @see Lock
 * 
 * @author michael
 */
public class LongJobsScheduler {

	public static class Lock {
		private LongJobsScheduler scheduler;
		Lock(LongJobsScheduler scheduler) {
			this.scheduler = scheduler;
		}

		public void release() {
			scheduler.jobFinished(this);
		}
	}

	private static LongJobsScheduler instance = null;
	private ConcurrentLinkedQueue<Lock> jobQueue = new ConcurrentLinkedQueue<Lock>();
	private Object jobQueueLock = new Object();

	public LongJobsScheduler() {
		//should be instantiated only once in Settings.
		instance = this;
	}

	public static LongJobsScheduler getInstance() {
		return instance;
	}

	public Lock getJobLock() {
		Lock lock = new Lock(this);

		int queueSize;
		synchronized (jobQueueLock) {
			jobQueue.add(lock);
			queueSize = jobQueue.size();
		}

		if (queueSize > 1) // some other thread is running.
			synchronized (lock) {
				try {
					lock.wait();// wait till scheduler will release the lock.
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		return lock;
	}

	private void notifyNext() {
		if (jobQueue.size() > 0){
			Lock lock = jobQueue.peek();

			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}

	public void jobFinished(Lock lock) {
		synchronized (jobQueueLock) {
			jobQueue.remove(lock);
			notifyNext();
		}
	}
}