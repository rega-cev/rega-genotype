package rega.genotype.framework.async;

import java.io.File;
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
		private File jobDir; // A unique identifier for the job. Used to tell the user where he is in queue.
		Lock(LongJobsScheduler scheduler, File jobDir) {
			this.scheduler = scheduler;
			this.jobDir = jobDir;
		}

		public void release() {
			scheduler.jobFinished(this);
		}

		public File getJobDir() {
			return jobDir;
		}

		public void setJobDir(File jobDir) {
			this.jobDir = jobDir;
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

	public Lock getJobLock(File jobDir) {
		Lock lock = new Lock(this, jobDir);

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

	public String getJobState(File jobDir) {
		int i = 0;
		for (Lock lock: jobQueue){
			if (lock.getJobDir().getAbsolutePath().equals(
					jobDir.getAbsolutePath()))
				if (i == jobQueue.size() - 1)
					return "Running.";
				else
					return "Witing for other jobs to finish. " + (jobQueue.size() - i -1) + " Jobs to go.";
			i++;
		}
		return "Not in queue: processing results.";
	}
}