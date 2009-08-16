package rega.genotype.ui.data;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.Genome;

public interface OrganismDefinition {
	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException;
	public AbstractJobOverview getJobOverview(GenotypeWindow main);
	public AbstractCsvGenerator getCsvGenerator(Writer w) throws IOException;
	public String getOrganismDirectory();
	public Genome getGenome();
	public IDetailsForm getMainDetailsForm();
	public List<IDetailsForm> getSupportingDetailsforms(SaxParser p);
	public int getUpdateInterval();
}
