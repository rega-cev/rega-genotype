/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.data.GenotypeResultParser;
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
 * A default extension of AbstractJobOverview, used by different virus implementations.
 * 
 * It provides 6 columns for the table.
 */
public class DefaultJobOverview extends AbstractJobOverview {
	private List<Header> headers = new ArrayList<Header>();
	
	public DefaultJobOverview(GenotypeWindow main) {
		super(main);
		
		headers.add(new Header(new WString("Name")));
		headers.add(new Header(new WString("Length")));
		headers.add(new Header(new WString("Report")));
		headers.add(new Header(new WString("Assignment")));
		headers.add(new Header(new WString("Support")));
		headers.add(new Header(new WString("Genome")));
	}
	
	@Override
	public List<WWidget> getData(final GenotypeResultParser p) {
		List<WWidget> data = new ArrayList<WWidget>();

		data.add(new WText(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name")));
		data.add(new WText(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@length")));
		
		WAnchor report = createReportLink(p);
		data.add(report);

		final String id;
		if (!p.elementExists("/genotype_result/sequence/conclusion")) {
			id = "-";
			data.add(new WText("NA"));
			data.add(new WText("NA"));
		} else {
			id = GenotypeLib.getEscapedValue(p,"/genotype_result/sequence/conclusion/assigned/id");
			data.add(new WText(GenotypeLib.getEscapedValue(p,"/genotype_result/sequence/conclusion/assigned/name")));
			
			String support = GenotypeLib.getEscapedValue(p,"/genotype_result/sequence/conclusion/assigned/support");
			if(support==null) {
				support = "NA";
			}
			data.add(new WText(support));

			final String scanType = getMain().getOrganismDefinition().getProfileScanType(p);
			final int start = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/start"));
			final int end = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/end"));
			final int sequenceIndex = p.getSequenceIndex();
			final String csvData = p.getValue("/genotype_result/sequence/result[@id='scan-" + scanType + "']/data");

			data.add(GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
				@Override
				public void handleRequest(WebRequest request, WebResponse response) {
					try {
						if (getFileName().isEmpty()) {
							File file = getMain().getOrganismDefinition().getGenome().getSmallGenomePNG(jobDir, sequenceIndex, id, start, end, 0, scanType, csvData);
							setFileName(file.getAbsolutePath());
						}
						super.handleRequest(request, response);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}				
			}));
		}
		
		return data;
	}

	@Override
	public List<Header> getHeaders() {
		return headers;
	}

	@Override
	public JobOverviewSummary getSummary(String filter) {
		if (filter == null)
			return new DefaultJobOverviewSummary(this);
		else
			return new DefaultJobOverviewFilterSummary();
	}

	@Override
	public void handleInternalPath(String internalPath) {

	}
}
