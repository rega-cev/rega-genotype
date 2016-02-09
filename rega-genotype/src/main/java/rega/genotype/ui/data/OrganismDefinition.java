/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.data;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.data.table.AbstractDataTableGenerator;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.Genome;
import rega.genotype.util.DataTable;

/**
 * An interface describing all attributes and functions specific to an organism.
 */
public interface OrganismDefinition {
	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException;
	public AbstractJobOverview getJobOverview(GenotypeWindow main);
	public AbstractDataTableGenerator getDataTableGenerator(AbstractJobOverview jobOverview, DataTable t) throws IOException;
	public String getOrganismDirectory();
	public String getJobDir();
	public Genome getGenome();
	public Genome getLargeGenome();
	public IDetailsForm getMainDetailsForm();
	public List<IDetailsForm> getSupportingDetailsforms(GenotypeResultParser p);
	public int getUpdateInterval();
	public boolean haveDetailsNavigationForm();
	public String getProfileScanType(GenotypeResultParser p);
	public List<String> getRecombinationResultXPaths();
}
