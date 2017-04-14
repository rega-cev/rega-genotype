package rega.genotype.ui.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;

import rega.genotype.Constants;
import rega.genotype.config.Config;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;

public class TestUtils {
	public static File setup(String fastaContent) {
		File jobDir;
		do {
			jobDir = new File(System.getProperty("java.io.tmpdir") + File.separatorChar +"jobDir" + File.separatorChar + System.currentTimeMillis());
			System.err.println(jobDir.getAbsolutePath());
		} while (jobDir.exists());
		jobDir.mkdirs();
		
		File fasta = getFastaFile(jobDir);
		
		try {
			FileUtil.writeStringToFile(fasta, fastaContent);
		} catch (IOException e) {
			TestCase.fail("Could not write fasta String to fasta file: " + e.getMessage());
		}
		
		Settings.initSettings(Settings.getInstance(null, true));

		// read global config from base-work-dir:
		// Global config contains paths to used software and that should be the same 
		// for unit test and the real program.
		File configFile = new File("./base-work-dir/config.json");
		if (configFile.exists()) {
			String json = FileUtil.readFile(configFile);
			Config config = Config.parseJson(json);
			Settings.getInstance().getConfig().setGeneralConfig(config.getGeneralConfig());
		} else {
			throw new RuntimeException("./base-work-dir/config.json file could not be found. If you base-work-dir in in an other place" +
					"you may copy the config file to ./base-work-dir/ . Note: only the global config is needed.");
		}
		
		return jobDir;
	}

	public static File setup() {
		File jobDir;
		do {
			jobDir = new File(System.getProperty("java.io.tmpdir") + File.separatorChar +"jobDir" + File.separatorChar + System.currentTimeMillis());
			System.err.println(jobDir.getAbsolutePath());
		} while (jobDir.exists());
		jobDir.mkdirs();

		Settings.initSettings(Settings.getInstance(null, true));

		return jobDir;
	}

	public static File getFastaFile(File jobDir) {
		return new File(jobDir.getAbsolutePath() + File.separatorChar + Constants.SEQUENCES_FILE_NAME);
	}

	public static File getResultFile(File jobDir) {
		return new File(jobDir.getAbsolutePath() + File.separatorChar + Constants.RESULT_FILE_NAME);
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
