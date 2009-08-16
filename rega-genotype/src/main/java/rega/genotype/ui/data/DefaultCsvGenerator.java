package rega.genotype.ui.data;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

public class DefaultCsvGenerator extends AbstractCsvGenerator {
	public DefaultCsvGenerator(Writer ps) throws IOException {
		super(ps);
		ps.append("name,length,assignment,support,begin,end,type,pure,pure_support,pure_inner,pure_outer,scan_best_support,scan_assigned_support,scan_assigned_nosupport,scan_best_profile,scan_assigned_profile,crf,crf_support,crf_inner,crf_outer,crfscan_best_support,crfscan_assigned_support,crfscan_assigned_nosupport,crfscan_best_profile,crfscan_assigned_profile,major_id,minor_id\n");
	}
    
	public void writeLine(Writer ps) throws IOException {
    	StringBuilder csvLine = new StringBuilder();
    	
    	csvLine.append(addCsvValue("genotype_result.sequence[name]", true));
    	csvLine.append(addCsvValue("genotype_result.sequence[length]"));

    	if (!elementExists("genotype_result.sequence.conclusion"))
    		csvLine.append(",\"Sequence error\"");
    	else
    		csvLine.append(addCsvValue("genotype_result.sequence.conclusion.assigned.name"));
    	
    	csvLine.append(addCsvValue("genotype_result.sequence.conclusion.assigned.support"));

    	csvLine.append(addCsvValue("genotype_result.sequence.result['blast'].start"));
    	csvLine.append(addCsvValue("genotype_result.sequence.result['blast'].end"));
    	csvLine.append(addCsvValue("genotype_result.sequence.result['blast'].cluster.name"));
    		
    	if (elementExists("genotype_result.sequence.result['pure']"))
    		addPhyloResults(csvLine, "pure");
    	else
    		addPhyloResults(csvLine, "pure-puzzle");

		addPhyloScanResults(csvLine, "scan");

		addPhyloResults(csvLine, "crf");

		addPhyloScanResults(csvLine, "crfscan");
		
		csvLine.append(addCsvValue("genotype_result.sequence.conclusion.assigned.major.assigned.id"));
		csvLine.append(addCsvValue("genotype_result.sequence.conclusion.assigned.minor.assigned.id"));
    	
    	ps.append(csvLine.toString()+"\n");
    }
}
