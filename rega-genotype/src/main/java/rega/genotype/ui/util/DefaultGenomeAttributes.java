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
	
	public Color getAssignmentColor(String assignment){
		Color c = colorMap.get(assignment);
		if(c == null){
			c = createAssignmentColor(assignment);
			colorMap.put(assignment, c);
		}
		return c;
	}
	
	protected Color createAssignmentColor(String assignment){
		int hash = assignment.hashCode();
		int red = Math.abs(hash/3 % 256);
		int green = Math.abs(hash/7 % 256);
		int blue = Math.abs(hash/13 % 256);
		return new Color(red,green,blue);
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
