/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import rega.genotype.ui.data.SaxParser;

/**
 * Utility class to parse and interpret the analysis' results.xml file.
 * 
 * @author simbre1
 *
 */
public class NovResults {
	public static class Conclusion {
		String majorAssignment;
		String majorMotivation;
		String majorBootstrap;
		String variantAssignment;
		String variantDescription;
		String variantBootstrap;
		String variantMotivation;
	}

	public static final String NA = "<i>NA</i>";

	public static Conclusion getConclusion(SaxParser p, String region) {
		Conclusion result = new Conclusion();

		String conclusionP = "genotype_result.sequence.conclusion['" + region + "']";

		if (p.elementExists(conclusionP)) {
			result.majorAssignment = p.getEscapedValue(conclusionP + ".assigned.name");
			result.majorBootstrap = p.getEscapedValue(conclusionP + ".assigned.support");
			result.majorMotivation = p.getEscapedValue(conclusionP + ".motivation");

			conclusionP = "genotype_result.sequence.conclusion['" + region + "-variant']";

			if (p.elementExists(conclusionP)) {
				result.variantAssignment = p.getEscapedValue(conclusionP + ".assigned.name");
				result.variantBootstrap = p.getEscapedValue(conclusionP + ".assigned.support");
				result.variantDescription = p.getEscapedValue(conclusionP + ".assigned.description");
				result.variantMotivation = p.getEscapedValue(conclusionP + ".motivation");
			}
		} else {
			result.majorAssignment = getBlastConclusion(p);
			result.majorMotivation = "Sequence does not overlap sufficiently (>100 nucleotides) with " + region;
		}

		return result;
	}
	
	public static String getBlastConclusion(SaxParser p) {
		return p.elementExists("genotype_result.sequence.conclusion")
		? p.getEscapedValue("genotype_result.sequence.conclusion.assigned.name")
		: NA;
	}

	public static String getBlastMotivation(SaxParser p) {
		return p.elementExists("genotype_result.sequence.conclusion")
		? p.getEscapedValue("genotype_result.sequence.conclusion.motivation")
		: "";
	}
}
