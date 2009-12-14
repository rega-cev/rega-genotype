/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms.details;

import java.io.File;
import java.io.IOException;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.framework.widgets.WListContainerWidget;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

/**
 * A default extension of IDetailsForm for visualizing phylogenetic singal details, used by different virus implementations.
 */
public class DefaultSignalDetailsForm extends IDetailsForm {

	@Override
	public void fillForm(GenotypeResultParser p, OrganismDefinition od, final File jobDir) {
		WListContainerWidget ul = new WListContainerWidget(this);
		WContainerWidget li;
		li = ul.addItem(new WText(tr("defaultSignalAnalysis.signalValue")));
		li.addWidget(new WText(p.getEscapedValue("/genotype_result/sequence/result[@id='pure-puzzle']/signal")));
		li = ul.addItem(new WText(tr("defaultSignalAnalysis.signalComment")));
		final String puzzleFile = p.getValue("/genotype_result/sequence/result[@id='pure-puzzle']/puzzle");

		addWidget(GenotypeLib.getWImageFromResource(new WFileResource("image/png", "") {
			@Override
			public void handleRequest(WebRequest request, WebResponse response) {
				if (getFileName().isEmpty()) {
					File file = GenotypeLib.getSignalPNG(GenotypeLib.getFile(jobDir, puzzleFile));
					setFileName(file.getAbsolutePath());
				}

				super.handleRequest(request, response);
			}				
		}));
	}
	
	@Override
	public WString getComment() {
		return tr("defaultSignalAnalysis.comment");
	}

	@Override
	public WString getTitle() {
		return tr("defaultSignalAnalysis.title");
	}

	@Override
	public WString getExtraComment() {
		return tr("defaultSignalAnalysis.extraComment");
	}
	
	@Override
	public String getIdentifier() {
		return "phylogenic-signal";
	}
}
