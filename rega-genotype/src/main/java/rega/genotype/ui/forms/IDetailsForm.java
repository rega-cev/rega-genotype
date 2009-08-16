/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.GenotypeResultParser;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;

/**
 * An interface describing all attributes and functions for a details module in the DetailsForm
 */
public abstract class IDetailsForm extends WContainerWidget {
	/**
	 * Load the form contents based the results read from the results parser.
	 * 
	 * @param p The results parser
	 * @param od The organism definition
	 * @param jobDir The directory of the job
	 */
	public abstract void fillForm(GenotypeResultParser p, final OrganismDefinition od, File jobDir);

	/**
	 * Return the details title.
	 */
	public abstract WString getTitle();

	/**
	 * Return a comment.
	 */
	public abstract WString getComment();

	/**
	 * Return an extra comment, or null if there is no such thing.
	 */
	public abstract WString getExtraComment();

	/**
	 * Return an ID that can be used as widget objectName()
	 */
	public abstract String getIdentifier();
}
