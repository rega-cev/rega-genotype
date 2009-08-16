package rega.genotype.ui.forms.details;

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

public class DefaultSequenceAssignmentForm extends IDetailsForm {
	private WTable mainTable;
	private WContainerWidget text;
	private WContainerWidget motivation;
	private int genomeVariantCount;
	private String genomeDataXPath;

	public DefaultSequenceAssignmentForm(int genomeVariantCount, String genomeDataXPath) {
		mainTable = new WTable(this);
		text = new WContainerWidget(mainTable.elementAt(0, 0));
		motivation = new WContainerWidget(mainTable.elementAt(genomeVariantCount + 1, 0));
		
		this.genomeVariantCount = genomeVariantCount;
		this.genomeDataXPath = genomeDataXPath;
	}

	@Override
	public void fillForm(SaxParser p, final OrganismDefinition od, File jobDir) {
		String id;
		if(!p.elementExists("genotype_result.sequence.conclusion")) {
			id = "-";
		} else {
			id = p.getValue("genotype_result.sequence.conclusion.assigned.id");
		}
			
		text.clear();
		text.addWidget(new WText(tr("defaultSequenceAssignment.sequenceName")));
		text.addWidget(new WText(lt(p.getValue("genotype_result.sequence[name]")+", ")));
		text.addWidget(new WText(tr("defaultSequenceAssignment.sequenceLength")));
		text.addWidget(new WText(lt(p.getValue("genotype_result.sequence[length]"))));
		text.addWidget(new WBreak());
		text.addWidget(new WText(tr("defaultSequenceAssignment.assignment")));
		if(!p.elementExists("genotype_result.sequence.conclusion")) {
			text.addWidget(new WText(lt(" Sequence error")));
		} else {
			text.addWidget(new WText(lt(" " +p.getValue("genotype_result.sequence.conclusion.assigned.name"))));
		}
		text.addWidget(new WText(lt(", ")));
		text.addWidget(new WText(tr("defaultSequenceAssignment.bootstrap")));
		if(!p.elementExists("genotype_result.sequence.conclusion.assigned.support")) {
			text.addWidget(new WText(lt(" NA")));
		} else {
			text.addWidget(new WText(lt(" " +p.getValue("genotype_result.sequence.conclusion.assigned.support")+"%")));
		}
		
		int start = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].start"));
		int end = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].end"));
		String csvData = genomeDataXPath != null ? p.getValue(genomeDataXPath) : null;
		try {
			for (int i = 0; i < genomeVariantCount; ++i) {
				WImage genome = GenotypeLib.getWImageFromFile(od.getGenome().getGenomePNG(jobDir, p.getSequenceIndex(), id, start, end, i, "pure", csvData));
				mainTable.putElementAt(i + 1, 0, genome);
			}
				
			WImage legend = GenotypeLib.getWImageFromResource(od, "legend.png", null);
			mainTable.elementAt(1, 1).setRowSpan(genomeVariantCount);
			
			motivation.clear();

			motivation.addWidget(new WBreak());
			WArgMessage refSeq = new WArgMessage("defaultSequenceAssignment.referenceSequence");
			refSeq.addArgument("${start}", start);
			refSeq.addArgument("${end}", end);
			refSeq.addArgument("${refSeq}", p.getValue("genotype_result.sequence.result[blast].refseq"));
			WText refSeqWidget = new WText(refSeq);
			refSeqWidget.setStyleClass("refseq");
			motivation.addWidget(refSeqWidget);

			motivation.addWidget(new WBreak());
			motivation.addWidget(new WText(tr("defaultSequenceAssignment.motivation")));
			if(!p.elementExists("genotype_result.sequence.conclusion")) {
				motivation.addWidget(new WText(lt(p.getValue("genotype_result.sequence.error"))));
			} else {
				motivation.addWidget(new WText(lt(p.getValue("genotype_result.sequence.conclusion.motivation"))));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
