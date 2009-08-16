package rega.genotype.ui.forms;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import eu.webtoolkit.jwt.WContainerWidget;

public interface JobOverviewSummary {
	public WContainerWidget getWidget();
	public void update(GenotypeResultParser parser, OrganismDefinition od);
	public void reset();
}
