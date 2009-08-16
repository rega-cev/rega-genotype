package rega.genotype.ui.viruses.nrv;

import rega.genotype.ui.data.SaxParser;

public class NrvResults {

	public static String getConclusion(SaxParser p, String region) {
		return p.elementExists("genotype_result.sequence.conclusion['" + region + "']")
			? p.getValue("genotype_result.sequence.conclusion['" + region
						 + "'].assigned.name")
			: "NA";
	}

	public static String getMotivation(SaxParser p, String region) {
		return p.elementExists("genotype_result.sequence.conclusion['" + region + "']")
		? p.getValue("genotype_result.sequence.conclusion['" + region
				 + "'].motivation")
		: "No sufficient overlap with region (<100 nucleotides)";
	}

}
