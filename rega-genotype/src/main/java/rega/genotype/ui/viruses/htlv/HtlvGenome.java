package rega.genotype.ui.viruses.htlv;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.Genome;

public class HtlvGenome extends Genome {

	private Map<String, Color> colorMap = new HashMap<String, Color>();
	private OrganismDefinition od;
	
	public HtlvGenome(OrganismDefinition od) {
		colorMap.put("subtype_e", new Color(0xff, 0, 0));
		colorMap.put("subtype_f", new Color(0, 0xaa, 0xff));
		colorMap.put("subtype_b", new Color(0xb0, 0x81, 0x55));
		colorMap.put("subtype_d", new Color(0xfa, 0xac, 0xd5));
		colorMap.put("subtype_a", new Color(0xd0, 0xff, 0x00));
		colorMap.put("subtype_a(subgroup_D)", new Color(0x6b, 0xc7, 0x72));
		colorMap.put("subtype_a(subgroup_E)", new Color(0xff, 0xd4, 0x00));
		colorMap.put("subtype_a(subgroup_B)", new Color(0x00, 0xfa, 0xff));
		colorMap.put("subtype_a(subgroup_A)", new Color(0, 0x5a, 0x7f));
		colorMap.put("subtype_c", new Color(0xb9, 0x5f, 0xff));
		colorMap.put("STLV1_(Macaca_tonkeana)", new Color(0, 0, 0));
		colorMap.put("-", new Color(0, 0xaa, 0xff));
		
		this.od = od;
	}
	
	
	public Map<String, Color> COLORS() {
		return colorMap;
	}

	public int GENOMEEND() {
		return 9500;
	}

	public int GENOMESTART() {
		return 1;
	}

	public int IMGGENOMEEND() {
		return 579;
	}

	public int IMGGENOMESTART() {
		return -4;
	}

	public OrganismDefinition getOrganismDefinition() {
		return od;
	}

}
