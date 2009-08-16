package rega.genotype.ui.viruses.nrv;

import rega.genotype.ui.data.SaxParser;

public class NrvResults {

	public static final String NA = "<i>NA</i>";

	public static String getBlastConclusion(SaxParser p) {
		return p.elementExists("genotype_result.sequence.conclusion")
		? p.getValue("genotype_result.sequence.conclusion.assigned.name")
		: NA;
	}

	public static String getBlastMotivation(SaxParser p) {
		return p.elementExists("genotype_result.sequence.conclusion")
		? p.getValue("genotype_result.sequence.conclusion.motivation")
		: "";
	}

	public static String getConclusion(SaxParser p, String region) {
		return p.elementExists("genotype_result.sequence.conclusion['" + region + "']")
			? p.getValue("genotype_result.sequence.conclusion['" + region
						 + "'].assigned.name")
			: getBlastConclusion(p);
	}

	public static String getMotivation(SaxParser p, String region) {
		return p.elementExists("genotype_result.sequence.conclusion['" + region + "']")
		? p.getValue("genotype_result.sequence.conclusion['" + region
				 + "'].motivation")
		: "Sequence does not overlap sufficiently (>100 nucleotides) with " + region;
	}

}
