/*
 * Copyright (C) 2008 Rega MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import rega.genotype.ui.data.GenotypeResultParser;

/**
 * Utility class to parse and interpret the analysis' results.xml file.
 */
public class EtvResults {
	public static class Conclusion {
		String majorAssignment;
		String majorMotivation;
		String majorBootstrap;
		String variantAssignment;
		String variantDescription;
		String variantBootstrap;
		String variantMotivation;
		String variantAssignmentForOverview;
	}

	public static final String NA = "<i>NA</i>";

	public static Conclusion getSerotype(GenotypeResultParser p) {
		Conclusion result = new Conclusion();

		String conclusionP = "genotype_result.sequence.conclusion";

		if (p.elementExists(conclusionP)) {
			result.majorAssignment = p.getEscapedValue(conclusionP + ".assigned.name");
			result.majorBootstrap = p.getEscapedValue(conclusionP + ".assigned.support");
			result.majorMotivation = p.getEscapedValue(conclusionP + ".motivation");
		} else {
			result.majorAssignment = getBlastConclusion(p);
			result.majorMotivation = "Sequence does not overlap sufficiently (>100 nucleotides) with VP1";
		}

		return result;
	}
	
	public static String getBlastConclusion(GenotypeResultParser p) {
		return p.elementExists("genotype_result.sequence.conclusion")
		? p.getEscapedValue("genotype_result.sequence.conclusion.assigned.name")
		: NA;
	}

	public static String getBlastMotivation(GenotypeResultParser p) {
		return p.elementExists("genotype_result.sequence.conclusion")
		? p.getEscapedValue("genotype_result.sequence.conclusion.motivation")
		: "";
	}
}
