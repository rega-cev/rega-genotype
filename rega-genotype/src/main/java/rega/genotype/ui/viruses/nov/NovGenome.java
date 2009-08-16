/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.Genome;

/**
 * NoV genome map drawing implementation.
 * 
 * @author simbre1
 *
 */
public class NovGenome extends Genome {
	private Map<String, Color> colorMap = new HashMap<String, Color>();
	private OrganismDefinition od;
	
	public NovGenome(OrganismDefinition od) {
		colorMap.put("-", new Color(0x53, 0xb8, 0x08));
		
		this.od = od;
	}
	
	public Map<String, Color> COLORS() {
		return colorMap;
	}

	public int GENOMEEND() {
		return 7582;
	}

	public int GENOMESTART() {
		return 1;
	}

	public int IMGGENOMEEND() {
		return 584;
	}

	public int IMGGENOMESTART() {
		return 0;
	}
	
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}
}
