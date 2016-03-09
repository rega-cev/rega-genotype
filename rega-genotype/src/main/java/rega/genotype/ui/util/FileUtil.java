package rega.genotype.ui.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

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
}
