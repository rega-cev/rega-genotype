package rega.genotype.utils;

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
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public class FileUtil {

	public static void storeFile(File file, String dir) {
		try {
			if (file == null)
				return;
			Files.copy(file.toPath(), new File(dir).toPath());
		} catch (IOException e) {
			e.printStackTrace();
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

	public static String toString(BufferedReader reader) {
		StringBuilder builder = new StringBuilder();
		String aux = "";

		try {
			while ((aux = reader.readLine()) != null) {
			    builder.append(aux);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return builder.toString();
	}

	public static String getFileExtension(File file) {
		String fileName = file.getName();
		if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
			return fileName.substring(fileName.lastIndexOf(".")+1);
		else 
			return "";
	}

	// zip 
	
	private static ZipFile toZipFile(final File zip) {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(zip.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return zipFile;
	}

	private static void closeZipFile(final ZipFile zip) {
		try {
			if (zip != null) 
				zip.close();
		} catch (IOException e) {
		}
	}


	public static String getFileContent(final File zip, String fileName) {
	    ZipFile zipFile = toZipFile(zip);

	    Enumeration<? extends ZipEntry> entries = zipFile.entries();

	    while(entries.hasMoreElements()){
	    	ZipEntry entry = entries.nextElement();
	    	if (fileName.equals(new File(entry.getName()).getName())){
	    		try {
	    			InputStream stream = zipFile.getInputStream(entry);
	    			return IOUtils.toString(stream); 
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    			return null;
	    		} finally {
	    			closeZipFile(zipFile);
	    		}
	    	}
	    }
	    closeZipFile(zipFile);
	    return null;
	}

	public static boolean isValidZip(final File zipFile) {
		ZipFile zipFile2 = toZipFile(zipFile);
		try {
			if (zipFile2 != null) 
				zipFile2.close();
		} catch (IOException e) {
		}
		return zipFile2 != null;
	}

	private static final int BUFFER = 2048;
	
	public static boolean unzip(File zipedFile, File extructFolder) {
		extructFolder.mkdirs();
		try {
			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(zipedFile);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			ZipEntry entry;
			while((entry = zis.getNextEntry()) != null) {
				int count;
				byte dataBytes[] = new byte[BUFFER];
				// write the files to the disk
				FileOutputStream fos = new FileOutputStream(
						extructFolder.getAbsolutePath() + File.separator + entry.getName());
				dest = new BufferedOutputStream(fos, BUFFER);
				while ((count = zis.read(dataBytes, 0, BUFFER)) != -1) {
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

	public static boolean zip(File srcDir, File destDir) {
		try {
			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(destDir);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			//out.setMethod(ZipOutputStream.DEFLATED);
			byte data[] = new byte[BUFFER];
			// get a list of files from current directory
			String files[] = srcDir.list();

			for (int i=0; i<files.length; i++) {
				FileInputStream fi = new FileInputStream(
						srcDir.getAbsolutePath() + File.separator + files[i]);
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
			return false;
		}
		return true;
	}
}
