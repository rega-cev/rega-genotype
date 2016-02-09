package rega.genotype.ui.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileUtil {

	public static String readFile(String path){
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
