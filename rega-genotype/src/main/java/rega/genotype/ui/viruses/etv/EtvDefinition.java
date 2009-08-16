/*
 * Copyright (C) 2008 MyBioData
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.data.AbstractDataTableGenerator;
import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.details.DefaultPhylogeneticDetailsForm;
import rega.genotype.ui.forms.details.DefaultSequenceAssignmentForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.util.Genome;
import rega.genotype.viruses.etv.EnteroTool;
import eu.webtoolkit.jwt.WString;

/**
 * Enterovirus OrganismDefinition implementation.
 */
public class EtvDefinition implements OrganismDefinition {
	private EtvGenome genome = new EtvGenome(this);

	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException {
		EnteroTool etvTool = new EnteroTool(jobDir);
		etvTool.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new EtvJobOverview(main);
	}
	
	public String getOrganismDirectory() {
		return "/rega/genotype/ui/viruses/etv/";
	}

	public AbstractDataTableGenerator getDataTableGenerator(DataTable table) throws IOException {
		return new EtvTableGenerator(table);
	}

	public Genome getGenome() {
		return genome;
	}

	public IDetailsForm getMainDetailsForm() {
		return new DefaultSequenceAssignmentForm(0, "genotype_result.sequence.result['scan'].data");
	}

	private void addPhyloDetailForms(GenotypeResultParser p, List<IDetailsForm> forms) {
		String result = "genotype_result.sequence.result";
		
		String phyloResult = result + "['phylo-serotype']";
		if (p.elementExists(phyloResult)) {
			WString title = new WString("Phylogenetic analyses");
			forms.add(new DefaultPhylogeneticDetailsForm(phyloResult, title, title, true));
		}
	}
	
	public List<IDetailsForm> getSupportingDetailsforms(GenotypeResultParser p) {
		List<IDetailsForm> forms = new ArrayList<IDetailsForm>();
		addPhyloDetailForms(p, forms);
		return forms;
	}
	
	public int getUpdateInterval(){
		return 5000;
	}

	public String getOrganismName() {
		return "Entero";
	}

	public boolean haveDetailsNavigationForm() {
		return false;
	}
}
