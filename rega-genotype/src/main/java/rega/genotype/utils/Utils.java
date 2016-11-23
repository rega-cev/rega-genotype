package rega.genotype.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import rega.genotype.ApplicationException;
import eu.webtoolkit.jwt.WWidget;

public class Utils {
	public static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	public static void removeSpellCheck(WWidget w) {
		w.setAttributeValue("autocomplete", "off");
		w.setAttributeValue("autocorrect", "off");
		w.setAttributeValue("autocapitalize", "off");
		w.setAttributeValue("spellcheck", "false");
	}

	public static boolean equal(Object o1, Object o2){
		if (o1 == null && o2 == null)
			return true;
		if (o1 == null || o2 == null)
			return false;
		return o1.equals(o2);
	}

	public static void executeCmd(String cmd, File workDir) throws ApplicationException{
		executeCmd(cmd, workDir, "process exited with error: ");
	}

	public static void executeCmd(String cmd, File workDir, String errorPrefix) throws ApplicationException{
		System.err.println(cmd);
		Process p = null;

		try {
			p = StreamReaderRuntime.exec(cmd, null, workDir);
			int exitResult = p.waitFor();

			// Clear in buff to avoid dead lock in Java, see: http://www.javaworld.com/article/2071275/core-java/when-runtime-exec---won-t.html
			BufferedReader inReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String inLine;
			while ((inLine = inReader.readLine()) != null)
				System.out.println(inLine);

			BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			String errLine;
			while ((errLine = errReader.readLine()) != null)
				System.out.println(errLine);

			if (exitResult != 0) {
				throw new ApplicationException(errorPrefix + exitResult);
			}
		} catch (IOException e) {
			throw new ApplicationException(errorPrefix + e.getMessage(), e);
		} catch (InterruptedException e) {
			if (p != null)
				p.destroy();
			throw new ApplicationException(errorPrefix + e.getMessage(), e);
		}
	}
}