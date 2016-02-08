package rega.genotype.tools;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.ApplicationException;
import rega.genotype.BlastAnalysis;
import rega.genotype.BlastAnalysis.BlastResults;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.Sequence;

public class ParseBlastResults {
	public static void main(String [] args) throws IOException, ParameterProblemException, FileFormatException, ApplicationException {
		if (args.length != 2) {
			fail();
		} else {
			String analysisFN = args[0];
			String blastResultsFN = args[1];
			run(new File(analysisFN), new File(blastResultsFN));
		}
	}
	
	private static class BlastResultList implements BlastResults {
		private Iterator<String> resultIterator;
		
		BlastResultList (List<String> results) {
			this.resultIterator = results.iterator();
		}
		public String[] next() throws ApplicationException {
			if (resultIterator.hasNext()) {
				String s = resultIterator.next();
				String []values = s.split("\t");
				if (values.length != 12)
					throw new RuntimeException("Invalid blast result: \"" + values + "\"");
				return values;
			}
			else
				return null;
		}
	}
	
	private static void run(File analysis, File blastResults) throws IOException, ParameterProblemException, FileFormatException, ApplicationException {
		File workingDir = File.createTempFile("parse_blast_results", "tmp");
		
		AlignmentAnalyses analyses = new AlignmentAnalyses(analysis, null, workingDir);
		BlastAnalysis ba = (BlastAnalysis) analyses.getAnalysis("blast");
		
		final LineNumberReader reader = new LineNumberReader(new FileReader(blastResults));
		String name = null;
		List<String> results = new ArrayList<String>();
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
		    if (!name(line).equals(name)) {
		    	if (results.size() != 0) {
		    		//TODO aa should be configurable
		    		boolean aa = false;
		    		BlastAnalysis.Result r = processBlastResults(ba, aa, new BlastResultList(results));
		    		String assignment = "Unassigned";
		    		if (r.haveSupport())
		    			assignment = r.getConcludedCluster().getName();
		    		System.out.println(name + "," + assignment + "," + r.getScore());
		    	}
		    	results.clear();
		    	name = name(line);
		    	results.add(line);
		    } else {
		    	results.add(line);
		    }
		}
	}
	
	private static String name(String line) {
		return line.split("\t")[0];
	}
	
	private static BlastAnalysis.Result processBlastResults(BlastAnalysis ba, boolean aa, BlastAnalysis.BlastResults results) throws ApplicationException {
		//TODO provide a proper sequence, this would require to pass the FASTA as well, for now, we pass a bogus sequence
		AbstractSequence sequence = new Sequence("", false, "", "");
		
		return BlastAnalysis.parseBlastResults(results, ba, aa, sequence);
	}
	
	private static void fail() {
		System.err.println("Usage: parse_blast_results analysis.xml blast_results.out");
		System.exit(1);
	}	
}
