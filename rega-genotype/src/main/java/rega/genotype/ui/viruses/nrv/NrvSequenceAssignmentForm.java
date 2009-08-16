package rega.genotype.ui.viruses.nrv;

import java.io.File;
import java.io.IOException;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WImage;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.i8n.WArgMessage;
import net.sf.witty.wt.i8n.WMessage;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.util.GenotypeLib;

public class NrvSequenceAssignmentForm extends IDetailsForm {
	public NrvSequenceAssignmentForm() {
	}

	@Override
	public void fillForm(SaxParser p, final OrganismDefinition od, File jobDir) {
		WContainerWidget block = new WContainerWidget(this);
		block.setStyleClass("assignment");

		block.addWidget(new WText(tr("defaultSequenceAssignment.sequenceName")));
		block.addWidget(new WText(lt(p.getValue("genotype_result.sequence[name]")+", ")));
		block.addWidget(new WText(tr("defaultSequenceAssignment.sequenceLength")));
		block.addWidget(new WText(lt(p.getValue("genotype_result.sequence[length]"))));
		block.addWidget(new WBreak());

		block = new WContainerWidget(this);

		String blastConclusion = NrvResults.getBlastConclusion(p);
		if (!blastConclusion.equals(NrvResults.NA)) {
			block.addWidget(new WText(lt("Assignment: " + blastConclusion + "<br />")));
			block.addWidget(new WText(lt("Motivation: " + NrvResults.getBlastMotivation(p))));
		} else {
			block.addWidget(new WText(lt("ORF1 assignment: " + NrvResults.getConclusion(p, "ORF1") + "<br />")));
			block.addWidget(new WText(lt("Motivation: " + NrvResults.getMotivation(p, "ORF1"))));

			block = new WContainerWidget(this);

			block.addWidget(new WText(lt("ORF2 assignment: " + NrvResults.getConclusion(p, "ORF2") + "<br />")));
			block.addWidget(new WText(lt("Motivation: " + NrvResults.getMotivation(p, "ORF2"))));
		}
		
		int start = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].start"));
		int end = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].end"));

		try {
			WImage genome = GenotypeLib.getWImageFromFile(od.getGenome().getGenomePNG(jobDir, p.getSequenceIndex(), "-", start, end, 0, "nrv", null));
			block.addWidget(genome);
		} catch (IOException e) {
			e.printStackTrace();
		}

		block = new WContainerWidget(this);

		WArgMessage refSeq = new WArgMessage("defaultSequenceAssignment.referenceSequence");
		refSeq.addArgument("${start}", start);
		refSeq.addArgument("${end}", end);
		refSeq.addArgument("${refSeq}", p.getValue("genotype_result.sequence.result['blast'].refseq"));
		WText refSeqWidget = new WText(refSeq);
		refSeqWidget.setStyleClass("refseq");
		block.addWidget(refSeqWidget);
	}

	@Override
	public WMessage getComment() {
		return null;
	}

	@Override
	public WMessage getTitle() {
		return tr("defaultSequenceAssignment.title");
	}

	@Override
	public WMessage getExtraComment() {
		return null;
	}
}
