/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nrv;

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
 * NRV assignment-details implementation.
 * 
 * @author simbre1
 *
 */
public class NrvSequenceAssignmentForm extends IDetailsForm {
	public NrvSequenceAssignmentForm() {
	}

	@Override
	public void fillForm(SaxParser p, final OrganismDefinition od, File jobDir) {
		WContainerWidget block;
		
		block = new WContainerWidget(this);
		block.setStyleClass("dsa-text");

		new WText(tr("defaultSequenceAssignment.sequenceName"), block)
			.setStyleClass("label");
		new WText(lt(p.getEscapedValue("genotype_result.sequence[name]")+", "), block)
			.setStyleClass("value");
		new WText(tr("defaultSequenceAssignment.sequenceLength"), block)
			.setStyleClass("label");
		new WText(lt(p.getEscapedValue("genotype_result.sequence[length]") + " bps"), block)
			.setStyleClass("value");

		block = new WContainerWidget(this);

		String blastConclusion = NrvResults.getBlastConclusion(p);
		if (!blastConclusion.equals(NrvResults.NA)) {
			new WText(lt("<h2>Genotyping result</h2>"), block);
			
			new WText(tr("nrvSequenceAssignment.assignment"), block);
			new WText(lt(blastConclusion), block);
			new WText(tr("nrvSequenceAssignment.motivation"), block);
			new WText(lt(NrvResults.getBlastMotivation(p)), block);
		} else {
			new WText(lt("<h2>ORF1</h2>"), block);

			new WText(tr("nrvSequenceAssignment.assignment"), block);
			new WText(lt(NrvResults.getConclusion(p, "ORF1")), block);
			new WText(tr("nrvSequenceAssignment.motivation"), block);
			new WText(lt(NrvResults.getMotivation(p, "ORF1")), block);

			new WText(lt("<h2>ORF2</h2>"), block);

			new WText(tr("nrvSequenceAssignment.assignment"), block);
			new WText(lt(NrvResults.getConclusion(p, "ORF2")), block);
			new WText(tr("nrvSequenceAssignment.motivation"), block);
			new WText(lt(NrvResults.getMotivation(p, "ORF2")), block);
		}

		block = new WContainerWidget(this);

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

		block = new WContainerWidget(this);

		WString refSeq = tr("defaultSequenceAssignment.referenceSequence");
		refSeq.arg(start);
		refSeq.arg(end);
		refSeq.arg(p.getEscapedValue("genotype_result.sequence.result['blast'].refseq"));
		WText refSeqWidget = new WText(refSeq);
		refSeqWidget.setStyleClass("refseq");
		block.addWidget(refSeqWidget);
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
