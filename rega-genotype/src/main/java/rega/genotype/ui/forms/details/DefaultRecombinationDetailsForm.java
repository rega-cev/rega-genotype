/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms.details;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.RecombinationForm;
import rega.genotype.ui.recombination.RecombinationPlot;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;

/**
 * A default extension of IDetailsForm for visualizing recombination details, used by different virus implementations.
 */
public class DefaultRecombinationDetailsForm extends IDetailsForm {
	private String path;
	private String type;
	private WString title;
	
	public DefaultRecombinationDetailsForm(String path, String type, WString title){
		this.path = path;
		this.type = type;
		this.title = title;
		setStyleClass("recombinationDetails");
	}
	@Override
	public void fillForm(GenotypeResultParser p, OrganismDefinition od, File jobDir) {
		try {
			initRecombinationSection(p, jobDir, od);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void initRecombinationSection(GenotypeResultParser p, File jobDir, OrganismDefinition od) throws UnsupportedEncodingException, IOException {
		if(p.elementExists("/genotype_result/sequence/result[@id='scan']/recombination")){
			WAnchor detailed = new WAnchor("", tr("defaultRecombinationAnalyses.detailedRecombination"));
			detailed.setObjectName("report-" + p.getSequenceIndex());
			detailed.setStyleClass("link");
			detailed.setRefInternalPath(RecombinationForm.recombinationPath(jobDir, p.getSequenceIndex()));
			addWidget(detailed);
			addWidget(new WBreak());
		}
		
		addWidget(new WText(tr("defaultRecombinationAnalyses.sequenceName")));
		addWidget(new WText(p.getEscapedValue("/genotype_result/sequence/@name")));
		addWidget(new WBreak());
		RecombinationPlot plot = new RecombinationPlot(p.getValue(path+"/data"), od);
		addWidget(plot);
		addWidget(new WBreak());
		addWidget(new WText(tr("defaultRecombinationAnalyses.bootscanClusterSupport")));
		addWidget(new WText(p.getEscapedValue(path+"/support[@id='best']")));
		addWidget(new WBreak());
		addWidget(new WText(tr("defaultRecombinationAnalyses.download").getValue() +" "));
		addWidget(GenotypeLib.getAnchor("CSV", "application/excel", plot.getRecombinationCSV(jobDir, p.getSequenceIndex(), type), null));
		addWidget(new WText(", "));
		addWidget(GenotypeLib.getAnchor(" PDF ", "application/pdf", plot.getRecombinationPDF(jobDir, p.getSequenceIndex(), type), null));
		addWidget(new WBreak());
		WString m = tr("defaultRecombinationAnalyses.bootscanAnalysis");
		m.arg(p.getValue(path+"/window"));
		m.arg(p.getValue(path+"/step"));
		addWidget(new WText(m));
		this.setContentAlignment(AlignmentFlag.AlignCenter);
	}
	
	@Override
	public WString getComment() {
		return tr("defaultRecombinationAnalyses.comment");
	}

	@Override
	public WString getTitle() {
		return title;
	}

	@Override
	public WString getExtraComment() {
		return null;
	}

	@Override
	public String getIdentifier() {
		return "recombination-detail";
	}
}
