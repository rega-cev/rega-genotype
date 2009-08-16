package rega.genotype.ui.viruses.phylo;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.data.AbstractDataTableGenerator;
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
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.util.Genome;
import rega.genotype.viruses.phylo.PhyloSubtypeTool;
import eu.webtoolkit.jwt.WString;

public class PhyloDefinition implements OrganismDefinition {

	private PhyloGenome genome = new PhyloGenome(this);

	public Genome getGenome() {
			return genome;
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new DefaultJobOverview(main);
	}

	public IDetailsForm getMainDetailsForm() {
		return new DefaultSequenceAssignmentForm(1, "genotype_result.sequence.result['scan'].data");
	}

	public String getOrganismDirectory() {
		return "/rega/genotype/ui/viruses/phylo/";
	}

	public String getOrganismName() {
		return "PHYLO";
	}

	public List<IDetailsForm> getSupportingDetailsforms(SaxParser p) {
		List<IDetailsForm> forms = new ArrayList<IDetailsForm>();

		WString m = WString.lt("Phylogenetic analysis with pure subtypes:");
		
		if (p.elementExists("genotype_result.sequence.result['pure']"))
			forms.add(new DefaultPhylogeneticDetailsForm("genotype_result.sequence.result['pure']", m, m, false));
		else if (p.elementExists("genotype_result.sequence.result['pure-puzzle']"))
			forms.add(new DefaultPhylogeneticDetailsForm("genotype_result.sequence.result['pure-puzzle']", m, m, false));

		m = WString.lt("Phylogenetic analysis with pure subtypes and CRFs:");

		if (p.elementExists("genotype_result.sequence.result['crf']"))
			forms.add(new DefaultPhylogeneticDetailsForm("genotype_result.sequence.result['crf']", m, m, false));
		
		if (p.elementExists("genotype_result.sequence.result['scan']"))
			forms.add(new DefaultRecombinationDetailsForm());
		
		if (p.elementExists("genotype_result.sequence.result['crfscan']"))
			forms.add(new DefaultRecombinationDetailsForm());

		if(p.elementExists("genotype_result.sequence.result['pure-puzzle']")) {
			forms.add(new DefaultSignalDetailsForm());
		}
		
		return forms;
	}
	public int getUpdateInterval() {
		return 5000;
	}


	
	public void startAnalysis(File jobDir) throws IOException,
	ParameterProblemException, FileFormatException {
		PhyloSubtypeTool phylo = new PhyloSubtypeTool(jobDir);
		phylo.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
		jobDir.getAbsolutePath() + File.separatorChar + "result.xml");


	}

	public AbstractDataTableGenerator getDataTableGenerator(DataTable t)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean haveDetailsNavigationForm() {
		// TODO Auto-generated method stub
		return false;
	}

}
