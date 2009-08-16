/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms.details;

import java.io.File;

import rega.genotype.SequenceAlignment;
import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.framework.widgets.WListContainerWidget;
import rega.genotype.ui.util.AlignmentResource;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WImage;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;

/**
 * A default extension of IDetailsForm for visualizing phylogenetic details, used by different virus implementations.
 */
public class DefaultPhylogeneticDetailsForm extends IDetailsForm {
	
	private String xpath;
	private WString commentTitle;
	private WString title;
	private boolean showTree;

	public DefaultPhylogeneticDetailsForm(String xpath, WString title, WString commentTitle, boolean showTree) {
		this.xpath = xpath;
		this.title = title;
		this.commentTitle = commentTitle;
		this.showTree = showTree;
	}

	public void fillForm(GenotypeResultParser p, OrganismDefinition od, File jobDir) {
		String phyloPath = xpath;
		initPhyloSection(p, commentTitle, jobDir, phyloPath);
	}
	
	private void initPhyloSection(GenotypeResultParser p, WString header, File jobDir, String phyloPath) {
		WWidget w;
		w = new WText(header, this);
		w.setId("");

		WListContainerWidget ul = new WListContainerWidget(this);
		ul.setId("");
		WContainerWidget li;
		li = ul.addItem(new WText("Bootstrap support: " + p.getValue(phyloPath + ".best.support")));
		
		li = ul.addItem(new WText("Download the alignment ("));
		li.addWidget(GenotypeLib.getAnchor("NEXUS format",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".alignment")), null));
		li.addWidget(w = new WText(", "));
		w.setId("");
		li.addWidget(GenotypeLib.getAnchor("FASTA format",
				"application/txt",
				new AlignmentResource(GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".alignment")),
						SequenceAlignment.SEQUENCE_ANY, SequenceAlignment.FILETYPE_FASTA), "alignment.fasta"));
		li.addWidget(w= new WText(")"));
		w.setId("");

		WAnchor anchorTreePdf = GenotypeLib.getAnchor("PDF",
				"application/pdf",
				GenotypeLib.getTreePDF(jobDir, GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".tree"))), null);
		anchorTreePdf.setId("");
		WAnchor anchorTreeNexus = GenotypeLib.getAnchor("NEXUS Format",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".tree")),null);
		anchorTreeNexus.setId("");

		if (!showTree) {
			li = ul.addItem(new WText("Export or View the Phylogenetic Tree: "));
			li.addWidget(anchorTreePdf);
			li.addWidget(w = new WText(", "));
			w.setId("");
			li.addWidget(anchorTreeNexus);
		} else {
			li = ul.addItem(new WText("Phylogenetic Tree (export as "));
			li.addWidget(anchorTreePdf);
			li.addWidget(w = new WText(", "));
			w.setId("");
			li.addWidget(anchorTreeNexus);
			li.addWidget(w = new WText("):"));
			w.setId("");

			WImage treePng = GenotypeLib.getWImageFromFile
				(GenotypeLib.getTreePNG(jobDir, GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".tree"))));
			treePng.setId("");
			treePng.setStyleClass("phyloTree");
			li.addWidget(treePng);
		}

		li = ul.addItem(new WText("View the "));
		li.addWidget(GenotypeLib.getAnchor("PAUP* Log file",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".log")), "paup-log.doc"));
		li.addWidget(w = new WText(" (Contains bootstrap values)"));
		w.setId("");
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

	@Override
	public String getIdentifier() {
		return "phylogeny-detail";
	}
}
