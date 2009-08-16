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
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.IDetailsForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.DataTable;
import rega.genotype.ui.util.Genome;

/**
 * An interface describing all attributes and functions specific to an organism.
 */
public interface OrganismDefinition {
	public void startAnalysis(File jobDir) throws IOException, ParameterProblemException, FileFormatException;
	public AbstractJobOverview getJobOverview(GenotypeWindow main);
	public AbstractDataTableGenerator getDataTableGenerator(DataTable t) throws IOException;
	public String getOrganismDirectory();
	public Genome getGenome();
	public IDetailsForm getMainDetailsForm();
	public List<IDetailsForm> getSupportingDetailsforms(SaxParser p);
	public int getUpdateInterval();
	public String getOrganismName();
}
