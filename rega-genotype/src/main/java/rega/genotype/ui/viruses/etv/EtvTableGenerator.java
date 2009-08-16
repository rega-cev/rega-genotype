/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import java.io.IOException;

import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.util.DataTable;

/**
 * Create a csv file of Enterovirus job results 
 */
public class EtvTableGenerator extends AbstractDataTableGenerator {
	public EtvTableGenerator(DataTable table) throws IOException {
		super(table);

		table.addLabel("name");
		table.addLabel("length");

		table.addLabel("conclusion");
		table.addLabel("species");
		table.addLabel("species_score");
		table.addLabel("begin");
		table.addLabel("end");

		table.addLabel("serotype");
		table.addLabel("serotype_support");
		table.addLabel("serotype_inner_support");
		table.addLabel("serotype_outer_support");
		
		table.newRow();
	}
    
	public void endSequence() {
    	addNamedValue("genotype_result.sequence[name]", ValueFormat.Label);
    	addNamedValue("genotype_result.sequence[length]", ValueFormat.Number);

    	addNamedValue("genotype_result.sequence.conclusion.assigned.name", ValueFormat.Label);

    	addNamedValue("genotype_result.sequence.result['blast'].cluster.name", ValueFormat.Label);
    	addNamedValue("genotype_result.sequence.result['blast'].cluster.score", ValueFormat.Number);
    	addNamedValue("genotype_result.sequence.result['blast'].start", ValueFormat.Number);
    	addNamedValue("genotype_result.sequence.result['blast'].end", ValueFormat.Number);

    	String id = getValue("genotype_result.sequence.result['phylo-serotype'].best.id");
    	if (id != null)
    		addPhyloResults("phylo-serotype", true);
    	else
    		for (int i = 0; i < 4; ++i)
    			addValue("");
    	
    	super.endSequence();
	}
}
