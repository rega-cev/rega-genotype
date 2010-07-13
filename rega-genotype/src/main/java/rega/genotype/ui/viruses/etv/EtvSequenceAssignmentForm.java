/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

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
public class EtvSequenceAssignmentForm extends IDetailsForm {
	public EtvSequenceAssignmentForm() {
	}

	@Override
	public void fillForm(GenotypeResultParser p, final OrganismDefinition od, final File jobDir) {
		WContainerWidget block = new WContainerWidget(this);
		block.setId("");

		WText t = new WText(tr("defaultSequenceAssignment.name-length")
				.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name"))
				.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@length")), block);
		t.setId("");

		boolean havePhyloAnalysis = p.getValue("/genotype_result/sequence/result[@id='phylo-serotype']/best/id") != null;
		boolean haveBlastAssignment = havePhyloAnalysis || p.getValue("/genotype_result/sequence/conclusion[@id='unassigned']/assigned/id") == null;

		if (haveBlastAssignment) {
			String blastConclusion = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/cluster/name");
			String blastScore = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/cluster/score");
			t = new WText(tr("etvSequenceAssignment.blast").arg(blastConclusion).arg(blastScore), block);
			t.setId("");
		}

		if (havePhyloAnalysis) {
			EtvResults.Conclusion c = EtvResults.getSerotype(p);

			WString motivation = new WString(c.majorMotivation);
			motivation.arg(c.majorBootstrap);

			t = new WText(tr("etvSequenceAssignment.phylo")
						.arg("Serotype (VP1)")
						.arg(c.majorAssignment)
						.arg(motivation), block);
			t.setId("");
		}

		t = new WText("<h3>Genome region</h3>", block);
		t.setId("");

		final int start = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/start"));
		final int end = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/end"));
		final int sequenceIndex = p.getSequenceIndex();

		WImage genome = GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
			@Override
			public void handleRequest(WebRequest request, WebResponse response) {
				try {
					if (getFileName().isEmpty()) {
						File file = od.getGenome().getGenomePNG(jobDir, sequenceIndex, "-", start, end, 0, "etv", null);
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

		WString refSeq = tr("defaultSequenceAssignment.referenceSequence");
		refSeq.arg(start);
		refSeq.arg(end);
		refSeq.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/refseq"));

		t = new WText(refSeq, block);
		t.setId("");
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
