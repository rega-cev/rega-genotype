/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms.details;

import java.io.File;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.framework.widgets.WListContainerWidget;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;

/**
 * A default extension of IDetailsForm for visualizing phylogenetic details, used by different virus implementations.
 */
public class DefaultPhylogeneticDetailsForm extends IDetailsForm {
	
	private String xpath;
	private WString commentTitle;
	private WString title;

	public DefaultPhylogeneticDetailsForm(String xpath, WString title, WString commentTitle) {
		this.xpath = xpath;
		this.title = title;
		this.commentTitle = commentTitle;
	}

	public void fillForm(SaxParser p, OrganismDefinition od, File jobDir) {
		String phyloPath = xpath;
		initPhyloSection(p, commentTitle, jobDir, phyloPath);
	}
	
	private void initPhyloSection(SaxParser p, WString header, File jobDir, String phyloPath) {
		addWidget(new WText(header));
		addWidget(new WBreak());

		WListContainerWidget ul = new WListContainerWidget(this);
		WContainerWidget li;
		li = ul.addItem(new WText(lt("Bootstrap support: " + p.getValue(phyloPath + ".best.support"))));
		
		li = ul.addItem(new WText(lt("Download the ")));
		li.addWidget(GenotypeLib.getAnchor("Alignment (NEXUS format)",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".alignment"))));

		li = ul.addItem(new WText(lt("Export or View the Phylogenetic Tree: ")));
		li.addWidget(GenotypeLib.getAnchor("PDF",
				"application/pdf",
				GenotypeLib.getTreePDF(jobDir, GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".tree")))));
		li.addWidget(new WText(lt(", ")));
		li.addWidget(GenotypeLib.getAnchor("NEXUS Format",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".tree"))));
		
		li = ul.addItem(new WText(lt("View the ")));
		li.addWidget(GenotypeLib.getAnchor("PAUP* Log file",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".log"))));
		li.addWidget(new WText(lt(" (Contains bootstrap values)")));
		
	}

	public WString getComment() {
		return tr("defaultPhylogeneticAnalyses.comment");
	}

	public WString getTitle() {
		return title != null ? title : tr("defaultPhylogeneticAnalyses.title");
	}

	@Override
	public WString getExtraComment() {
		return null;
	}
}
