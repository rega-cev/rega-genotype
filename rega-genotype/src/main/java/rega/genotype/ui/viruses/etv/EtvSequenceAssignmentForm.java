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
import rega.genotype.ui.viruses.nov.NovResults;
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

		String blastConclusion = EtvResults.getBlastConclusion(p);
		if (!blastConclusion.equals(EtvResults.NA)) {
			String blastScore = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/cluster/score");
			t = new WText(tr("etvSequenceAssignment.blast").arg(blastConclusion).arg(blastScore), block);
			t.setId("");
		}

		EtvResults.Conclusion c = EtvResults.getConclusion(p);

		if (c.majorAssignment == null || c.majorAssignment.isEmpty())
			c.majorAssignment = NovResults.NA;
		
		WString motivation = new WString(c.majorMotivation);
		motivation.arg(c.majorBootstrap);

		t = new WText(tr("etvSequenceAssignment.phylo")
				.arg("Serotype (VP1)")
				.arg(c.majorAssignment)
				.arg(motivation), block);
		t.setId("");

		if (c.variantDescription != null) {
			motivation = new WString(c.variantMotivation);
			motivation.arg(c.variantBootstrap);

			t = new WText(tr("etvSequenceAssignment.phylo-variant")
					.arg(c.variantDescription)
					.arg(motivation), block);
			t.setId("");
		}

		t = new WText("<h3>Genome region</h3>", block);
		t.setId("");

		String startV = p.getValue("/genotype_result/sequence/result[@id='blast']/start");
		final int start = startV == null ? -1 : Integer.parseInt(startV);
		String endV = p.getValue("/genotype_result/sequence/result[@id='blast']/end");
		final int end = endV == null ? -1 : Integer.parseInt(endV);
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

		if (start > 0 && end > 0) {
			WString refSeq = tr("defaultSequenceAssignment.referenceSequence");
			refSeq.arg(start);
			refSeq.arg(end);
			refSeq.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/refseq"));

			t = new WText(refSeq, block);
			t.setId("");
		}
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
