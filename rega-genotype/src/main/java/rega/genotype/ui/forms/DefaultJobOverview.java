/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;

/**
 * A default extension of AbstractJobOverview, used by different virus implementations.
 */
public class DefaultJobOverview extends AbstractJobOverview {
	private List<Header> headers = new ArrayList<Header>();
	private List<WWidget> data = new ArrayList<WWidget>();
	
	public DefaultJobOverview(GenotypeWindow main) {
		super(main);
		
		headers.add(new Header(lt("Name")));
		headers.add(new Header(lt("Length")));
		headers.add(new Header(lt("Report")));
		headers.add(new Header(lt("Assignment")));
		headers.add(new Header(lt("Support")));
		headers.add(new Header(lt("Genome")));
	}
	
	@Override
	public List<WWidget> getData(final SaxParser p) {
		data.clear();
		
		data.add(new WText(lt(p.getEscapedValue("genotype_result.sequence[name]"))));
		data.add(new WText(lt(p.getEscapedValue("genotype_result.sequence[length]"))));
		
		WAnchor report = createReportLink(p);
		data.add(report);

		String id;
		if (!p.elementExists("genotype_result.sequence.conclusion")) {
			id = "-";
			data.add(new WText(lt("NA")));
			data.add(new WText(lt("NA")));
		} else {
			id = p.getEscapedValue("genotype_result.sequence.conclusion.assigned.id");
			data.add(new WText(lt(p.getEscapedValue("genotype_result.sequence.conclusion.assigned.name"))));
			
			String support = p.getEscapedValue("genotype_result.sequence.conclusion.assigned.support");
			if(support==null) {
				support = "NA";
			}
			data.add(new WText(lt(support)));
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

}
