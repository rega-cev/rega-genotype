/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

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
 * NoV assignment-details implementation.
 * 
 * @author simbre1
 *
 */
public class NovSequenceAssignmentForm extends IDetailsForm {
	public NovSequenceAssignmentForm() {
	}

	@Override
	public void fillForm(GenotypeResultParser p, final OrganismDefinition od, File jobDir) {
		WContainerWidget block = new WContainerWidget(this);
		block.setId("");

		WText t = new WText(tr("defaultSequenceAssignment.name-length")
				.arg(p.getEscapedValue("genotype_result.sequence[name]"))
				.arg(p.getEscapedValue("genotype_result.sequence[length]")), block);
		t.setId("");

		String blastConclusion = NovResults.getBlastConclusion(p);
		if (!blastConclusion.equals(NovResults.NA)) {
			t = new WText(tr("nrvSequenceAssignment.blast")
					.arg(blastConclusion)
					.arg(NovResults.getBlastMotivation(p)), block);
			t.setId("");
		}

		for (int i = 1; i <= 2; ++i) {
			String orf = "ORF" + i;
			NovResults.Conclusion c = NovResults.getConclusion(p, orf);

			WString motivation = WString.lt(c.majorMotivation);
			motivation.arg(c.majorBootstrap);

			t = new WText(tr("nrvSequenceAssignment.phylo")
					.arg(orf)
					.arg(c.majorAssignment)
					.arg(motivation), block);
			t.setId("");

			if (c.variantDescription != null) {
				motivation = WString.lt(c.variantMotivation);
				motivation.arg(c.variantBootstrap);

				t = new WText(tr("nrvSequenceAssignment.phylo-variant")
						.arg(c.variantDescription)
						.arg(motivation), block);
				t.setId("");
			}
		}

		t = new WText(lt("<h3>Genome region</h3>"), block);
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
	public String getId() {
		return "assignment";
	}
}
