package rega.genotype.ui.viruses.hcv;

import rega.genotype.ui.data.OrganismDefinition;

public class LargeHcvGenome extends HcvGenome{

	public LargeHcvGenome(OrganismDefinition od) {
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
