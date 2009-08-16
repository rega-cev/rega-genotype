/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;

/**
 * A default extension of AbstractJobOverview, used by different virus implementations.
 * 
 * It provides 6 columns for the table.
 */
public class DefaultJobOverview extends AbstractJobOverview {
	private List<Header> headers = new ArrayList<Header>();
	private DefaultJobOverviewSummary summary = new DefaultJobOverviewSummary();
	
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

		data.add(new WText(p.getEscapedValue("genotype_result.sequence[name]")));
		data.add(new WText(p.getEscapedValue("genotype_result.sequence[length]")));
		
		WAnchor report = createReportLink(p);
		data.add(report);

		String id;
		if (!p.elementExists("genotype_result.sequence.conclusion")) {
			id = "-";
			data.add(new WText("NA"));
			data.add(new WText("NA"));
		} else {
			id = p.getEscapedValue("genotype_result.sequence.conclusion.assigned.id");
			data.add(new WText(p.getEscapedValue("genotype_result.sequence.conclusion.assigned.name")));
			
			String support = p.getEscapedValue("genotype_result.sequence.conclusion.assigned.support");
			if(support==null) {
				support = "NA";
			}
			data.add(new WText(support));
			try {
				data.add(GenotypeLib.getWImageFromFile(getMain().getOrganismDefinition().getGenome().getSmallGenomePNG(jobDir, p.getSequenceIndex(), 
						id,
						Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].start")), 
						Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].end")),
						0, 
						"pure", 
						p.getValue("genotype_result.sequence.result['scan'].data"))));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return data;
	}

	@Override
	public List<Header> getHeaders() {
		return headers;
	}

	@Override
	public JobOverviewSummary getSummary(String filter) {
		//TODO
		return summary;
	}
}
