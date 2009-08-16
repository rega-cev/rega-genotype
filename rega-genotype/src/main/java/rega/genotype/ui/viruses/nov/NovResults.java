/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import rega.genotype.ui.data.GenotypeResultParser;

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
		String variantAssignmentForOverview;
	}

	public static final String NA = "<i>NA</i>";

	public static Conclusion getConclusion(GenotypeResultParser p, String region) {
		Conclusion result = new Conclusion();

		String conclusionP = "genotype_result.sequence.conclusion['" + region + "']";

		if (p.elementExists(conclusionP)) {
			result.majorAssignment = p.getEscapedValue(conclusionP + ".assigned.name");
			result.majorBootstrap = p.getEscapedValue(conclusionP + ".assigned.support");
			result.majorMotivation = p.getEscapedValue(conclusionP + ".motivation");

			String variantConclusionP = "genotype_result.sequence.conclusion['" + region + "-variant']";

			if (p.elementExists(variantConclusionP)) {
				result.variantAssignment = p.getEscapedValue(variantConclusionP + ".assigned.name");

				boolean showVariantNotAssigned = p.getValue(conclusionP + ".assigned.id").equals("II.4");
				boolean variantNotAssigned = p.getValue(variantConclusionP + ".assigned.id").equals("Unassigned");

				if (!variantNotAssigned || showVariantNotAssigned)
					result.variantAssignmentForOverview = result.variantAssignment;

				result.variantBootstrap = p.getEscapedValue(variantConclusionP + ".assigned.support");
				if (!variantNotAssigned)
					result.variantDescription = p.getEscapedValue(variantConclusionP + ".assigned.description");
				else
					result.variantDescription = "Not assigned";
				result.variantMotivation = p.getEscapedValue(variantConclusionP + ".motivation");
			}
		} else {
			result.majorAssignment = getBlastConclusion(p);
			result.majorMotivation = "Sequence does not overlap sufficiently (>100 nucleotides) with " + region;
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
