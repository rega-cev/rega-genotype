/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.data;

import java.io.IOException;

import rega.genotype.ui.util.DataTable;

/**
 * A default extension of AbstractCsvGenerator, used by different virus implementations.
 */
public class DefaultTableGenerator extends AbstractDataTableGenerator {
	public DefaultTableGenerator(SequenceFilter filter, DataTable table) throws IOException {
		super(filter, table);

		table.addLabel("name");
		table.addLabel("length");
		table.addLabel("assignment");
		table.addLabel("rule");
		table.addLabel("support");
		table.addLabel("begin");
		table.addLabel("end");
		table.addLabel("type");
		table.addLabel("pure");
		table.addLabel("pure_support");
		table.addLabel("pure_inner");
		table.addLabel("pure_outer");
		table.addLabel("scan_best_support");
		table.addLabel("scan_assigned_support");
		table.addLabel("scan_assigned_nosupport");
		table.addLabel("scan_best_profile");
		table.addLabel("scan_assigned_profile");
		table.addLabel("crf");
		table.addLabel("crf_support");
		table.addLabel("crf_inner");
		table.addLabel("crf_outer");
		table.addLabel("crfscan_best_support");
		table.addLabel("crfscan_assigned_support");
		table.addLabel("crfscan_assigned_nosupport");
		table.addLabel("crfscan_best_profile");
		table.addLabel("crfscan_assigned_profile");
		table.addLabel("major_id");
		table.addLabel("minor_id");
		table.newRow();
	}
    
	public void endSequence() {
    	addNamedValue("/genotype_result/sequence/@name", ValueFormat.Label);
    	addNamedValue("/genotype_result/sequence/@length", ValueFormat.Number);

    	if (!elementExists("/genotype_result/sequence/conclusion")){
    		addValue(",\"Sequence error\"");
    		addValue(",");
    	}
    	else{
    		addNamedValue("/genotype_result/sequence/conclusion/assigned/name", ValueFormat.Label);
    		addNamedValue("/genotype_result/sequence/conclusion/rule", ValueFormat.Label);
    	}
    	
    	addNamedValue("/genotype_result/sequence/conclusion/assigned/support", ValueFormat.Number);

    	addNamedValue("/genotype_result/sequence/result[@id='blast']/start", ValueFormat.Number);
    	addNamedValue("/genotype_result/sequence/result[@id='blast']/end", ValueFormat.Number);
    	addNamedValue("/genotype_result/sequence/result[@id='blast']/cluster/name", ValueFormat.Label);
    		
    	if (elementExists("/genotype_result/sequence/result[@id='pure']"))
    		addPhyloResults("pure", false);
    	else
    		addPhyloResults("pure-puzzle", false);

		addPhyloScanResults("scan-pure");

		addPhyloResults("crf", false);

		addPhyloScanResults("scan-crf");
		
		addNamedValue("/genotype_result/sequence/conclusion/assigned/major/assigned/id", ValueFormat.Label);
		addNamedValue("/genotype_result/sequence/conclusion/assigned/minor/assigned/id", ValueFormat.Label);
    	
    	super.endSequence();
    }
}
