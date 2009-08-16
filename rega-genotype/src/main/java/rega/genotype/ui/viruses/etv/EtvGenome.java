/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.Genome;

/**
 * Enterovirus genome map drawing implementation.
 */
public class EtvGenome extends Genome {
	private Map<String, Color> colorMap = new HashMap<String, Color>();
	private OrganismDefinition od;
	
	public EtvGenome(OrganismDefinition od) {
		colorMap.put("-", new Color(0x53, 0xb8, 0x08));
		
		this.od = od;
	}
	
	public Map<String, Color> COLORS() {
		return colorMap;
	}

	public int GENOMEEND() {
		return 7550;
	}

	public int GENOMESTART() {
		return 80;
	}

	public int IMGGENOMEEND() {
		return 605;
	}

	public int IMGGENOMESTART() {
		return 1;
	}
	
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}
}
