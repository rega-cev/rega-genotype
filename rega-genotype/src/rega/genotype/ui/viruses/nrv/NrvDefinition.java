package rega.genotype.ui.viruses.nrv;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.witty.wt.i8n.WMessage;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.data.AbstractCsvGenerator;
import rega.genotype.ui.data.DefaultCsvGenerator;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.DefaultJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.details.DefaultPhylogeneticDetailsForm;
import rega.genotype.ui.forms.details.DefaultRecombinationDetailsForm;
import rega.genotype.ui.forms.details.DefaultSequenceAssignmentForm;
import rega.genotype.ui.forms.details.DefaultSignalDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.Genome;
import rega.genotype.viruses.nrv.NRVTool;

public class NrvDefinition implements OrganismDefinition {
	private NrvGenome genome = new NrvGenome(this);

	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException {
		NRVTool nrvTool = new NRVTool(jobDir);
		nrvTool.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new NrvJobOverview(main);
	}
	
	public String getOrganismDirectory() {
		return "/rega/genotype/ui/viruses/nrv/";
	}

	public AbstractCsvGenerator getCsvGenerator(PrintStream ps) {
		return new DefaultCsvGenerator(ps);
	}

	public Genome getGenome() {
		return genome;
	}

	public IDetailsForm getMainDetailsForm() {
		return new DefaultSequenceAssignmentForm(1, null);
	}

	private void addPhyloDetailForms(SaxParser p, List<IDetailsForm> forms, String region) {
		String result = "genotype_result.sequence.result";
		
		String phyloResult = result + "['phylo-" + region + "']";
		if (p.elementExists(phyloResult)) {
			forms.add(new DefaultPhylogeneticDetailsForm(phyloResult,
					WMessage.lt("Phylogenetic analyses for " + region + " genotype: ")));

			String bestGenotype = p.getValue(phyloResult + ".best.id");
			
			String variantResult = result + "['phylo-" + region + "-" + bestGenotype + "']";
			if (p.elementExists(variantResult)) {
				forms.add(new DefaultPhylogeneticDetailsForm(variantResult,
					WMessage.lt("Phylogenetic analyses for variant:")));
			}
		}
	}
	
	public List<IDetailsForm> getSupportingDetailsforms(SaxParser p) {
		List<IDetailsForm> forms = new ArrayList<IDetailsForm>();

		addPhyloDetailForms(p, forms, "ORF1");
		addPhyloDetailForms(p, forms, "ORF2");
		
		return forms;
	}
}
