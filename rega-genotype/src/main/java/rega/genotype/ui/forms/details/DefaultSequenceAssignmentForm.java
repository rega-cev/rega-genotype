/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms.details;

import java.io.File;
import java.io.IOException;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

/**
 * A default extension of IDetailsForm for visualizing the genotypic assignment, used by different virus implementations.
 */
public class DefaultSequenceAssignmentForm extends IDetailsForm {
	private WContainerWidget text;
	private WContainerWidget images;
	private WContainerWidget motivation;
	private int genomeVariantCount;

	public DefaultSequenceAssignmentForm(int genomeVariantCount) {
		text = new WContainerWidget(this);
		text.setStyleClass("dsa-text");
		images = new WContainerWidget(this);
		images.setStyleClass("dsa-images");
		motivation = new WContainerWidget(this);
		motivation.setStyleClass("dsa-motivation");
		
		this.genomeVariantCount = genomeVariantCount;
	}

	@Override
	public void fillForm(final GenotypeResultParser p, final OrganismDefinition od, final File jobDir) {
		final String id;

		if (!p.elementExists("/genotype_result/sequence/conclusion")) {
			id = "-";
		} else {
			id = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/assigned/id");
		}
			
		text.clear();
		
		new WText(tr("defaultSequenceAssignment.name-length")
				.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name"))
				.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@length")), text);

		String assignment, bootstrap;
		
		if (!p.elementExists("/genotype_result/sequence/conclusion"))
			assignment = " Sequence error";
		else
			assignment = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/assigned/name");
	
		if (!p.elementExists("/genotype_result/sequence/conclusion/assigned/support"))
			bootstrap = "NA";
		else
			bootstrap = GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/assigned/support") + "%";

		text.addWidget(new WText(tr("defaultSequenceAssignment.assignment-bootstrap")
				.arg(assignment)
				.arg(bootstrap)));

		final int start = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/start"));
		final int end = Integer.parseInt(p.getValue("/genotype_result/sequence/result[@id='blast']/end"));
		
		final String scanType = od.getProfileScanType(p);
		final int sequenceIndex = p.getSequenceIndex();
		final String csvData = p.getValue("/genotype_result/sequence/result[@id='scan-" + scanType + "']/data");

		images.clear();
		WImage legend = GenotypeLib.getWImageFromResource(od, "legend.png", null);
		legend.setStyleClass("legend");
		images.addWidget(legend);

		for (int i = 0; i < genomeVariantCount; ++i) {
			final int index = i;

			WImage genome = GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
				@Override
				public void handleRequest(WebRequest request, WebResponse response) {
					try {
						if (getFileName().isEmpty()) {
							File file = od.getGenome().getGenomePNG(jobDir, sequenceIndex, id, start, end, index, scanType, csvData);
							setFileName(file.getAbsolutePath());
						}
						super.handleRequest(request, response);
					} catch (NumberFormatException e) {
						throw new RuntimeException(e);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					super.handleRequest(request, response);
				} });

			images.addWidget(genome);
		}
		
		motivation.clear();

		motivation.addWidget(new WBreak());
		WString refSeq = tr("defaultSequenceAssignment.referenceSequence");
		refSeq.arg(start);
		refSeq.arg(end);
		refSeq.arg(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/result[@id='blast']/refseq"));
		WText refSeqWidget = new WText(refSeq);
		refSeqWidget.setStyleClass("refseq");
		motivation.addWidget(refSeqWidget);

		motivation.addWidget(new WBreak());
		motivation.addWidget(new WText(tr("defaultSequenceAssignment.motivation")));
		if(!p.elementExists("/genotype_result/sequence/conclusion")) {
			motivation.addWidget(new WText(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/error")));
		} else {
			motivation.addWidget(new WText(GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/conclusion/motivation")));
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
