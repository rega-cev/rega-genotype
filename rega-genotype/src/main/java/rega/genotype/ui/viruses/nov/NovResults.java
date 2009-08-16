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

	public static final String NA = "<i>NA</i>";

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

	public static String getConclusion(SaxParser p, String region) {
		return p.elementExists("genotype_result.sequence.conclusion['" + region + "']")
			? p.getEscapedValue("genotype_result.sequence.conclusion['" + region
						 + "'].assigned.name")
			: getBlastConclusion(p);
	}

	public static String getMotivation(SaxParser p, String region) {
		return p.elementExists("genotype_result.sequence.conclusion['" + region + "']")
		? p.getEscapedValue("genotype_result.sequence.conclusion['" + region
				 + "'].motivation")
		: "Sequence does not overlap sufficiently (>100 nucleotides) with " + region;
	}

}
