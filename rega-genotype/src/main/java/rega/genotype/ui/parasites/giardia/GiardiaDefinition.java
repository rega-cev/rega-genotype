/*
 * Copyright (C) 2008 MyBioData
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.parasites.giardia;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.parasites.giardia.GiardiaTool;
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
import eu.webtoolkit.jwt.WString;

/**
 * Enterovirus OrganismDefinition implementation.
 */
public class GiardiaDefinition implements OrganismDefinition {
	private Genome genome = new Genome(new GiardiaGenome(this));

	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException {
		GiardiaTool giardiaTool = new GiardiaTool(jobDir);
		giardiaTool.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new GiardiaJobOverview(main);
	}
	
	public String getOrganismDirectory() {
		return "/rega/genotype/ui/parasites/giardia/";
	}

	public AbstractDataTableGenerator getDataTableGenerator(AbstractJobOverview jobOverview, DataTable table) throws IOException {
		return new GiardiaTableGenerator(jobOverview, table);
	}

	public Genome getGenome() {
		return genome;
	}

	public IDetailsForm getMainDetailsForm() {
		return new GiardiaSequenceAssignmentForm();
	}

	private void addPhyloDetailForms(GenotypeResultParser p, List<IDetailsForm> forms) {
		String result = "genotype_result.sequence.result";

		for (String region : GiardiaGenome.regions) {
			String phyloResult = result + "['phylo-" + region + "']";
			if (p.elementExists(phyloResult)) {
				WString title = new WString("Phylogenetic analyses for " + region);
				forms.add(new DefaultPhylogeneticDetailsForm(phyloResult, title, title, true));
			}
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
		return "Giardia";
	}

	public boolean haveDetailsNavigationForm() {
		return false;
	}

	//TODO
	public Genome getLargeGenome() {
		return null;
	}
}
