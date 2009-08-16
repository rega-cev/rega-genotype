package rega.genotype.ui.data;

import java.io.PrintStream;

public class DefaultCsvGenerator extends AbstractCsvGenerator {
	public DefaultCsvGenerator(PrintStream ps) {
		super(ps);
		ps.append("name,length,assignment,support,begin,end,type,pure,pure_support,pure_inner,pure_outer,scan_best_support,scan_assigned_support,scan_assigned_nosupport,scan_best_profile,scan_assigned_profile,crf,crf_support,crf_inner,crf_outer,crfscan_best_support,crfscan_assigned_support,crfscan_assigned_nosupport,crfscan_best_profile,crfscan_assigned_profile,major_id,minor_id\n");
	}
    
	public void writeLine(PrintStream ps) {
    	StringBuilder csvLine = new StringBuilder();
    	
    	csvLine.append(getCsvValue("genotype_result.sequence['name']")+",");
    	csvLine.append(getCsvValue("genotype_result.sequence['length']")+",");

    	if(!elementExists("genotype_result.sequence.conclusion")) {
    		csvLine.append("\"Sequence error\",");
    	} else {
    		csvLine.append(getCsvValue("genotype_result.sequence.conclusion.assigned.name")+",");
    		if(elementExists("genotype_result.sequence.conclusion.assigned.support")) {
    			csvLine.append(getCsvValue("genotype_result.sequence.conclusion.assigned.support"));
    		}
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[blast].start"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[blast].end"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[blast].cluster.name"));
    		
    		if(elementExists("genotype_result.sequence.result[pure]")) {
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
}
