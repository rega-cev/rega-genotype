/*
 * Copyright (C) 2013 Emweb
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.generic;

import java.util.ArrayList;
import java.util.List;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.DefaultJobOverviewSummary;
import rega.genotype.ui.forms.JobOverviewSummary;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.viruses.generic.GenericDefinition.ResultColumn;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;

/**
 * Enterovirus job overview implementation.
 */
public class GenericJobOverview extends AbstractJobOverview {
	private List<Header> headers = new ArrayList<Header>();
	private List<ResultColumn> columns;

	public GenericJobOverview(GenotypeWindow main, List<ResultColumn> resultColumns) {
		super(main);
		
		this.columns = resultColumns;
		
		if (this.columns == null) {
			/* Standard columns */
			headers.add(new Header(WString.tr("table.header.name")));
			headers.add(new Header(WString.tr("table.header.length")));
			headers.add(new Header(WString.tr("table.header.blast")));
			headers.add(new Header(WString.tr("table.header.phylo"), 2));
			headers.add(new Header(WString.tr("table.header.report")));
			headers.add(new Header(WString.tr("table.header.genome")));
		} else {
			for (ResultColumn c : columns)
				headers.add(new Header(new WString(c.label), c.colSpan));
		}
	}

	@Override
	public List<WWidget> getData(final GenotypeResultParser p) {
		List<WWidget> data = new ArrayList<WWidget>();

		if (this.columns == null) {
			data.add(new WText(new WString(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name"))));
			data.add(new WText(new WString(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@length"))));

			String blastResult = GenericResults.getBlastConclusion(p);
			data.add(new WText(new WString(notNull(blastResult))));

			GenericResults.Conclusion c = GenericResults.getConclusion(p);

			data.add(new WText(new WString(notNull(c.majorAssignmentForOverview))));
			data.add(new WText(new WString(notNull(c.variantAssignmentForOverview))));
			
			WAnchor report = createReportLink(p);
			data.add(report);

			boolean havePhyloAnalysis = p.getValue("/genotype_result/sequence/result[@id='type']/best/id") != null;
			boolean haveBlastAssignment = havePhyloAnalysis || !"Unassigned".equals(p.getValue("/genotype_result/sequence/conclusion/assigned/id"));
			data.add(createGenomeImage(p, "-", !haveBlastAssignment));
		} else {
			for (ResultColumn c : columns) {
				if (c.field.equals("report-link"))
					data.add(createReportLink(p));
				else if (c.field.equals("genome")) {
					data.add(createGenomeImage(p, "-", false));
				} else {					
					String v = GenotypeLib.getEscapedValue(p, c.field);
					if (v != null)
						data.add(new WText(new WString(v)));
					else
						data.add(null);
				}
			}
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
	public JobOverviewSummary getSummary(String filter) {
		return new DefaultJobOverviewSummary(this);
	}

	@Override
	public void handleInternalPath(String internalPath) {
	}
}
