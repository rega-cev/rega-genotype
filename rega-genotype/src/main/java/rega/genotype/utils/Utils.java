package rega.genotype.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class Utils {
	public static void writeStringToFile(File f, String s) throws IOException {
	      Writer output = new BufferedWriter(new FileWriter(f));
	      try {
	        output.write( s );
	      }
	      finally {
	        output.close();
	      }
	}
}
