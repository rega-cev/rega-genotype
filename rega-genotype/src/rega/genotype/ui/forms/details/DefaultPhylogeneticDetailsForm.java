package rega.genotype.ui.forms.details;

import java.io.File;

import net.sf.witty.wt.WAnchor;
import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WFileResource;
import net.sf.witty.wt.WText;
import net.sf.witty.wt.i8n.WMessage;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.forms.IDetailsForm;
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
		addWidget(new WText(lt("<li>Export or View the Phylogenetic Tree: ")));
//		addWidget(getAnchor("PDF",
//				"application/pdf",
//				GenotypeLib.getTreePDF(jobDir, getFile(jobDir, p.getValue(phyloPath+".tree")))));
		addWidget(new WText(lt(", ")));
		addWidget(GenotypeLib.getAnchor("NEXUS Format",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".tree"))));
		addWidget(new WText(lt("</li>")));
		
		addWidget(new WText(lt("<li>View the ")));
		addWidget(GenotypeLib.getAnchor("PAUP* Log file",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".log"))));
		addWidget(new WText(lt(" (Contains bootstrap values for all HIV subtypes)</li>")));
		
		addWidget(new WText(lt("<li>Download the ")));
		addWidget(GenotypeLib.getAnchor("Alignment (NEXUS format)",
				"application/txt",
				GenotypeLib.getFile(jobDir, p.getValue(phyloPath+".alignment"))));
		addWidget(new WText(lt("</li>")));
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
