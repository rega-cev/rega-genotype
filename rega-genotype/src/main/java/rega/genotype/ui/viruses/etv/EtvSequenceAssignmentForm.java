/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import java.io.File;
import java.io.IOException;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;

/**
 * Enterovirus assignment-details implementation.
 */
public class EtvSequenceAssignmentForm extends IDetailsForm {
	public EtvSequenceAssignmentForm() {
	}

	@Override
	public void fillForm(GenotypeResultParser p, final OrganismDefinition od, File jobDir) {
		WContainerWidget block = new WContainerWidget(this);
		block.setId("");

		WText t = new WText(tr("defaultSequenceAssignment.name-length")
				.arg(p.getEscapedValue("genotype_result.sequence[name]"))
				.arg(p.getEscapedValue("genotype_result.sequence[length]")), block);
		t.setId("");

		boolean havePhyloAnalysis = p.getValue("genotype_result.sequence.result['phylo-serotype'].best.id") != null;
		boolean haveBlastAssignment = havePhyloAnalysis || p.getValue("genotype_result.sequence.conclusion['unassigned'].assigned.id") == null;

		if (haveBlastAssignment) {
			String blastConclusion = p.getEscapedValue("genotype_result.sequence.result['blast'].cluster.name");
			String blastScore = p.getEscapedValue("genotype_result.sequence.result['blast'].cluster.score");
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

		int start = 0;
		int end = 0;
		try {
			start = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].start"));
			end = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].end"));
		} catch (NumberFormatException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			WImage genome = GenotypeLib.getWImageFromFile(od.getGenome().getGenomePNG(jobDir, p.getSequenceIndex(), "-", start, end, 0, "nrv", null));
			genome.setId("");
			block.addWidget(genome);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		WString refSeq = tr("defaultSequenceAssignment.referenceSequence");
		refSeq.arg(start);
		refSeq.arg(end);
		refSeq.arg(p.getEscapedValue("genotype_result.sequence.result['blast'].refseq"));

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
