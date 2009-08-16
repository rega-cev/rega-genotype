/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.JobOverviewSummary;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;

/**
 * NoV job overview implementation.
 */
public class NovJobOverview extends AbstractJobOverview {
	private List<Header> headers = new ArrayList<Header>();
	private List<WWidget> data = new ArrayList<WWidget>();
	
	public NovJobOverview(GenotypeWindow main) {
		super(main);
		
		headers.add(new Header(new WString("Name")));
		headers.add(new Header(new WString("Length")));
		headers.add(new Header(new WString("Report")));
		headers.add(new Header(new WString("ORF 1"), 2));
		headers.add(new Header(new WString("ORF 2"), 2));
		headers.add(new Header(new WString("Genome")));
	}
	
	@Override
	public List<WWidget> getData(final GenotypeResultParser p) {
		data.clear();

		data.add(new WText(new WString(p.getEscapedValue("genotype_result.sequence[name]"))));
		data.add(new WText(new WString(p.getEscapedValue("genotype_result.sequence[length]"))));

		WAnchor report = createReportLink(p);
		data.add(report);

		NovResults.Conclusion c = NovResults.getConclusion(p, "ORF1");

		data.add(new WText(new WString(notNull(c.majorAssignment))));
		data.add(new WText(new WString(notNull(c.variantAssignmentForOverview))));

		c = NovResults.getConclusion(p, "ORF2");

		data.add(new WText(new WString(notNull(c.majorAssignment))));
		data.add(new WText(new WString(notNull(c.variantAssignmentForOverview))));

		try {
			data.add(GenotypeLib.getWImageFromFile(getMain().getOrganismDefinition().getGenome().getSmallGenomePNG(jobDir, p.getSequenceIndex(), 
					"-",
					Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].start")), 
					Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].end")),
					0, "", null)));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
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
