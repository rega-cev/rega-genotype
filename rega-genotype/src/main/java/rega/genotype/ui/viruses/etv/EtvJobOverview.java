/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import java.util.ArrayList;
import java.util.List;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.JobOverviewSummary;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;

/**
 * Enterovirus job overview implementation.
 */
public class EtvJobOverview extends AbstractJobOverview {
	private List<Header> headers = new ArrayList<Header>();

	public EtvJobOverview(GenotypeWindow main) {
		super(main);
		
		headers.add(new Header(new WString("Name")));
		headers.add(new Header(new WString("Length")));
		headers.add(new Header(new WString("Genus/Species")));
		headers.add(new Header(new WString("Serotype, Sub-Genogroup"), 2));
		headers.add(new Header(new WString("Report")));
		headers.add(new Header(new WString("Genome")));
	}

	@Override
	public List<WWidget> getData(final GenotypeResultParser p) {
		List<WWidget> data = new ArrayList<WWidget>();

		data.add(new WText(new WString(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name"))));
		data.add(new WText(new WString(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@length"))));

		String blastResult = EtvResults.getBlastConclusion(p);
		data.add(new WText(new WString(notNull(blastResult))));

		EtvResults.Conclusion c = EtvResults.getConclusion(p);

		data.add(new WText(new WString(notNull(c.majorAssignmentForOverview))));
		data.add(new WText(new WString(notNull(c.variantAssignmentForOverview))));
		
		WAnchor report = createReportLink(p);
		data.add(report);

		// XX could be through EtvResults ?
		boolean havePhyloAnalysis = p.getValue("/genotype_result/sequence/result[@id='phylo-serotype']/best/id") != null;
		boolean haveBlastAssignment = havePhyloAnalysis || !"Unassigned".equals(p.getValue("/genotype_result/sequence/conclusion/assigned/id"));
		data.add(createGenomeImage(p, "-", !haveBlastAssignment));
	
		return data;
	}

	private static String notNull(String s) {
		return s == null ? "" : s;
	}

	@Override
	public List<Header> getHeaders() {
		return headers;
	}

	@Override
	protected boolean downloadResultsLink() {
		return false;
	}

	@Override
	public JobOverviewSummary getSummary(String filter) {
		return null;
	}
}
