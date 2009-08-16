/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

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
 * Enterovirus job overview implementation.
 */
public class EtvJobOverview extends AbstractJobOverview {
	private List<Header> headers = new ArrayList<Header>();

	public EtvJobOverview(GenotypeWindow main) {
		super(main);
		
		headers.add(new Header(new WString("Name")));
		headers.add(new Header(new WString("Length")));
		headers.add(new Header(new WString("Species")));
		headers.add(new Header(new WString("Serotype")));
		headers.add(new Header(new WString("Report")));
		headers.add(new Header(new WString("Genome")));
	}

	@Override
	public List<WWidget> getData(final GenotypeResultParser p) {
		List<WWidget> data = new ArrayList<WWidget>();

		data.add(new WText(new WString(p.getEscapedValue("genotype_result.sequence[name]"))));
		data.add(new WText(new WString(p.getEscapedValue("genotype_result.sequence[length]"))));

		boolean havePhyloAnalysis = p.getValue("genotype_result.sequence.result['phylo-serotype'].best.id") != null;
		boolean haveBlastAssignment = havePhyloAnalysis || p.getValue("genotype_result.sequence.conclusion['unassigned'].assigned.id") != null;

		if (haveBlastAssignment) {
			String blastAssignment = p.getEscapedValue("genotype_result.sequence.result['blast'].cluster.name");
			data.add(new WText(new WString(notNull(blastAssignment))));
		} else
			data.add(new WText("Could not assign"));
		
		if (havePhyloAnalysis) {
			String serotypeAssignment = p.getEscapedValue("genotype_result.sequence.conclusion.assigned.id");
			data.add(new WText(new WString(notNull(serotypeAssignment))));
		} else
			data.add(new WText());

		if (havePhyloAnalysis) {
			WAnchor report = createReportLink(p);
			data.add(report);
		} else
			data.add(new WText());

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
