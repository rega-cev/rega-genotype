package rega.genotype.ui.viruses.hiv;

import rega.genotype.ui.data.OrganismDefinition;

public class LargeHivGenome extends HivGenome{

	public LargeHivGenome(OrganismDefinition od) {
		super(od);
	}

	public int getGenomeImageEndX() {
		return 1166;
	}

	public int getGenomeImageStartX() {
		return 4;
	}
	
	public int getGenomeImageEndY() {
		return 190;
	}

	public int getGenomeImageStartY() {
		return 4;
	}
	
	public int getFontSize() {
		return 18;
	}
}
