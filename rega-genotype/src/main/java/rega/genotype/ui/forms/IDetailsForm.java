/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;

/**
 * An interface describing all attributes and functions for a details module in the DetailsForm
 */
public abstract class IDetailsForm extends WContainerWidget {
	public abstract void fillForm(SaxParser p, final OrganismDefinition od, File jobDir);
	public abstract WString getTitle();
	public abstract WString getComment();
	public abstract WString getExtraComment();
}
