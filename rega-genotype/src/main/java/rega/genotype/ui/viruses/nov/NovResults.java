/*
 * Copyright (C) 2008 Rega MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.util.GenotypeLib;

/**
 * Utility class to parse and interpret the analysis' results.xml file.
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

		String conclusionP = "/genotype_result/sequence/conclusion[@id='" + region + "']";

		if (p.elementExists(conclusionP)) {
			result.majorAssignment = GenotypeLib.getEscapedValue(p, conclusionP + "/assigned/name");
			result.majorBootstrap = GenotypeLib.getEscapedValue(p, conclusionP + "/assigned/support");
			result.majorMotivation = GenotypeLib.getEscapedValue(p, conclusionP + "/motivation");

			String variantConclusionP = "/genotype_result/sequence/conclusion[@id='" + region + "-variant']";

			if (p.elementExists(variantConclusionP)) {
				result.variantAssignment = GenotypeLib.getEscapedValue(p, variantConclusionP + "/assigned/name");

				boolean showVariantNotAssigned = p.getValue(conclusionP + "/assigned/id").equals("II.4");
				boolean variantNotAssigned = p.getValue(variantConclusionP + "/assigned/id").equals("Unassigned");

				if (!variantNotAssigned || showVariantNotAssigned)
					result.variantAssignmentForOverview = result.variantAssignment;

				result.variantBootstrap = GenotypeLib.getEscapedValue(p, variantConclusionP + "/assigned/support");
				if (!variantNotAssigned)
					result.variantDescription = GenotypeLib.getEscapedValue(p, variantConclusionP + "/assigned/description");
				else
					result.variantDescription = "Not assigned";
				result.variantMotivation = GenotypeLib.getEscapedValue(p, variantConclusionP + "/motivation");
			}
		} else {
			result.majorAssignment = "";
			result.majorMotivation = "Sequence does not overlap sufficiently (>100 nucleotides) with " + region;
		}

		return result;
	}
	
	public static String getBlastConclusion(GenotypeResultParser p) {
		return GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/cluster/concluded-name");
	}
}
