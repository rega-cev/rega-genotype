/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;

import rega.genotype.Constants.Mode;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.framework.widgets.WListContainerWidget;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.TextFormat;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;

/**
 * The DetailsForm widget groups together the different detail modules defined in the 
 * OrganismDefinition implementation.
 */
public class DetailsForm extends AbstractForm {
	private WContainerWidget mainTable;
	private IDetailsForm mainDetails;
	
	private GenotypeResultParser p;
	
	public DetailsForm(GenotypeWindow main) {
		super(main);
		
		Template t = new Template(tr("details-form"), this);
		
		t.bindString("app.base.url", GenotypeMain.getApp().getEnvironment().getDeploymentPath());
		t.bindString("app.context", GenotypeMain.getApp().getServletContext().getContextPath());
		
		mainTable = new WContainerWidget();
		mainTable.setObjectName("details-container");
		mainTable.setStyleClass("detailsForm");
		t.bindWidget("details-form", mainTable);
	}
	
	WString init(File jobDir, Integer selectedSequenceIndex) {
		try {
			p = GenotypeResultParser.parseFile(jobDir, selectedSequenceIndex, Mode.Classical);
		
			if (p == null) {
				return tr("detailsForm.nonExistingSequenceId").arg(selectedSequenceIndex);
			}
		} catch (NumberFormatException nfe) {
			return tr("detailsForm.nonExistingSequenceId").arg(selectedSequenceIndex);
		}
		
		mainTable.clear();

		mainDetails = getMain().getOrganismDefinition().getMainDetailsForm();
		addDetailsForm(mainDetails, jobDir);

		WListContainerWidget ul = null;
		WContainerWidget details = null;
		WContainerWidget li;

		if (getMain().getOrganismDefinition().haveDetailsNavigationForm()) {
			WContainerWidget title = new WContainerWidget(mainTable);
			title.addWidget(new WText(tr("details.analysisDetails")));
			details = new WContainerWidget(mainTable);
			details.setStyleClass("details");
			ul = new WListContainerWidget(details);
		}

		for (IDetailsForm df : getMain().getOrganismDefinition().getSupportingDetailsforms(p)) {
			if (ul != null) {
				String detailTitle = df.getTitle().getValue();
				WText titleText = new WText("<a href=\"#" + detailTitle.replace(" ", "")
						.toLowerCase() + "\">" + detailTitle + "</a>");
				titleText.setStyleClass("link");
				li = ul.addItem(titleText);
				li.addWidget(new WBreak());
				li.addWidget(new WText(df.getComment()));
				li.addWidget(new WBreak());
			
				if (df.getExtraComment()!=null) {
					WText extraComment = new WText(df.getExtraComment());
					details.addWidget(extraComment);
					extraComment.setStyleClass("details-extraComments");
				}
			}
			
			addDetailsForm(df, jobDir);
		}
		
		return null;
	}
	
	void addDetailsForm(IDetailsForm df, File jobDir){
		WContainerWidget cwTitle = new WContainerWidget(mainTable);
		cwTitle.setObjectName(df.getIdentifier() + "-title");
		String detailTitle = df.getTitle().getValue();
		WText titleText = new WText("<h2><a name=\"" + detailTitle.replace(" ", "").toLowerCase() + "\"></a>"
				+ detailTitle + "</h2>", TextFormat.XHTMLUnsafeText);
		titleText.setId("");

		cwTitle.addWidget(titleText);

		WContainerWidget cwDetails = new WContainerWidget(mainTable);
		cwDetails.setObjectName(df.getIdentifier() + "-details");
		cwDetails.setStyleClass("details");

		cwDetails.addWidget(df);
		df.setId("");

		df.fillForm(p, getMain().getOrganismDefinition(), jobDir);
	}
	
	public String getSequenceName() {
		return GenotypeLib.getEscapedValue(p, "/genotype_result/sequence/@name");
	}

	@Override
	public void handleInternalPath(String internalPath) {

	}
}
