/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.hcv;

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
import rega.genotype.util.DataTable;
import rega.genotype.viruses.hcv.HCVTool;
import eu.webtoolkit.jwt.WString;

/**
 * HCV OrganismDefinition implementation.
 * 
 * @author simbre1
 *
 */
public class HcvDefinition implements OrganismDefinition {
	private Genome genome = new Genome(new HcvGenome(this));
	private Genome largeGenome = new Genome(new LargeHcvGenome(this));

	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException {
		HCVTool hcv = new HCVTool(jobDir);
		hcv.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new DefaultJobOverview(main);
	}
	
	public String getOrganismDirectory() {
		return "/rega/genotype/ui/viruses/hcv/";
	}

	public AbstractDataTableGenerator getDataTableGenerator(AbstractJobOverview jobOverview, DataTable t) throws IOException {
		return new DefaultTableGenerator(jobOverview.getFilter(), t);
	}

	public Genome getGenome() {
		return genome;
	}

	public IDetailsForm getMainDetailsForm() {
		return new DefaultSequenceAssignmentForm(2);
	}
	
	public String getProfileScanType(GenotypeResultParser p) {
		String rule = p.getValue("/genotype_result/sequence/conclusion/rule");
		if (rule != null && (rule.equals("3a") || rule.equals("5c")))
			return "crf";
		else
			return "pure";
	}
	
	private String SCAN_PURE = "/genotype_result/sequence/result[@id='scan-pure']";
	private String SCAN_CRF = "/genotype_result/sequence/result[@id='scan-crf']";
	
	public List<String> getRecombinationResultXPaths() {
		List<String> paths = new ArrayList<String>();
		paths.add(SCAN_PURE);
		paths.add(SCAN_CRF);
		return paths;
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
		
		if (p.elementExists(SCAN_PURE))
			forms.add(new DefaultRecombinationDetailsForm(SCAN_PURE, "pure", new WString("HCV Subtype Recombination Analysis")));
		
		if (p.elementExists(SCAN_CRF))
			forms.add(new DefaultRecombinationDetailsForm(SCAN_CRF, "crf", new WString("HCV CRF/Subtype Recombination Analysis")));

		if(p.elementExists("/genotype_result/sequence/result[@id='pure-puzzle']")) {
			forms.add(new DefaultSignalDetailsForm());
		}
		
		return forms;
	}
	
	public int getUpdateInterval(){
		return 5000;
	}

	public String getOrganismName() {
		return "HCV";
	}

	public boolean haveDetailsNavigationForm() {
		return true;
	}

	public Genome getLargeGenome() {
		return largeGenome;
	}
}
