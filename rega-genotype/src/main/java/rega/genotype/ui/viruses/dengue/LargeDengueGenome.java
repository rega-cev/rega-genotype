package rega.genotype.ui.viruses.dengue;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.viruses.dengue.DengueGenome;

public class LargeDengueGenome extends DengueGenome{

	public LargeDengueGenome(OrganismDefinition od) {
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
