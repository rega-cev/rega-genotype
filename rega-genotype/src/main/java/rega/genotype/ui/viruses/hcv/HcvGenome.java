package rega.genotype.ui.viruses.hcv;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.Genome;

/**
 * HIV genome map drawing implementation.
 * 
 * @author plibin0
 */
public class HcvGenome extends Genome {
	private Map<String, Color> colorMap = new HashMap<String, Color>();
	private OrganismDefinition od;
	
	public HcvGenome(OrganismDefinition od) {
		colorMap.put("1a" ,new Color( 0xff, 0, 0));
		colorMap.put("1b" ,new Color(0, 0xaa, 0xff));
		colorMap.put("1c" ,new Color(0xb0, 0x81, 0x55));
		colorMap.put("2a" ,new Color(0xfa, 0xac, 0xd5));
		colorMap.put("2b" ,new Color(0xd0, 0xff, 0x00));
		colorMap.put("2c" ,new Color(0x6b, 0xc7, 0x72));
		colorMap.put("2k" ,new Color(0xff, 0xd4, 0x00));
		colorMap.put("3a" ,new Color(0x00, 0xfa, 0xff));
		colorMap.put("3b" ,new Color(0xb9, 0x5f, 0xff));
		colorMap.put("3k" ,new Color(0, 0, 0));
		colorMap.put("4a" ,new Color(0x7f, 0, 0));
		colorMap.put("5a" ,new Color(0, 0x5a, 0x7f));
		colorMap.put("Geno_6" ,new Color(0x60, 0x41, 0x25));
		colorMap.put("-" ,new Color(0xff, 0xff, 0xff));
		
		this.od = od;
	}
	
	@Override
	public Map<String, Color> COLORS() {
		return colorMap;
	}

	@Override
	public int GENOMEEND() {
		return 9600;
	}

	@Override
	public int GENOMESTART() {
		return 1;
	}

	@Override
	public int IMGGENOMEEND() {
		return 580;
	}

	@Override
	public int IMGGENOMESTART() {
		return 6;
	}

	@Override
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}
}
