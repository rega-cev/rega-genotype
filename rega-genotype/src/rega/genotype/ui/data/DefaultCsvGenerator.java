package rega.genotype.ui.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DefaultCsvGenerator extends AbstractCsvGenerator {
	public DefaultCsvGenerator(PrintStream os) {
		super(os);
	}
    
	public void writeLine(PrintStream ps) {
    	StringBuilder csvLine = new StringBuilder();
    	
    	if(!writtenFullSequence) {
    		ps.append("name,length,assignment,support,begin,end,type,pure,pure_support,pure_inner,pure_outer,scan_best_support,scan_assigned_support,scan_assigned_nosupport,scan_best_profile,scan_assigned_profile,crf,crf_support,crf_inner,crf_outer,crfscan_best_support,crfscan_assigned_support,crfscan_assigned_nosupport,crfscan_best_profile,crfscan_assigned_profile,major_id,minor_id\n");
    	}
    	
    	csvLine.append(getCsvValue("genotype_result.sequence['name']")+",");
    	csvLine.append(getCsvValue("genotype_result.sequence['length']")+",");

    	if(!elements.contains("genotype_result.sequence.conclusion")) {
    		csvLine.append("\"Sequence error\",");
    	} else {
    		csvLine.append(getCsvValue("genotype_result.sequence.conclusion.assigned.name")+",");
    		if(elements.contains("genotype_result.sequence.conclusion.assigned.support")) {
    			csvLine.append(getCsvValue("genotype_result.sequence.conclusion.assigned.support"));
    		}
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[blast].start"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[blast].end"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[blast].cluster.name"));
    		
    		if(elements.contains("genotype_result.sequence.result[pure]")) {
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[pure].best.id"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[pure].best.support"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[pure].best.inner"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[pure].best.outer"));
    		} else {
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[purepuzzle].best.id"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[purepuzzle].best.support"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[purepuzzle].best.inner"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[purepuzzle].best.outer"));
    		}
    		
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].support[assigned]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].support[best]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].nosupport[best]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].profile[assigned]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].profile[best]"));
    		
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crf].best.id"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crf].best.support"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crf].best.inner"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crf].best.outer"));
    		
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].support[assigned]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].support[best]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].nosupport[best]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].profile[assigned]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].profile[best]"));
    		
    		csvLine.append("," + getCsvValue("genotype_result.sequence.conclusion.assigned.major.assigned.id"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.conclusion.assigned.minor.assigned.id"));
    	}
    	
    	ps.append(csvLine.toString()+"\n");
    }


    public static void main(String [] args) {
    	DefaultCsvGenerator csvgen;
		try {
			FileOutputStream fos = new FileOutputStream("/home/plibin0/projects/utrecht/genotype/result3.csv");
			csvgen = new DefaultCsvGenerator(new PrintStream(fos));
			csvgen.parse(new InputSource(new FileInputStream("/home/plibin0/projects/utrecht/genotype/result.xml")));
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
