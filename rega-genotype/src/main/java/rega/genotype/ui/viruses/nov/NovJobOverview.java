/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.DefaultJobOverviewSummary;
import rega.genotype.ui.forms.JobOverviewSummary;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

/**
 * NoV job overview implementation.
 */
public class NovJobOverview extends AbstractJobOverview {
	private static final int NOVII_TO_NOVI_POSITION_OFFSET = 260;
	private List<Header> headers = new ArrayList<Header>();
	private List<WWidget> data = new ArrayList<WWidget>();
	
	public NovJobOverview(GenotypeWindow main) {
		super(main);
		
		headers.add(new Header(new WString("Name")));
		headers.add(new Header(new WString("Length")));
		headers.add(new Header(new WString("Report")));
		headers.add(new Header(new WString("Genus / Genogroup")));
		headers.add(new Header(new WString("ORF 1"), 2));
		headers.add(new Header(new WString("ORF 2"), 2));
		headers.add(new Header(new WString("Genome")));
	}
	
	@Override
	public List<WWidget> getData(final GenotypeResultParser p) {
		data.clear();

		data.add(new WText(new WString(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name"))));
		data.add(new WText(new WString(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@length"))));

		WAnchor report = createReportLink(p);
		data.add(report);

		String blastResult = NovResults.getBlastConclusion(p);
		data.add(new WText(new WString(notNull(blastResult))));
		
		NovResults.Conclusion c = NovResults.getConclusion(p, "ORF1");

		data.add(new WText(new WString(notNull(c.majorAssignment))));
		data.add(new WText(new WString(notNull(c.variantAssignmentForOverview))));

		c = NovResults.getConclusion(p, "ORF2");

		data.add(new WText(new WString(notNull(c.majorAssignment))));
		data.add(new WText(new WString(notNull(c.variantAssignmentForOverview))));

		data.add(createGenomeImage(p, "-", blastResult.equals("Unassigned")));
		
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

	/**
	 * Adjusts the blast-determined position for novII, since these are offset but we use
	 * the same image.
	 */
	public static int getAdjustedImageStart(GenotypeResultParser p, boolean unassigned) {
		int start = -1;
		
		if (!unassigned) {
			String value = p.getValue("/genotype_result/sequence/result[@id='blast']/start");
			if (value != null) {
				start = Integer.parseInt(value);
			
				if (p.getValue("/genotype_result/sequence/result[@id='blast']/cluster/id").equals("II"))
					start += NOVII_TO_NOVI_POSITION_OFFSET;
			}
		}
		
		return start;
	}
	

	public static int getAdjustedImageEnd(GenotypeResultParser p, boolean unassigned) {
		int end = -1;
		
		if (!unassigned) {
			String value = p.getValue("/genotype_result/sequence/result[@id='blast']/end");
			if (value != null) {
				end = Integer.parseInt(value);
			
				if (p.getValue("/genotype_result/sequence/result[@id='blast']/cluster/id").equals("II"))
					end += NOVII_TO_NOVI_POSITION_OFFSET;
			}
		}
		return end;
	}

	@Override
	protected WImage createGenomeImage(GenotypeResultParser p, final String assignedId, boolean unassigned) {
		
		final int sequenceIndex = p.getSequenceIndex();
		final int start = getAdjustedImageStart(p, unassigned);
		final int end = getAdjustedImageEnd(p, unassigned);

		return GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
			@Override
			public void handleRequest(WebRequest request, WebResponse response) {
				try {
					if (getFileName().isEmpty()) {
						File file = getMain().getOrganismDefinition().getGenome().getSmallGenomePNG(jobDir, sequenceIndex, assignedId, start, end, 0, "", null);
						setFileName(file.getAbsolutePath());
					}
	
					super.handleRequest(request, response);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}				
		});
	}

	@Override
	public void handleInternalPath(String internalPath) {
	}

}
