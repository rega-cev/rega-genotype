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

import eu.webtoolkit.jwt.WString;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.AbstractDataTableGenerator;
import rega.genotype.data.table.SequenceFilter;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.details.DefaultPhylogeneticDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.Genome;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.util.DataTable;
import rega.genotype.utils.Settings;
import rega.genotype.viruses.etv.EnteroTool;

/**
 * Enterovirus OrganismDefinition implementation.
 */
public class EtvDefinition implements OrganismDefinition {
	private Genome genome = new Genome(new EtvGenome(this));

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

	public AbstractDataTableGenerator getDataTableGenerator(SequenceFilter sequenceFilter, DataTable table) throws IOException {
		return new EtvTableGenerator(sequenceFilter, table);
	}

	public Genome getGenome() {
		return genome;
	}

	public IDetailsForm getMainDetailsForm() {
		return new EtvSequenceAssignmentForm();
	}

	public String getProfileScanType(GenotypeResultParser p) {
		return null;
	}

	private void addPhyloDetailForms(GenotypeResultParser p, List<IDetailsForm> forms) {
		String result = "/genotype_result/sequence/result";
		
		String phyloResult = result + "[@id='phylo-serotype']";
		if (p.elementExists(phyloResult)) {
			WString title = new WString("Phylogenetic analyses");
			forms.add(new DefaultPhylogeneticDetailsForm(phyloResult, title, title, true));

			String bestGenotype = GenotypeLib.getEscapedValue(p, phyloResult + "/best/id");
			
			String variantResult = result + "[@id='phylo-VP1-" + bestGenotype + "']";
			if (p.elementExists(variantResult)) {
				WString variantTitle = new WString("Phylogenetic analyses for subgenogroup within "
						+ bestGenotype);
				forms.add(new DefaultPhylogeneticDetailsForm(variantResult, variantTitle, variantTitle, true));
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

	public boolean haveDetailsNavigationForm() {
		return false;
	}

	public Genome getLargeGenome() {
		return getGenome();
	}

	public List<String> getRecombinationResultXPaths() {
		return null;
	}

	public String getJobDir() {
		return Settings.getInstance().getJobDir(EtvMain.ETV_URL).getAbsolutePath();
	}
}
