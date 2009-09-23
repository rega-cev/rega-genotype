package rega.genotype.ui.viruses.htlv;

import java.awt.Color;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.DefaultGenomeAttributes;

public class HtlvGenome extends DefaultGenomeAttributes {
	
	public HtlvGenome(OrganismDefinition od) {
		super(od);
		getColors().put("subtype_e", new Color(0xff, 0, 0));
		getColors().put("subtype_f", new Color(0, 0xaa, 0xff));
		getColors().put("subtype_b", new Color(0xb0, 0x81, 0x55));
		getColors().put("subtype_d", new Color(0xfa, 0xac, 0xd5));
		getColors().put("subtype_a", new Color(0xd0, 0xff, 0x00));
		getColors().put("subtype_a(subgroup_D)", new Color(0x6b, 0xc7, 0x72));
		getColors().put("subtype_a(subgroup_E)", new Color(0xff, 0xd4, 0x00));
		getColors().put("subtype_a(subgroup_B)", new Color(0x00, 0xfa, 0xff));
		getColors().put("subtype_a(subgroup_A)", new Color(0, 0x5a, 0x7f));
		getColors().put("subtype_c", new Color(0xb9, 0x5f, 0xff));
		getColors().put("STLV1_(Macaca_tonkeana)", new Color(0, 0, 0));
		getColors().put("-", new Color(0, 0xaa, 0xff));
	}
	
	public int getGenomeEnd() {
		return 9500;
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
		return 125;
	}

	public int getGenomeImageStartY() {
		return 30;
	}
}
