package rega.genotype.ui.viruses.nrv;

import java.io.PrintStream;

import rega.genotype.ui.data.AbstractCsvGenerator;

public class NrvCsvGenerator extends AbstractCsvGenerator {
	public NrvCsvGenerator(PrintStream ps) {
		super(ps);
		ps.append("name,length,orf1,orf2,begin,end,genogroup," +
				"ORF1_genotype,ORF1_genotype_support,ORF1_inner_support,ORF1_outer_support," +
				"ORF1_variant,ORF1_variant_support,ORF1_variant_inner_support,ORF1_variant_outer_support" +
				"ORF2_genotype,ORF2_genotype_support,ORF2_inner_support,ORF2_outer_support," +
				"ORF2_variant,ORF2_variant_support,ORF2_variant_inner_support,ORF2_variant_outer_support");
		
		ps.append("\n");
	}
    
	public void writeLine(PrintStream ps) {
    	StringBuilder csvLine = new StringBuilder();
    	
    	csvLine.append(addCsvValue("genotype_result.sequence[name]", true));
    	csvLine.append(addCsvValue("genotype_result.sequence[length]"));

    	if (!elementExists("genotype_result.sequence.conclusion['ORF1']")
    		&& !elementExists("genotype_result.sequence.conclusion['ORF2']"))
    		csvLine.append(",\"Sequence error\",\"Sequence error\"");
    	else {
    		csvLine.append(addCsvValue("genotype_result.sequence.conclusion['ORF1'].assigned.name"));
    		csvLine.append(addCsvValue("genotype_result.sequence.conclusion['ORF2'].assigned.name"));
    	}

    	csvLine.append(addCsvValue("genotype_result.sequence.result['blast'].start"));
    	csvLine.append(addCsvValue("genotype_result.sequence.result['blast'].end"));
    	csvLine.append(addCsvValue("genotype_result.sequence.result['blast'].cluster.name"));
    	
    	addPhyloResults(csvLine, "phylo-ORF1");

    	String id = getValue("genotype_result.sequence.result['phylo-ORF1'].best.id");
    	if (id == null)
    		id = "";

    	addPhyloResults(csvLine, "phylo-ORF1-" + id);

    	addPhyloResults(csvLine, "phylo-ORF2");

    	id = getValue("genotype_result.sequence.result['phylo-ORF2'].best.id");
    	if (id == null)
    		id = "";

    	addPhyloResults(csvLine, "phylo-ORF2-" + id);
    	
    	ps.append(csvLine.toString()+"\n");
	}
}
