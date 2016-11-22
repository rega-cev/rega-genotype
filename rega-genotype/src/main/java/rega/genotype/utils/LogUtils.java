package rega.genotype.utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogUtils {
	public static Logger createLogger(File logFile, String loggerName) {
		Logger logger = Logger.getLogger("MyLog");  
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
