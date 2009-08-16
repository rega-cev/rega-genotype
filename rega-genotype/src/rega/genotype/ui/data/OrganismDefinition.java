package rega.genotype.ui.data;

import java.io.File;
import java.io.IOException;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;

public interface OrganismDefinition {
	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException;
	public AbstractJobOverview getJobOverview(File jobDir, GenotypeResourceManager grm);
	public String getResourcesFile();
}
