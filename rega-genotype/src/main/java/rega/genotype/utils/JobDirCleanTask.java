package rega.genotype.utils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import rega.genotype.ui.util.Settings;
import eu.webtoolkit.jwt.WDate;

public class JobDirCleanTask implements Job {
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		System.err.println("Cleaning job dirs.");
		
		Settings settings = Settings.getInstance();
		
		Date fileAgeLimit = (new WDate(new Date()).addMilliseconds(-Settings.getInstance().getMaxJobDirLifeTime())).getDate(); 
		
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
	
	public static void main(String [] args) throws JobExecutionException {
		JobDirCleanTask t = new JobDirCleanTask();
		t.execute(null);
		
	}
}
