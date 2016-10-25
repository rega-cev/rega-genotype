package rega.genotype.async;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import rega.genotype.ui.framework.async.LongJobsScheduler;
import rega.genotype.ui.framework.async.LongJobsScheduler.Lock;

public class SchdulerTest  extends TestCase {

	String workingThreadName = null;

	private Thread addThread(final String text, final LongJobsScheduler scheduler) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				Lock jobLock = scheduler.getJobLock();

				assertNull(workingThreadName);
				workingThreadName = text;

				// do some work
				for (int i = 0; i < 100000; ++i) {
					System.out.println(text + " " + i + " | ");
				}
				workingThreadName = null;
				jobLock.release();
			}
		});
		t.setName(text);
		t.start();
		return t;
	}
	@Test
	public void testSchduler() {
		LongJobsScheduler scheduler = new LongJobsScheduler();
		List<Thread> threads = new ArrayList<Thread>();
		for (int i = 0; i < 10; ++i){
			Thread thread = addThread("t." + i, scheduler);
			threads.add(thread);
		}

		for (Thread t:threads)
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

}
