/*
 * Copyright (C) 2013 Emweb
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.generic;

import java.io.IOException;
import java.util.List;

import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.viruses.generic.GenericDefinition.ResultColumn;

/**
 * Create a csv file of Enterovirus job results 
 */
public class GenericTableGenerator extends AbstractDataTableGenerator {
	private List<ResultColumn> columns;
	
	public GenericTableGenerator(AbstractJobOverview jobOverview, DataTable table, List<ResultColumn> columns) throws IOException {
		super(jobOverview.getFilter(), table);
		this.columns = columns;

		if (columns == null) {
			table.addLabel("name");
			table.addLabel("length");
		
			table.addLabel("Genus/Species");
			table.addLabel("Serotype");
			table.addLabel("Genogroup");
		
			table.addLabel("species_score");
			table.addLabel("reverse-compliment");
			table.addLabel("refseq");
			table.addLabel("begin");
			table.addLabel("end");
	
			table.addLabel("serotype_support");
			table.addLabel("serotype_inner_support");
			table.addLabel("serotype_outer_support");
			
			table.addLabel("genogroup_support");
			table.addLabel("genogroup_inner_support");
			table.addLabel("genogroup_outer_support");
		} else {
			for (ResultColumn column : columns) {
				table.addLabel(column.label);
			}
		}
		
		table.newRow();
	}
    
	private final static String serotype = "serotype";
	private final static String subgenogroup = "subgenogroup";

	public void endSequence() {
		if (columns == null) {
	    	addNamedValue("/genotype_result/sequence/@name", ValueFormat.Label);
	    	addNamedValue("/genotype_result/sequence/@length", ValueFormat.Number);

	    	addNamedValue("/genotype_result/sequence/result[@id='blast']/cluster/name", ValueFormat.Label);
	    	
	    	String serotype_id = getValue("/genotype_result/sequence/conclusion[@id='" + serotype + "']/assigned/id");
	    	addValue(serotype_id == null ? "" : serotype_id);
	    	
	    	String subgeno_id = getValue("/genotype_result/sequence/conclusion[@id='" + subgenogroup + "']/assigned/id");
	    	addValue(subgeno_id == null ? "" : subgeno_id);

	    	addNamedValue("/genotype_result/sequence/result[@id='blast']/cluster/score", ValueFormat.Number);
	    	addNamedValue("/genotype_result/sequence/result[@id='blast']/cluster/reverse-compliment", ValueFormat.Label);
	    	addNamedValue("/genotype_result/sequence/result[@id='blast']/refseq", ValueFormat.Label);
	    	addNamedValue("/genotype_result/sequence/result[@id='blast']/start", ValueFormat.Number);
	    	addNamedValue("/genotype_result/sequence/result[@id='blast']/end", ValueFormat.Number);

	   		addConclusion(serotype);
	   		addConclusion(subgenogroup);
		} else {
			for (ResultColumn column : columns) {
				addNamedValue(column.field, ValueFormat.Label);
			}
		}
	    	
    	super.endSequence();
	}
	
	private void addConclusion(String id) {
		if (getValue("/genotype_result/sequence/conclusion[@id='" + id + "']/assigned/id") != null) {
			addNamedValue("/genotype_result/sequence/conclusion[@id='" + id + "']/assigned/support", ValueFormat.Number);
			addNamedValue("/genotype_result/sequence/conclusion[@id='" + id + "']/assigned/inner", ValueFormat.Number);
			addNamedValue("/genotype_result/sequence/conclusion[@id='" + id + "']/assigned/outer", ValueFormat.Number);
		} else {
	  		for (int i = 0; i < 3; ++i)
    			addValue("");
		}
	}
}
