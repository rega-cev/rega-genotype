/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.parasites.giardia;

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
public class GiardiaSequenceAssignmentForm extends IDetailsForm {
	public GiardiaSequenceAssignmentForm() {
	}

	@Override
	public void fillForm(GenotypeResultParser p, final OrganismDefinition od, File jobDir) {
		WContainerWidget block = new WContainerWidget(this);
		block.setId("");

		WText t = new WText(tr("defaultSequenceAssignment.name-length")
				.arg(p.getEscapedValue("genotype_result.sequence[name]"))
				.arg(p.getEscapedValue("genotype_result.sequence[length]")), block);
		t.setId("");

		boolean hasAssignment = p.getEscapedValue("genotype_result.sequence.conclusion.assigned.support") != null;
		String assignedId = "-";
		for (String region : GiardiaGenome.regions) {
			String phyloResult = p.getEscapedValue("genotype_result.sequence.result['phylo-" + region + "'].best.id");
			if (phyloResult != null) {
				if (hasAssignment)
					assignedId = phyloResult;

				WString motivation = new WString(p.getEscapedValue("genotype_result.sequence.conclusion.motivation"));
				motivation.arg(p.getValue("genotype_result.sequence.conclusion.assigned.support"));

				t = new WText(tr("sequenceAssignment.phylo")
						.arg(region)
						.arg(p.getValue("genotype_result.sequence.conclusion.assigned.name"))
						.arg(motivation), block);
				t.setId("");
			}
		}

		t = new WText("<h3>Genome region</h3>", block);
		t.setId("");

		try {
			int start = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].start"));
			int end = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].end"));
			String region = p.getValue("genotype_result.sequence.result['blast'].cluster.id");

			if (region != null) {
				System.err.println(region);
				start = GiardiaGenome.mapToImageGenome(start, region);
				end = GiardiaGenome.mapToImageGenome(end, region);
			} else {
				start = 0; end = 0;
			}

			WImage genome = GenotypeLib.getWImageFromFile(od.getGenome().getGenomePNG(jobDir, p.getSequenceIndex(), assignedId, start, end, 0, "", null));
			genome.setId("");
			block.addWidget(genome);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
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
