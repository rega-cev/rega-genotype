package rega.genotype.ui.util;

import java.awt.Color;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;

public interface GenomeAttributes {
	public Map<String, Color> getColors();
	public int getGenomeImageStartX();
	public int getGenomeImageEndX();
	public int getGenomeImageStartY();
	public int getGenomeImageEndY();
	public int getGenomeStart();
	public int getGenomeEnd();
	public OrganismDefinition getOrganismDefinition();
	public int getFontSize();
}
