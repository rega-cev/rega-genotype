package rega.genotype.ui.viruses.hhv8;

import java.awt.Color;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.DefaultGenomeAttributes;

/**
 * HIV genome map drawing implementation.
 * 
 * @author plibin0
 */
public class Hhv8Genome extends DefaultGenomeAttributes {
	
	public Hhv8Genome(OrganismDefinition od) {
		super(od);
		getColors().put("subA", new Color(0xff, 0, 0));
		getColors().put("subA5", new Color(0, 0xaa, 0xff));
		getColors().put("subB", new Color(0xb0, 0x81, 0x55));
		getColors().put("subC", new Color(0xfa, 0xac, 0xd5));
		getColors().put("subD", new Color(0xd0, 0xff, 0x00));
		getColors().put("subE", new Color(0x6b, 0xc7, 0x72));
		getColors().put("HBVsubG", new Color(0xff, 0xd4, 0x00));
		getColors().put("HBVsubH", new Color(0x00, 0xfa, 0xff));
		getColors().put("-", new Color(0, 0xaa, 0xff));
	}
	
	public int getGenomeEnd() {
		return 140000;
	}

	public int getGenomeStart() {
		return 1;
	}

	public int getGenomeImageEndX() {
		return 579;
	}

	public int getGenomeImageStartX() {
		return 31;
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
