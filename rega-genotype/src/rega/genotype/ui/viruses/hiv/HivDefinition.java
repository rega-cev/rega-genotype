package rega.genotype.ui.viruses.hiv;

import java.io.File;
import java.io.IOException;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.DefaultJobOverview;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;
import rega.genotype.viruses.hiv.HIVTool;

public class HivDefinition implements OrganismDefinition {
	private HivGenome genome = new HivGenome();

	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException {
		HIVTool hiv = new HIVTool(jobDir);
		hiv.analyze(jobDir.getAbsolutePath() + File.separatorChar + "sequences.fasta",
				jobDir.getAbsolutePath() + File.separatorChar + "result.xml");
	}

	public AbstractJobOverview getJobOverview(File jobDir, GenotypeResourceManager grm) {
		return new DefaultJobOverview(jobDir, grm, genome);
	}
	
	public String getResourcesFile() {
		return "/rega/genotype/ui/viruses/hiv/resources.xml";
	}
}
