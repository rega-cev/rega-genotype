/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import java.io.File;
import java.io.IOException;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
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
	public void fillForm(SaxParser p, final OrganismDefinition od, File jobDir) {
		WContainerWidget block = new WContainerWidget(this);

		new WText(tr("defaultSequenceAssignment.name-length")
				.arg(p.getEscapedValue("genotype_result.sequence[name]"))
				.arg(p.getEscapedValue("genotype_result.sequence[length]")), block);

		String blastConclusion = NovResults.getBlastConclusion(p);
		if (!blastConclusion.equals(NovResults.NA)) {
			new WText(tr("nrvSequenceAssignment.blast")
					.arg(blastConclusion)
					.arg(NovResults.getBlastMotivation(p)), block);
		}

		for (int i = 1; i <= 2; ++i) {
			String orf = "ORF" + i;
			NovResults.Conclusion c = NovResults.getConclusion(p, orf);

			WString motivation = WString.lt(c.majorMotivation);
			motivation.arg(c.majorBootstrap);

			new WText(tr("nrvSequenceAssignment.phylo")
					.arg(orf)
					.arg(c.majorAssignment)
					.arg(motivation), block);

			if (c.variantDescription != null) {
				motivation = WString.lt(c.variantMotivation);
				motivation.arg(c.variantBootstrap);

				new WText(tr("nrvSequenceAssignment.phylo-variant")
						.arg(c.variantDescription)
						.arg(motivation), block);
			}
		}

		new WText(lt("<h2>Genome region</h2>"), block);

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

		new WText(refSeq, block);
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
}
