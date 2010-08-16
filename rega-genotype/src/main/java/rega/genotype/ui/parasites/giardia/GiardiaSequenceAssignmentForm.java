/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.parasites.giardia;

import java.io.File;
import java.io.IOException;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

/**
 * Enterovirus assignment-details implementation.
 */
public class GiardiaSequenceAssignmentForm extends IDetailsForm {
	public GiardiaSequenceAssignmentForm() {
	}

	@Override
	public void fillForm(GenotypeResultParser p, final OrganismDefinition od, final File jobDir) {
		WContainerWidget block = new WContainerWidget(this);
		block.setId("");

		WText t = new WText(tr("defaultSequenceAssignment.name-length")
				.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name"))
				.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@length")), block);
		t.setId("");

		boolean hasAssignment = p.elementExists("/genotype_result/sequence/conclusion/assigned/support");
		String assignedId = "-";
		for (String region : GiardiaGenome.regions) {
			String phyloResult = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='phylo-" + region + "']/best/id");
			if (phyloResult != null) {
				if (hasAssignment)
					assignedId = phyloResult;

				WString motivation = new WString(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/motivation"));
				motivation.arg(p.getValue("/genotype_result/sequence/conclusion/assigned/support"));

				t = new WText(tr("sequenceAssignment")
						.arg(region)
						.arg(p.getValue("/genotype_result/sequence/conclusion/assigned/name"))
						.arg(motivation), block);
				t.setId("");
			} else if (region.equals("16S") 
					&& "16S".equals(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/cluster/id"))) {
				t = new WText(tr("sequenceAssignment")
						.arg(region)
						.arg(region)
						.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/motivation")), block);
				t.setId("");
			}
		}

		t = new WText("<h3>Genome region</h3>", block);
		t.setId("");

		int start = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/start"));
		int end = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/end"));
		String region = p.getValue("/genotype_result/sequence/result[@id='blast']/cluster/id");

		if (region != null) {
			System.err.println(region);
			start = GiardiaGenome.mapToImageGenome(start, region);
			end = GiardiaGenome.mapToImageGenome(end, region);
		} else {
			start = 0; end = 0;
		}

		final int sequenceIndex = p.getSequenceIndex();
		final String finalAssignedId = assignedId;
		final int finalStart = start;
		final int finalEnd = end;

		WImage genome = GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
			@Override
			public void handleRequest(WebRequest request, WebResponse response) {
				try {
					if (getFileName().isEmpty()) {
						File file = od.getGenome().getGenomePNG(jobDir, sequenceIndex, finalAssignedId, finalStart, finalEnd, 0, "", null);
						setFileName(file.getAbsolutePath());
					}
	
					super.handleRequest(request, response);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}				
		});
		
		genome.setId("");
		block.addWidget(genome);
	}

	@Override
	public WString getComment() {
		return null;
	}

	@Override
	public WString getTitle() {
		return tr("defaultSequenceAssignment.title");
	}

	@Override
	public WString getExtraComment() {
		return null;
	}
	
	@Override
	public String getIdentifier() {
		return "assignment";
	}
}
