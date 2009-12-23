package rega.genotype.utils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import rega.genotype.ui.util.Settings;

public class JobDirCleanTask implements Job {
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		System.err.println("Cleaning job dirs.");
		
		Settings settings = Settings.getInstance();
		
		Date fileAgeLimit = addMilliseconds(new Date(), -Settings.getInstance().getMaxJobDirLifeTime());
		
		for (File jobDir : settings.getJobDirs()) 
			processJobDir(jobDir, fileAgeLimit);			
		
		System.err.println("Finished cleaning job dirs.");
	}

	private void processJobDir(File jobDir, Date fileAgeLimit) {
		if (!jobDir.exists())
			return;
		
		for (File job : jobDir.listFiles()) {
			if (!job.isDirectory()) 
				continue;
			
			File resultFile = new File(job.getAbsolutePath() + File.separatorChar + "result.xml");
			if (resultFile.exists() && resultFile.lastModified() < fileAgeLimit.getTime()) {
				try {
					FileUtils.deleteDirectory(job);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	//TODO
	//use wdate
	private Date addMilliseconds(Date d, int nMilliseconds) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		c.add(Calendar.MILLISECOND, (int)nMilliseconds);
		return c.getTime();
	}
	
	public static void main(String [] args) throws JobExecutionException {
		JobDirCleanTask t = new JobDirCleanTask();
		t.execute(null);
		
	}
}
