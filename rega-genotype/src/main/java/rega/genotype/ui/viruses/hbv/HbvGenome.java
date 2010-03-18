package rega.genotype.ui.viruses.hbv;

import java.awt.Color;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.DefaultGenomeAttributes;

/**
 * HIV genome map drawing implementation.
 * 
 * @author plibin0
 */
public class HbvGenome extends DefaultGenomeAttributes {
	
	public HbvGenome(OrganismDefinition od) {
		super(od);
		getColors().put("HBVsubA" ,new Color( 0xff, 0, 0));
		getColors().put("HBVsubB" ,new Color(0, 0xaa, 0xff));
		getColors().put("HBVsubC" ,new Color(0xb0, 0x81, 0x55));
		getColors().put("HBVsubD" ,new Color(0xfa, 0xac, 0xd5));
		getColors().put("HBVsubE" ,new Color(0xd0, 0xff, 0x00));
		getColors().put("HBVsubF" ,new Color(0x6b, 0xc7, 0x72));
		getColors().put("HBVsubG" ,new Color(0xff, 0xd4, 0x00));
		getColors().put("HBVsubH" ,new Color(0x00, 0xfa, 0xff));
		getColors().put("<70%" ,new Color(0xff, 0xff, 0xff));
		getColors().put("NA" ,new Color(0xe6, 0xe6, 0xe6));
	}
	
	public int getGenomeEnd() {
		return 9600;
	}

	public int getGenomeStart() {
		return 1;
	}

	public int getGenomeImageEndX() {
		return 576;
	}

	public int getGenomeImageStartX() {
		return 4;
	}

	public int getGenomeImageEndY() {
		// TODO Auto-generated method stub
		return 2;
	}

	public int getGenomeImageStartY() {
		// TODO Auto-generated method stub
		return 120;
	}
}
