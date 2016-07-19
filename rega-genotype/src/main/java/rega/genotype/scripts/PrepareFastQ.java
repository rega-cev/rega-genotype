package rega.genotype.scripts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;

import rega.genotype.FileFormatException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;

public class PrepareFastQ {

	public static void main(String args[]){
		File f = new File(args[0]);
		File out = new File(args[1]);

		FileReader fr1;
		try {
		    PrintWriter saveFile = new PrintWriter(out);
	    	fr1 = new FileReader(f.getAbsolutePath());
	    	LineNumberReader lnr1 = new LineNumberReader(fr1);
		    while(true) {
		    	Sequence s1 = SequenceAlignment.readFastqFileSequence(lnr1, SequenceAlignment.SEQUENCE_DNA);
		    	if (s1 == null)
		    		break;

		    	saveFile.println("@" + s1.getName());
		    	saveFile.println(s1.getSequence());
		    	saveFile.println("+");
		    	saveFile.println(s1.getQuality());
		    }

			saveFile.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
