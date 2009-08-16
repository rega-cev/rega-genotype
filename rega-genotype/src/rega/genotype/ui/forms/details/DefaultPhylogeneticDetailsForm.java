package rega.genotype.ui.forms.details;

import java.io.File;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.i8n.WMessage;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.framework.widgets.WListContainerWidget;
import rega.genotype.ui.util.GenotypeLib;

public class DefaultPhylogeneticDetailsForm extends IDetailsForm {
	
	public void fillForm(SaxParser p, OrganismDefinition od, File jobDir) {
		String phyloPath;
		if(p.elementExists("genotype_result.sequence.result[pure]")) {
			phyloPath = "genotype_result.sequence.result[pure]";
		} else {
			phyloPath = "genotype_result.sequence.result[pure-puzzle]";
		}
		initPhyloSection(p, lt("Phylogenetic analysis with pure subtypes:"), jobDir, phyloPath);
		
		if(p.elementExists("genotype_result.sequence.result[crf]")) {
			initPhyloSection(p, lt("Phylogenetic analysis with pure subtypes and CRFs:"), jobDir, "genotype_result.sequence.result[crf]");
		}
	}
	
	private void initPhyloSection(SaxParser p, WMessage header, File jobDir, String phyloPath) {
		addWidget(new WText(header));
		addWidget(new WBreak());

		WListContainerWidget ul = new WListContainerWidget(this);
		WContainerWidget li;
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
		li.addWidget(new WText(lt(" (Contains bootstrap values for all HIV subtypes)")));
		
		li = ul.addItem(new WText(lt("Download the ")));
		li.addWidget(GenotypeLib.getAnchor("Alignment (NEXUS format)",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".alignment"))));
	}

	public WMessage getComment() {
		return tr("defaultPhylogeneticAnalyses.comment");
	}

	public WMessage getTitle() {
		return tr("defaultPhylogeneticAnalyses.title");
	}

	@Override
	public WMessage getExtraComment() {
		return null;
	}
}
