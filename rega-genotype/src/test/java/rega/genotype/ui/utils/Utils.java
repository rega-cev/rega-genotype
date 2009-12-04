package rega.genotype.ui.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.util.Settings;

public class Utils {
	public static File setup(String fastaContent) {
		File jobDir = null;
		do {
			try {
				jobDir = File.createTempFile("test-rega-genotype" + System.currentTimeMillis(), "dir");
				jobDir.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.err.println(jobDir.getAbsolutePath());
		} while (jobDir.exists());
		jobDir.mkdirs();
		
		File fasta = getFastaFile(jobDir);
		
		try {
			GenotypeLib.writeStringToFile(fasta, fastaContent);
		} catch (IOException e) {
			TestCase.fail("Could not write fasta String to fasta file: " + e.getMessage());
		}
		
		GenotypeLib.initSettings(Settings.getInstance());
		
		return jobDir;
	}
	
	public static File getFastaFile(File jobDir) {
		return new File(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta");
	}
	
	public static File getResultFile(File jobDir) {
		return new File(jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}
	
	public static void deleteJobDirs(List<File> jobDirs) {
        try {
        	for(File f : jobDirs)
        		FileUtils.deleteDirectory(f);
		} catch (IOException e) {
			TestCase.fail("Could not delete the jobDir directory");
		}
	}
}
