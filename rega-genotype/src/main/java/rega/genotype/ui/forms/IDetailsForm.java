package rega.genotype.ui.forms;

import java.io.File;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;

public abstract class IDetailsForm extends WContainerWidget {
	public abstract void fillForm(SaxParser p, final OrganismDefinition od, File jobDir);
	public abstract WString getTitle();
	public abstract WString getComment();
	public abstract WString getExtraComment();
}
