package rega.genotype.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;

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

	public static void execShellCmd(String cmd) throws IOException, InterruptedException, ApplicationException {
		String[] shellCmd = {"/bin/sh", "-c", cmd};
		System.err.println(cmd);

		Process fetchFasta = null;
		fetchFasta = Runtime.getRuntime().exec(shellCmd);
		int exitResult = fetchFasta.waitFor();
		if (exitResult != 0){
			throw new ApplicationException("fetchFasta exited with error: " + exitResult);
		}
	}

	/**
	 * Download from url and save result in file same as linox wget.
	 * return true if file was downloaded
	 */
	public static boolean wget(String url, File out) {
		RandomAccessFile fos = null;

		try {
			URLConnection conn = new URL(url).openConnection();

			out.getParentFile().mkdirs();
			out.delete();
			out.createNewFile();

			fos = new RandomAccessFile(out, "rw");

			final int BUF_SIZE = 4 * 1024;
			byte[] bytes = new byte[BUF_SIZE];
			int read = 0;

			BufferedInputStream binaryreader = new BufferedInputStream(conn.getInputStream());

			while ((read = binaryreader.read(bytes)) > 0)
				fos.write(bytes, 0, read);

			binaryreader.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
}