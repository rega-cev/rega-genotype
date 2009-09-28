/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.parasites.giardia;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.Genome;

/**
 * Enterovirus genome map drawing implementation.
 */
public class GiardiaGenome extends Genome {
	private Map<String, Color> colorMap = new HashMap<String, Color>();
	private OrganismDefinition od;
	
	public static String regions[] = { "16S", "B-giardin", "GDH", "TPI" };

	public GiardiaGenome(OrganismDefinition od) {
		colorMap.put("-", new Color(0x53, 0xb8, 0x08));
		colorMap.put("A", new Color(0xff, 0x33, 0x00));
		colorMap.put("AIII", new Color(0xff, 0x66, 0x00));
		colorMap.put("B", new Color(0x00, 0x33, 0xcc));
		colorMap.put("C", new Color(0x00, 0x99, 0x00));
		colorMap.put("D", new Color(0x00, 0xff, 0x00));
		colorMap.put("E", new Color(0xff, 0xff, 0x00));
		colorMap.put("F", new Color(0xff, 0x00, 0xff));
		colorMap.put("G", new Color(0xcc, 0x66, 0x00));
		
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
