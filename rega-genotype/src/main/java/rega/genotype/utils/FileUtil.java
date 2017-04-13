package rega.genotype.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.python.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class FileUtil {
	
	public static void writeStringToFile(File f, String s) throws IOException {
		writeStringToFile(f, s, false);
	}

	public static void writeStringToFile(File f, String s, boolean append) throws IOException {
		if (!f.exists()) {
			f.getParentFile().mkdirs();
			f.createNewFile();
		}

		PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(f.getAbsolutePath(), append)));
		out.println(s);
		out.close();
	}

	public static void appendToFile(File src, File destination) throws IOException {
		if (!destination.exists()) {
			destination.getParentFile().mkdirs();
			destination.createNewFile();
		}

		PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(destination.getAbsolutePath(), true)));
		LineNumberReader reader = new LineNumberReader(new FileReader(src));
		String line = null;
		while ((line = reader.readLine()) != null) {
			out.println(line);
		}
		reader.close();
		out.close();
	}
	
	public static String readFile(File path){
		String fileText = null;
		try {
			byte[] encoded = IOUtils.toByteArray(new FileInputStream(path));
			fileText = new String(encoded, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileText;
	}

	public static String toString(BufferedReader reader) throws IOException {
		return IOUtils.toString(reader);
	}

	public static String getFileExtension(File file) {
		String fileName = file.getName();
		if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
			return fileName.substring(fileName.lastIndexOf(".")+1);
		else 
			return "";
	}

	/**
	 * Same as JDK 7 Files.createTempDirectory 
	 * @return
	 * @throws IOException
	 */
	public static File createTempDirectory(String prefix, File folder) throws IOException {
		File file = new File(folder.getAbsolutePath() + File.separator + prefix + (int)(Math.random()*1000000000));
		if (!file.mkdirs())
			throw new IOException("Could not create file: " + file.getAbsolutePath());

		return file;
	}

	public static void moveDirRecorsively(File srcDir, File destDir) {
		destDir.mkdirs();
		moveDirContentRecorsively(srcDir, destDir);
		srcDir.delete();
	}

	public static void moveDirContentRecorsively(File srcDir, File destDir) {
		if(srcDir.isDirectory() && srcDir.listFiles() != null) {
		    File[] content = srcDir.listFiles();
		    for (File f: content) {
		    	File destFile = new File(destDir, f.getName());
		    	if (f.isDirectory()) {
		    		destFile.mkdirs();
		    		moveDirContentRecorsively(f, destFile);
		    	} else {
		    		try {
		    			FileUtils.moveFile(f, destFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
		    	}
		    }
		}
	}

	public static void copyDirContentRecorsively(File srcDir, File destDir) throws IOException {
		if(srcDir.isDirectory() && srcDir.listFiles() != null) {
		    File[] content = srcDir.listFiles();
		    for (File f: content) {
		    	File destFile = new File(destDir, f.getName());
		    	if (f.isDirectory()) {
		    		destFile.mkdirs();
		    		copyDirContentRecorsively(f, destFile);
		    	} else {
		    		FileUtils.copyFile(f, destFile);
		    	}
		    		
		    }
		}
	}

	public static boolean isSameFile(String path1, String path2) {
		return new File(path1).getAbsolutePath().equals(
				new File(path2).getAbsolutePath());
	}

	public static File find(File dir, String startWith, String endWith) {
		if (dir.listFiles() == null)
			return null;
		for (File f: dir.listFiles())
			if (f.getName().startsWith(startWith) 
					&& f.getName().endsWith(endWith))
				return f;

		return null;
	}

	public static String removeExtention(String fileName) {
		while (fileName.indexOf(".") > 0)
		    fileName = fileName.substring(0, fileName.lastIndexOf("."));

		return fileName;
	}

	public static File createTempDirectory() throws IOException {
		final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

		if(!temp.delete())
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());

		if(!temp.mkdir())
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());

		return temp;
	}

	// csv
	public static List<String[]> readCSV(File csvFile) {
		return readCSV(csvFile, ",");
	}

	public static List<String[]> readCSV(File csvFile, String delimiter) {
        BufferedReader br = null;
        String line = "";
        List<String[]> ans = new ArrayList<String[]>();
        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                ans.add(line.split(delimiter));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return ans;
	}

	// gzip
	/**
     * extract .gz file
     */
	public static boolean unGzip1File(File gzipFile, File destinationFile){
		byte[] buffer = new byte[1024];
		try{
			GZIPInputStream gzis =
					new GZIPInputStream(new FileInputStream(gzipFile));
			FileOutputStream out =
					new FileOutputStream(destinationFile);

			int len;
			while ((len = gzis.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}

			gzis.close();
			out.close();
		}catch(IOException e){
			e.printStackTrace();
			return false;
		}

		return true;
	}

	//bzip2
	/**
     * extract .bzip2 file
     */
	public static boolean unBzip2(File gzipFile, File destinationFile){
		try {
			FileInputStream in = new FileInputStream(gzipFile);
			FileOutputStream out = new FileOutputStream(destinationFile);
			BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(in);
			final byte[] buffer = new byte[BUFFER];
			int n = 0;
			while (-1 != (n = bzIn.read(buffer))) {
				out.write(buffer, 0, n);
			}
			out.close();
			bzIn.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return false;
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

	    if (zipFile == null)
	    	return null;

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

	public static boolean unzip1File(File zipedFile, File destination) {
		ZipFile zipFile = toZipFile(zipedFile);
		if (zipFile.size() != 1)
			return false;

		try {
			BufferedOutputStream dest = null;
			FileInputStream fis = new FileInputStream(zipedFile);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			while((zis.getNextEntry()) != null) {
				int count;
				byte dataBytes[] = new byte[BUFFER];
				// write the files to the disk
				FileOutputStream fos = new FileOutputStream(
						destination.getAbsolutePath());
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

	public static boolean zip(File srcDir, File destDir, String ignoreFile) {
		try {
			BufferedInputStream origin = null;
			FileOutputStream dest = new FileOutputStream(destDir);
			ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
			//out.setMethod(ZipOutputStream.DEFLATED);
			byte data[] = new byte[BUFFER];
			String files[] = srcDir.list();

			for (int i=0; i<files.length; i++) {
				if (files[i].equals(ignoreFile))
					continue;
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
