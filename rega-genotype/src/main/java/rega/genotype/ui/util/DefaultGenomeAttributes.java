package rega.genotype.ui.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;

public abstract class DefaultGenomeAttributes implements GenomeAttributes{
	
	private Map<String, Color> colorMap = new HashMap<String, Color>();
	private OrganismDefinition od;
	
	public DefaultGenomeAttributes(){
		
	}
	public DefaultGenomeAttributes(OrganismDefinition od){
		setOrganismDefinition(od);
	}

	public Map<String, Color> getColors() {
		return colorMap;
	}

	public int getFontSize() {
		return 8;
	}

	public OrganismDefinition getOrganismDefinition() {
		return od;
	}

	protected void setOrganismDefinition(OrganismDefinition od){
		this.od = od;
	}
}
