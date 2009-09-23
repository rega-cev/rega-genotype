/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.hiv;

import java.awt.Color;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.DefaultGenomeAttributes;

/**
 * HIV genome map drawing implementation.
 * 
 * @author simbre1
 *
 */
public class HivGenome extends DefaultGenomeAttributes {
	
	public HivGenome(OrganismDefinition od) {
		super(od);
		getColors().put("A1", new Color(0xff, 0, 0));
		getColors().put("B", new Color(0, 0xaa, 0xff));
		getColors().put("C", new Color(0xb0, 0x81, 0x55));
		getColors().put("D", new Color(0xfa, 0xac, 0xd5));
		getColors().put("F1", new Color(0xd0, 0xff, 0x00));
		getColors().put("G", new Color(0x6b, 0xc7, 0x72));
		getColors().put("H", new Color(0xff, 0xd4, 0x00));
		getColors().put("J", new Color(0x00, 0xfa, 0xff));
		getColors().put("K", new Color(0xb9, 0x5f, 0xff));
		getColors().put("Group_O", new Color(0, 0, 0));
		getColors().put("-", new Color(0xff, 0xff, 0xff));
	}
	
	public int getGenomeEnd() {
		return 9700;
	}

	public int getGenomeStart() {
		return 1;
	}

	public int getGenomeImageEndX() {
		return 579;
	}

	public int getGenomeImageStartX() {
		return -4;
	}
	
	public int getGenomeImageEndY() {
		return 120;
	}

	public int getGenomeImageStartY() {
		return 5;
	}
}
