/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

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
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.forms.details.DefaultPhylogeneticDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.util.Genome;
import rega.genotype.viruses.nov.NoVTool;
import eu.webtoolkit.jwt.WString;

/**
 * NoV OrganismDefinition implementation.
 * 
 * @author simbre1
 *
 */
public class NovDefinition implements OrganismDefinition {
	private NovGenome genome = new NovGenome(this);

	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException {
		NoVTool nrvTool = new NoVTool(jobDir);
		nrvTool.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}

	public AbstractJobOverview getJobOverview(GenotypeWindow main) {
		return new NovJobOverview(main);
	}
	
	public String getOrganismDirectory() {
		return "/rega/genotype/ui/viruses/nov/";
	}

	public AbstractDataTableGenerator getDataTableGenerator(DataTable table) throws IOException {
		return new NovTableGenerator(table);
	}

	public Genome getGenome() {
		return genome;
	}

	public IDetailsForm getMainDetailsForm() {
		return new NovSequenceAssignmentForm();
	}

	private void addPhyloDetailForms(SaxParser p, List<IDetailsForm> forms, String region) {
		String result = "genotype_result.sequence.result";
		
		String phyloResult = result + "['phylo-" + region + "']";
		if (p.elementExists(phyloResult)) {
			WString title = WString.lt("Phylogenetic analyses (" + region + ")");
			forms.add(new DefaultPhylogeneticDetailsForm(phyloResult, title, title));

			String bestGenotype = p.getEscapedValue(phyloResult + ".best.id");
			
			String variantResult = result + "['phylo-" + region + "-" + bestGenotype + "']";
			if (p.elementExists(variantResult)) {
				WString variantTitle = WString.lt("Phylogenetic analyses (" + region + ") for variant within "
						+ bestGenotype);
				forms.add(new DefaultPhylogeneticDetailsForm(variantResult, variantTitle, variantTitle));
			}
		}
	}
	
	public List<IDetailsForm> getSupportingDetailsforms(SaxParser p) {
		List<IDetailsForm> forms = new ArrayList<IDetailsForm>();

		addPhyloDetailForms(p, forms, "ORF1");
		addPhyloDetailForms(p, forms, "ORF2");
		
		return forms;
	}
	
	public int getUpdateInterval(){
		return 5000;
	}

	public String getOrganismName() {
		return "NoV";
	}
}
