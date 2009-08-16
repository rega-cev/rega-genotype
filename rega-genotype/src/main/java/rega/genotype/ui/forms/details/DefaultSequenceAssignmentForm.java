/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms.details;

import java.io.File;
import java.io.IOException;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;

/**
 * A default extension of IDetailsForm for visualizing the genotypic assignment, used by different virus implementations.
 */
public class DefaultSequenceAssignmentForm extends IDetailsForm {
	private WContainerWidget text;
	private WContainerWidget images;
	private WContainerWidget motivation;
	private int genomeVariantCount;
	private String genomeDataXPath;

	public DefaultSequenceAssignmentForm(int genomeVariantCount, String genomeDataXPath) {
		text = new WContainerWidget(this);
		text.setStyleClass("dsa-text");
		images = new WContainerWidget(this);
		images.setStyleClass("dsa-images");
		motivation = new WContainerWidget(this);
		motivation.setStyleClass("dsa-motivation");
		
		this.genomeVariantCount = genomeVariantCount;
		this.genomeDataXPath = genomeDataXPath;
	}

	@Override
	public void fillForm(SaxParser p, final OrganismDefinition od, File jobDir) {
		String id;

		if (!p.elementExists("genotype_result.sequence.conclusion")) {
			id = "-";
		} else {
			id = p.getEscapedValue("genotype_result.sequence.conclusion.assigned.id");
		}
			
		text.clear();
		
		new WText(tr("defaultSequenceAssignment.name-length")
				.arg(p.getEscapedValue("genotype_result.sequence[name]"))
				.arg(p.getEscapedValue("genotype_result.sequence[length]")), text);
		
		text.addWidget(new WText(tr("defaultSequenceAssignment.assignment")));

		if(!p.elementExists("genotype_result.sequence.conclusion")) {
			text.addWidget(new WText(lt(" Sequence error")));
		} else {
			text.addWidget(new WText(lt(" " +p.getEscapedValue("genotype_result.sequence.conclusion.assigned.name"))));
		}
		text.addWidget(new WText(lt(", ")));
		text.addWidget(new WText(tr("defaultSequenceAssignment.bootstrap")));
		if(!p.elementExists("genotype_result.sequence.conclusion.assigned.support")) {
			text.addWidget(new WText(lt(" NA")));
		} else {
			text.addWidget(new WText(lt(" " +p.getEscapedValue("genotype_result.sequence.conclusion.assigned.support")+"%")));
		}
		
		int start = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].start"));
		int end = Integer.parseInt(p.getValue("genotype_result.sequence.result['blast'].end"));
		String csvData = genomeDataXPath != null ? p.getValue(genomeDataXPath) : null;
		
		images.clear();
		try {
			WImage legend = GenotypeLib.getWImageFromResource(od, "legend.png", null);
			legend.setStyleClass("legend");
			images.addWidget(legend);

			for (int i = 0; i < genomeVariantCount; ++i) {
				WImage genome = GenotypeLib.getWImageFromFile(od.getGenome().getGenomePNG(jobDir, p.getSequenceIndex(), id, start, end, i, "pure", csvData));
				images.addWidget(genome);
			}
			
			motivation.clear();

			motivation.addWidget(new WBreak());
			WString refSeq = tr("defaultSequenceAssignment.referenceSequence");
			refSeq.arg(start);
			refSeq.arg(end);
			refSeq.arg(p.getEscapedValue("genotype_result.sequence.result['blast'].refseq"));
			WText refSeqWidget = new WText(refSeq);
			refSeqWidget.setStyleClass("refseq");
			motivation.addWidget(refSeqWidget);

			motivation.addWidget(new WBreak());
			motivation.addWidget(new WText(tr("defaultSequenceAssignment.motivation")));
			if(!p.elementExists("genotype_result.sequence.conclusion")) {
				motivation.addWidget(new WText(lt(p.getEscapedValue("genotype_result.sequence.error"))));
			} else {
				motivation.addWidget(new WText(lt(p.getEscapedValue("genotype_result.sequence.conclusion.motivation"))));
			}
		} catch (IOException e) {
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
}
