package rega.genotype.ui.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.i18n.resources.GenotypeResourceManager;
import rega.genotype.ui.util.Genome;

public interface OrganismDefinition {
	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException;
	public AbstractJobOverview getJobOverview(File jobDir, GenotypeResourceManager grm);
	public AbstractCsvGenerator getCsvGenerator(PrintStream ps);
	public String getOrganismDirectory();
	public Genome getGenome();
}
