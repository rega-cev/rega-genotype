/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import java.io.IOException;

import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.util.DataTable;

/**
 * Create a csv file of NoV job results 
 * 
 * @author simbre1
 *
 */
public class NovTableGenerator extends AbstractDataTableGenerator {
	public NovTableGenerator(DataTable table) throws IOException {
		super(table);

		table.addLabel("name");
		table.addLabel("length");
		table.addLabel("ORF1");
		table.addLabel("ORF2");
		table.addLabel("begin");
		table.addLabel("end");
		table.addLabel("genogroup");
		table.addLabel("ORF1_genotype");
		table.addLabel("ORF1_genotype_support");
		table.addLabel("ORF1_inner_support");
		table.addLabel("ORF1_outer_support");
		table.addLabel("ORF1_variant");
		table.addLabel("ORF1_variant_support");
		table.addLabel("ORF1_variant_inner_support");
		table.addLabel("ORF1_variant_outer_support");
		table.addLabel("ORF2_genotype");
		table.addLabel("ORF2_genotype_support");
		table.addLabel("ORF2_inner_support");
		table.addLabel("ORF2_outer_support");
		table.addLabel("ORF2_variant");
		table.addLabel("ORF2_variant_support");
		table.addLabel("ORF2_variant_inner_support");
		table.addLabel("ORF2_variant_outer_support");
		
		table.newRow();
	}
    
	public void endSequence() {
    	addNamedValue("genotype_result.sequence[name]", ValueFormat.Label);
    	addNamedValue("genotype_result.sequence[length]", ValueFormat.Number);

    	String orf1Conclusion = NovResults.getConclusion(this, "ORF1");
    	if (orf1Conclusion.equals(NovResults.NA))
    		addValue("");
    	else
    		addValue(orf1Conclusion);

    	String orf2Conclusion = NovResults.getConclusion(this, "ORF2");
    	if (orf2Conclusion.equals(NovResults.NA))
    		addValue("");
    	else
    		addValue(orf2Conclusion);

    	addNamedValue("genotype_result.sequence.result['blast'].start", ValueFormat.Number);
    	addNamedValue("genotype_result.sequence.result['blast'].end", ValueFormat.Number);
    	addNamedValue("genotype_result.sequence.result['blast'].cluster.name", ValueFormat.Label);
    	
    	addPhyloResults("phylo-ORF1");

    	String id = getValue("genotype_result.sequence.result['phylo-ORF1'].best.id");
    	if (id != null)
    		addPhyloResults("phylo-ORF1-" + id);
    	else
    		for (int i = 0; i < 4; ++i)
    			addValue("");

    	addPhyloResults("phylo-ORF2");

    	id = getValue("genotype_result.sequence.result['phylo-ORF2'].best.id");
    	if (id != null)
    		addPhyloResults("phylo-ORF2-" + id);
    	else
    		for (int i = 0; i < 4; ++i)
    			addValue("");
    	
    	super.endSequence();
	}
}
