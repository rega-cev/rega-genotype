/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.etv;

import java.awt.Color;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.DefaultGenomeAttributes;

/**
 * Enterovirus genome map drawing implementation.
 */
public class EtvGenome extends DefaultGenomeAttributes {

	public EtvGenome(OrganismDefinition od) {
		super(od);
		getColors().put("-", new Color(0x53, 0xb8, 0x08));
	}
	
	public int getGenomeEnd() {
		return 7550;
	}

	public int getGenomeStart() {
		return 80;
	}

	public int getGenomeImageEndX() {
		return 605;
	}

	public int getGenomeImageStartX() {
		return 1;
	}
	
	public int getGenomeImageEndY() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getGenomeImageStartY() {
		// TODO Auto-generated method stub
		return 0;
	}
}
