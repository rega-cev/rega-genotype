/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.parasites.giardia;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.JobOverviewSummary;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

/**
 * Enterovirus job overview implementation.
 */
public class GiardiaJobOverview extends AbstractJobOverview {
	private List<Header> headers = new ArrayList<Header>();

	public GiardiaJobOverview(GenotypeWindow main) {
		super(main);
		
		headers.add(new Header(new WString("Name")));
		headers.add(new Header(new WString("Length")));
		headers.add(new Header(new WString("Report")));
		
		for (String region : GiardiaGenome.regions)
			headers.add(new Header(new WString(region)));

		headers.add(new Header(new WString("Genome")));
	}

	@Override
	public List<WWidget> getData(final GenotypeResultParser p) {
		List<WWidget> data = new ArrayList<WWidget>();

		data.add(new WText(new WString(p.getEscapedValue("genotype_result/sequence/@name"))));
		data.add(new WText(new WString(p.getEscapedValue("genotype_result/sequence/@length"))));
		data.add(createReportLink(p));

		boolean hasAssignment = p.getEscapedValue("genotype_result/sequence/conclusion/assigned/support") != null;
		String assignedId = "-";
		for (String region : GiardiaGenome.regions) {
			String phyloResult = p.getEscapedValue("genotype_result/sequence/result[@id='phylo-" + region + "']/best/id");
			if (phyloResult != null)
				if (hasAssignment) {
					assignedId = phyloResult;
					data.add(new WText(phyloResult));
				} else
					data.add(new WText("Could not assign"));
			else
				data.add(new WText());
		}

		int start = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/start"));
		int end = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/end"));
		final int sequenceIndex = p.getSequenceIndex();
		String region = p.getValue("genotype_result/sequence/result[@id='blast']/cluster/id");

		if (region != null) {
			start = GiardiaGenome.mapToImageGenome(start, region);
			end = GiardiaGenome.mapToImageGenome(end, region);
		} else {
			start = 0; end = 0;
		}

		final String finalAssignedId = assignedId;
		final int finalStart = start;
		final int finalEnd = end;
		
		data.add(GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
			@Override
			public void handleRequest(WebRequest request, WebResponse response) {
				try {
					if (getFileName().isEmpty()) {
						File file = getMain().getOrganismDefinition().getGenome().getSmallGenomePNG(jobDir, sequenceIndex, finalAssignedId, finalStart, finalEnd, 0, "", null);
						setFileName(file.getAbsolutePath());
					}
	
					super.handleRequest(request, response);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}				
		}));
		
		return data;
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
