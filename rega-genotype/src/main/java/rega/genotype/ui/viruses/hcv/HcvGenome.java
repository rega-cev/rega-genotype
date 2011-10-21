package rega.genotype.ui.viruses.hcv;

import java.awt.Color;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.DefaultGenomeAttributes;

/**
 * HIV genome map drawing implementation.
 * 
 * @author plibin0
 */
public class HcvGenome extends DefaultGenomeAttributes {
	
	public HcvGenome(OrganismDefinition od) {
		super(od);
		getColors().put("other", new Color(0x77, 0x77, 0x77));
		getColors().put("1a", new Color( 0xff, 0, 0));
		getColors().put("1b", new Color(0, 0xaa, 0xff));
		getColors().put("1c", new Color(0xb0, 0x81, 0x55));
		getColors().put("2a", new Color(0xfa, 0xac, 0xd5));
		getColors().put("2b", new Color(0xd0, 0xff, 0x00));
		getColors().put("2c", new Color(0x6b, 0xc7, 0x72));
		getColors().put("2k", new Color(0xff, 0xd4, 0x00));
		getColors().put("3a", new Color(0x00, 0xfa, 0xff));
		getColors().put("3b", new Color(0xb9, 0x5f, 0xff));
		getColors().put("3k", new Color(0, 0, 0));
		getColors().put("4a", new Color(0x7f, 0, 0));
		getColors().put("5a", new Color(0, 0x5a, 0x7f));
		getColors().put("6a", new Color(0xfa, 0xac, 0xd5));
		getColors().put("6b", new Color(0xd0, 0xff, 0x00));
		getColors().put("6d", new Color(0x6b, 0xc7, 0x72));
		getColors().put("6g", new Color(0xff, 0xd4, 0x00));
		getColors().put("6h", new Color(0x00, 0xfa, 0xff));
		getColors().put("6k", new Color(0xff, 0xd4, 0x00));
		getColors().put("Geno_1", new Color(0xff, 0xff, 0xff));
		getColors().put("Geno_2", new Color(0xff, 0xff, 0xff));
		getColors().put("Geno_3", new Color(0xff, 0xff, 0xff));
		getColors().put("Geno_4", new Color(0xff, 0xff, 0xff));
		getColors().put("Geno_5", new Color(0xff, 0xff, 0xff));
		getColors().put("Geno_6", new Color(0xff, 0xff, 0xff));
		getColors().put("-", new Color(0xff, 0xff, 0xff));
	}
	
	public int getGenomeEnd() {
		return 9600;
	}

	public int getGenomeStart() {
		return 1;
	}

	public int getGenomeImageEndX() {
		return 580;
	}

	public int getGenomeImageStartX() {
		return 6;
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
