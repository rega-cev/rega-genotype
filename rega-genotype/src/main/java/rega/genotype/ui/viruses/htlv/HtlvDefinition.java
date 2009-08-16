package rega.genotype.ui.viruses.htlv;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.data.DefaultTableGenerator;
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
import rega.genotype.viruses.htlv.HTLVTool;
import eu.webtoolkit.jwt.WString;

public class HtlvDefinition implements OrganismDefinition {

	private HtlvGenome genome = new HtlvGenome(this);

	public AbstractDataTableGenerator getDataTableGenerator(DataTable t) throws IOException {
		return new DefaultTableGenerator(t);	
	}

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
		return "/rega/genotype/ui/viruses/htlv/";
	}

	public String getOrganismName() {
		return "HTLV";
	}

	public List<IDetailsForm> getSupportingDetailsforms(SaxParser p) {
		List<IDetailsForm> forms = new ArrayList<IDetailsForm>();

		WString m = WString.lt("Phylogenetic analysis with pure subtypes:");
		
		if (p.elementExists("genotype_result.sequence.result['pure']"))
			forms.add(new DefaultPhylogeneticDetailsForm("genotype_result.sequence.result['pure']", m, m));
		else if (p.elementExists("genotype_result.sequence.result['pure-puzzle']"))
			forms.add(new DefaultPhylogeneticDetailsForm("genotype_result.sequence.result['pure-puzzle']", m, m));

		m = WString.lt("Phylogenetic analysis with pure subtypes and CRFs:");

		if (p.elementExists("genotype_result.sequence.result['crf']"))
			forms.add(new DefaultPhylogeneticDetailsForm("genotype_result.sequence.result['crf']", m, m));
		
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
		// TODO Auto-generated method stub
		return 5000;
	}

	public void startAnalysis(File jobDir) throws IOException,
			ParameterProblemException, FileFormatException {
		HTLVTool htlv = new HTLVTool(jobDir);
		htlv.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");

	}

}
