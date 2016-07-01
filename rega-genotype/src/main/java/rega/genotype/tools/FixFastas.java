package rega.genotype.tools;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

import rega.genotype.FileFormatException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;

public class FixFastas {
	public static void main(String [] args) throws IOException, FileFormatException {
		LineNumberReader reader
        = new LineNumberReader
            (new InputStreamReader(new BufferedInputStream(new FileInputStream(args[0]))));
    
		for (;;) {
		    Sequence s = SequenceAlignment.readFastqFileSequence(reader, SequenceAlignment.SEQUENCE_DNA);
		    if (s != null) {
		    	System.out.println("<sequence name=\"" + s.getName() + "\">");
		    	System.out.println(s.getSequence());
		    	System.out.println("</sequence>");
		    } else 
		    	break;
		}
	}
}
