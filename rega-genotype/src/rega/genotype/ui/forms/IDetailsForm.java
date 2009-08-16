package rega.genotype.ui.forms;

import java.io.File;

import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.i8n.WMessage;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.data.SaxParser;

public abstract class IDetailsForm extends WContainerWidget {
	public abstract void fillForm(SaxParser p, final OrganismDefinition od, File jobDir);
	public abstract WMessage getTitle();
	public abstract WMessage getComment();
}
