package rega.genotype.ui.forms;

import rega.genotype.ui.data.GenotypeResultParser;
import rega.genotype.ui.data.OrganismDefinition;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.WTable;

public abstract class JobOverviewSummary extends WTable {
	protected final String CHECK_THE_BOOTSCAN = "Check the bootscan";
	protected final String NOT_ASSIGNED = "Not assigned";
	
	public abstract void update(GenotypeResultParser parser, OrganismDefinition od);
	public abstract Side getLocation();
	public abstract void reset();

	protected String formatAssignment(String assignment) {
		if (assignment == null) {
			assignment = NOT_ASSIGNED;
		}
		
		return assignment;
	}
	
	protected String encodeAssignment(String assignment) {
		return assignment;
	}
}
