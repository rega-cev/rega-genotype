/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms.details;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.RecombinationForm;
import rega.genotype.ui.framework.widgets.WListContainerWidget;
import rega.genotype.ui.recombination.RecombinationPlot;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

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
	
	private void initRecombinationSection(final GenotypeResultParser p, final File jobDir, OrganismDefinition od)
		throws UnsupportedEncodingException, IOException {

		// FIXME: we should not show a link to a detailed recombination section if we concluded a pure or CRF
		// this should be passed as an argument ?

		if (p.elementExists(path + "/recombination")) {
			WAnchor detailed = new WAnchor("", tr("defaultRecombinationAnalyses.detailedRecombination"));
			detailed.setObjectName("report-" + p.getSequenceIndex());
			detailed.setStyleClass("link");
			detailed.setRefInternalPath(RecombinationForm.recombinationPath(jobDir, p.getSequenceIndex(), type));
			addWidget(detailed);
			addWidget(new WBreak());
		}

		final RecombinationPlot plot = new RecombinationPlot(p.getValue(path+"/data"), od);
		addWidget(plot);
		addWidget(new WText(tr("defaultRecombinationAnalyses.bootscanClusterSupport")));
		addWidget(new WText(GenotypeLib.getEscapedValue(p, path+"/support[@id='best']")));
		addWidget(new WBreak());
		addWidget(new WText(tr("defaultRecombinationAnalyses.download").getValue() +" "));
		WAnchor csv = new WAnchor();
		csv.setText("CSV");
		csv.setLink(new WLink(new WResource() {
					protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
						plot.streamRecombinationCSV(jobDir, p.getSequenceIndex(), type, response.getOutputStream());
					}}));
		csv.setTarget(AnchorTarget.TargetNewWindow);
		addWidget(csv);
		addWidget(new WText(", "));
		final int sequenceIndex = p.getSequenceIndex();
		WAnchor pdf = new WAnchor();
		pdf.setText("PDF");
		pdf.setLink(new WLink(new WResource() {
			protected void handleRequest(WebRequest request, WebResponse response) throws IOException {
				try {
					plot.streamRecombinationPDF(jobDir, sequenceIndex, type, response.getOutputStream());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}}));
		pdf.setTarget(AnchorTarget.TargetNewWindow);
		addWidget(pdf);
		addWidget(new WBreak());
		WString m = tr("defaultRecombinationAnalyses.bootscanAnalysis");
		m.arg(p.getValue(path + "/window"));
		m.arg(p.getValue(path + "/step"));
		addWidget(new WText(m));
		
		addWidget(new WBreak());
		addWidget(new WBreak());
		WContainerWidget recombinationProfile = new WContainerWidget(this);
		recombinationProfile.addWidget(new WText(tr("defaultRecombinationAnalyses.profile")));
		WListContainerWidget profileList = new WListContainerWidget(recombinationProfile);
		profileList.addItem(tr("defaultRecombinationAnalyses.profile.short")
				.arg(toShortProfile(p.getValue(path + "/profile[@id='assigned']"))));
		profileList.addItem(tr("defaultRecombinationAnalyses.profile.long")
				.arg(p.getValue(path + "/profile[@id='assigned']")));
		profileList.addItem(tr("defaultRecombinationAnalyses.profile.best")
				.arg(p.getValue(path + "/profile[@id='best']")));
		recombinationProfile.setContentAlignment(AlignmentFlag.AlignLeft);
		
		this.setContentAlignment(AlignmentFlag.AlignCenter);
	}

	private String toShortProfile(String profile) {
		String shortProfile = "";
		for (String p : profile.split(" ")) {
			if (!shortProfile.contains(p)) 
				shortProfile += p + " ";
		}
		return shortProfile;
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
