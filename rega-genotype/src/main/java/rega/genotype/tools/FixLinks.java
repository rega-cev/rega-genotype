package rega.genotype.tools;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import rega.genotype.utils.Table;

public class FixLinks {
	public static void main(String [] args) throws FileNotFoundException, UnsupportedEncodingException {
		Table t = Table.readTable(args[0]);

		String url = "<a href=\"http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&amp;db=Nucleotide&amp;list_uids=${number}&amp;dopt=GenBank\">${gi}</a>";
		
		for (int i = 1; i < t.numRows(); i++) {
			String gi = t.valueAt(3, i);
			String number = gi.split("\\|")[1];
			
			String value = url
				.replace("${number}", number)
				.replace("${gi}", gi);
			t.setValue(3, i, value);
		}
		
		t.exportAsCsv(System.out);
	}
}
