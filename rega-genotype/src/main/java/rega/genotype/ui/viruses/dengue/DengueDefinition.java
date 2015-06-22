package rega.genotype.ui.viruses.dengue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.AbstractDataTableGenerator;
import rega.genotype.data.table.DefaultTableGenerator;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.DefaultJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.details.DefaultPhylogeneticDetailsForm;
import rega.genotype.ui.forms.details.DefaultRecombinationDetailsForm;
import rega.genotype.ui.forms.details.DefaultSequenceAssignmentForm;
import rega.genotype.ui.forms.details.DefaultSignalDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.Genome;
import rega.genotype.ui.viruses.dengue.DengueGenome;
import rega.genotype.util.DataTable;
import rega.genotype.viruses.dengue.DENGUETool;
import eu.webtoolkit.jwt.WString;

public class DengueDefinition implements OrganismDefinition {

	private Genome genome = new Genome(new DengueGenome(this));

	public AbstractDataTableGenerator getDataTableGenerator(AbstractJobOverview jobOverview, DataTable t) throws IOException {
		return new DefaultTableGenerator(jobOverview.getFilter(), t);
	}

	public Genome getGenome() {
		return genome;
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new DefaultJobOverview(main);
	}

	public IDetailsForm getMainDetailsForm() {
		return new DefaultSequenceAssignmentForm(1);
	}

	public String getProfileScanType(GenotypeResultParser p) {
		return "pure";
	}

	public String getOrganismDirectory() {
		return "/rega/genotype/ui/viruses/dengue/";
	}

	public String getOrganismName() {
		return "DENGUE";
	}

	public List<IDetailsForm> getSupportingDetailsforms(GenotypeResultParser p) {
		List<IDetailsForm> forms = new ArrayList<IDetailsForm>();

		WString m = new WString("Phylogenetic analysis with pure subtypes:");
		
		if (p.elementExists("/genotype_result/sequence/result[@id='pure']"))
			forms.add(new DefaultPhylogeneticDetailsForm("/genotype_result/sequence/result[@id='pure']", m, m, false));
		else if (p.elementExists("/genotype_result/sequence/result[@id='pure-puzzle']"))
			forms.add(new DefaultPhylogeneticDetailsForm("/genotype_result/sequence/result[@id='pure-puzzle']", m, m, false));

		m = new WString("Phylogenetic analysis with pure subtypes and CRFs:");

		if (p.elementExists("/genotype_result/sequence/result[@id='crf']"))
			forms.add(new DefaultPhylogeneticDetailsForm("/genotype_result/sequence/result[@id='crf']", m, m, false));
		
		String scan = "/genotype_result/sequence/result[@id='scan-pure']";
		if (p.elementExists(scan))
			forms.add(new DefaultRecombinationDetailsForm(scan, "pure", new WString("DENGUE Subtype Recombination Analysis")));
		
		String crfScan = "/genotype_result/sequence/result[@id='scan-crf']";
		if (p.elementExists(crfScan))
			forms.add(new DefaultRecombinationDetailsForm(crfScan, "crf", new WString("DENGUE CRF/Subtype Recombination Analysis")));

		if(p.elementExists("/genotype_result/sequence/result[@id='pure-puzzle']")) {
			forms.add(new DefaultSignalDetailsForm());
		}
		
		return forms;
	}
	public int getUpdateInterval() {
		// TODO Auto-generated method stub
		return 5000;
	}

	
	public void startAnalysis(File jobDir) throws IOException,
			ParameterProblemException, FileFormatException {
		DENGUETool dengue = new DENGUETool(jobDir);
		dengue.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");

	}
	/*
	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException {
		HIVTool hiv = new HIVTool(jobDir);
		hiv.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}*/

	public boolean haveDetailsNavigationForm() {
		return true;
	}

	public Genome getLargeGenome() {
		return getGenome();
	}

	public List<String> getRecombinationResultXPaths() {
		// TODO Auto-generated method stub
		return null;
	}
}
