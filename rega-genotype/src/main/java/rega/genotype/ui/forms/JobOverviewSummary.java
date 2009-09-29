package rega.genotype.ui.forms;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import eu.webtoolkit.jwt.WContainerWidget;

public abstract class JobOverviewSummary extends WContainerWidget {
	protected final String CHECK_THE_BOOTSCAN = "Check the bootscan";
	protected final String NOT_ASSIGNED = "Not assigned";
	
	public abstract void update(GenotypeResultParser parser, OrganismDefinition od);
	public abstract void reset();
	
	protected String formatAssignment(String assignment) {
		if (assignment == null) {
			assignment = NOT_ASSIGNED;
		}
		
		return assignment;
	}
}
