/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.nov;

import java.awt.Color;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.DefaultGenomeAttributes;

/**
 * NoV genome map drawing implementation.
 */
public class NovGenome extends DefaultGenomeAttributes {
	
	public NovGenome(OrganismDefinition od) {
		super(od);
		getColors().put("-", new Color(0x53, 0xb8, 0x08));
	}

	public int getGenomeEnd() {
		return 7582;
	}

	public int getGenomeStart() {
		return 1;
	}

	public int getGenomeImageEndX() {
		return 584;
	}

	public int getGenomeImageStartX() {
		return 0;
	}
	
	public int getGenomeImageEndY() {
		return 100;
	}

	public int getGenomeImageStartY() {
		return 13;
	}
}
