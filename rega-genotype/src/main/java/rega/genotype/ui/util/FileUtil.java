package rega.genotype.ui.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtil {

	public static void storeFile(File file, String dir) {
		try {
			if (file == null)
				return;
			Files.copy(file.toPath(), new File(dir).toPath());
		} catch (IOException e) {
			throw new RuntimeException("Could not copy uploaded file", e);
		}
	}

	public static void writeStringToFile(File f, String s) throws IOException {
		if (!f.exists()) {
			f.getParentFile().mkdirs();
			f.createNewFile();
		}

		Writer output = new BufferedWriter(new FileWriter(f));
		try {
			output.write( s );
		}
		finally {
			output.close();
		}
	}

	public static String readFile(File path){
		BufferedReader buffReader = null;
		String ans = "";
		try{
			buffReader = new BufferedReader (new FileReader(path));
			String line = buffReader.readLine();
			while(line != null) {
				ans += line + "\n";
				line = buffReader.readLine();
			}
		}catch(IOException ioe){
			ioe.printStackTrace();
		}finally{
			try{
				buffReader.close();
			}catch(IOException ioe1){
				//Leave It
			}
		}
		return ans;
	}

	// zip 
	
	public static boolean isValidZip(final File zipFile) {
		ZipFile zipfile = null;
		try {
			zipfile = new ZipFile(zipFile);
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (zipfile != null) {
					zipfile.close();
					zipfile = null;
				}
			} catch (IOException e) {
			}
		}
	}

	private static final int BUFFER = 2048;
	
	public static boolean unzip(String data) {
		try {
			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(data);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			while((entry = zis.getNextEntry()) != null) {
				System.out.println("Extracting: " +entry);
				int count;
				byte dataBytes[] = new byte[BUFFER];
				// write the files to the disk
				FileOutputStream fos = new 
						FileOutputStream(entry.getName());
				dest = new 
						BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(dataBytes, 0, BUFFER)) 
						!= -1) {
					dest.write(dataBytes, 0, count);
				}
				dest.flush();
				dest.close();
			}
			zis.close();
		} catch(Exception e) {
			e.printStackTrace();
			return true;
		}

		return false;
	}

	public static void zip(File srcDir, File destDir) {
		try {
			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(destDir);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			//out.setMethod(ZipOutputStream.DEFLATED);
			byte data[] = new byte[BUFFER];
			// get a list of files from current directory
			String files[] = srcDir.list();

			for (int i=0; i<files.length; i++) {
				System.out.println("Adding: "+files[i]);
				FileInputStream fi = new FileInputStream(files[i]);
				origin = new BufferedInputStream(fi, BUFFER);
				ZipEntry entry = new ZipEntry(files[i]);
				out.putNextEntry(entry);
				int count;
				while((count = origin.read(data, 0, BUFFER)) != -1) {
					out.write(data, 0, count);
				}
				origin.close();
			}
			out.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
