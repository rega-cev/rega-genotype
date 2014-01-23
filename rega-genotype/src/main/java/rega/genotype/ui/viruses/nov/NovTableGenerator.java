/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import java.io.IOException;

import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.data.AbstractDataTableGenerator.ValueFormat;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.util.DataTable;

/**
 * Create a csv file of NoV job results 
 */
public class NovTableGenerator extends AbstractDataTableGenerator {
	public NovTableGenerator(AbstractJobOverview jobOverview, DataTable table) throws IOException {
		super(jobOverview.getFilter(), table);

		table.addLabel("name");
		table.addLabel("length");
		table.addLabel("ORF1");
		table.addLabel("ORF1_variant");
		table.addLabel("ORF2");
		table.addLabel("ORF2_variant");
		table.addLabel("refseq");
		table.addLabel("begin");
		table.addLabel("end");
		table.addLabel("genogroup");
		table.addLabel("ORF1_genotype");
		table.addLabel("ORF1_genotype_support");
		table.addLabel("ORF1_variant");
		table.addLabel("ORF1_variant_support");
		table.addLabel("ORF2_genotype");
		table.addLabel("ORF2_genotype_support");
		table.addLabel("ORF2_variant");
		table.addLabel("ORF2_variant_support");
		
		table.newRow();
	}
    
	public void endSequence() {
    	addNamedValue("/genotype_result/sequence/@name", ValueFormat.Label);
    	addNamedValue("/genotype_result/sequence/@length", ValueFormat.Number);

		NovResults.Conclusion c = NovResults.getConclusion(this, "ORF1");

		addValue(c.majorAssignment == NovResults.NA ? "" : c.majorAssignment);
		addValue(c.variantAssignmentForOverview == null ? "" : c.variantAssignmentForOverview);

		c = NovResults.getConclusion(this, "ORF2");

		addValue(c.majorAssignment == NovResults.NA ? "" : c.majorAssignment);
		addValue(c.variantAssignmentForOverview == null ? "" : c.variantAssignmentForOverview);

    	addNamedValue("/genotype_result/sequence/result[@id='blast']/refseq", ValueFormat.Label);
    	addNamedValue("/genotype_result/sequence/result[@id='blast']/start", ValueFormat.Number);
    	addNamedValue("/genotype_result/sequence/result[@id='blast']/end", ValueFormat.Number);
    	addNamedValue("/genotype_result/sequence/result[@id='blast']/cluster/name", ValueFormat.Label);
    	
    	addPhyloResults("phylo-ORF1", false, false);

    	String id = getValue("/genotype_result/sequence/result[@id='phylo-ORF1']/best/id");
    	if (id != null)
    		addPhyloResults("phylo-ORF1-" + id, true, false);
    	else
    		for (int i = 0; i < 2; ++i)
    			addValue("");

    	addPhyloResults("phylo-ORF2", false, false);

    	id = getValue("/genotype_result/sequence/result[@id='phylo-ORF2']/best/id");
    	if (id != null)
    		addPhyloResults("phylo-ORF2-" + id, true, false);
    	else
    		for (int i = 0; i < 2; ++i)
    			addValue("");
    	
    	super.endSequence();
	}
}
