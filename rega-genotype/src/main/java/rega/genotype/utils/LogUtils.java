package rega.genotype.utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogUtils {
	private static String NGS_LOG_FILE_NAME = "ngs-log";
	private static String NGS_LOG_DEBUG_FILE_NAME = "ngs-log-debug";

	public static File getLogFile(File workDir) {
		return new File(workDir, NGS_LOG_FILE_NAME);
	}

	public static File getDebugLogFile(File workDir) {
		return new File(workDir, NGS_LOG_DEBUG_FILE_NAME);
	}

	public static Logger getLogger(File workDir) {
		String loggerName = getLogFile(workDir).getAbsolutePath();
		return Logger.getLogger(loggerName);
	}

	public static Logger createLogger(File workDir) {
		Logger logger = getLogger(workDir);
		FileHandler fh;  
		try {  
			fh = new FileHandler(getLogFile(workDir).getAbsolutePath());  
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();  
			fh.setFormatter(formatter);  
		} catch (SecurityException e) {  
			e.printStackTrace();
			return null;
		} catch (IOException e) {  
			e.printStackTrace();
			return null;
		}  

		return logger;
	}

	// TODO
	public static Logger createDebugLogger(File workDir) {
		Logger logger = getLogger(getDebugLogFile(workDir));
		FileHandler fh;  
		try {  
			fh = new FileHandler(getDebugLogFile(workDir).getAbsolutePath());  
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();  
			fh.setFormatter(formatter);  
		} catch (SecurityException e) {  
			e.printStackTrace();
			return null;
		} catch (IOException e) {  
			e.printStackTrace();
			return null;
		}  

		return logger;
	}

	public static Logger createRandomLogger(File logFile) {
		Logger logger = getLogger(logFile);
		FileHandler fh;  
		try {  
			fh = new FileHandler(logFile.getAbsolutePath());  
			logger.addHandler(fh);
			SimpleFormatter formatter = new SimpleFormatter();  
			fh.setFormatter(formatter);  
		} catch (SecurityException e) {  
			e.printStackTrace();
			return null;
		} catch (IOException e) {  
			e.printStackTrace();
			return null;
		}  

		return logger;
	}
}
