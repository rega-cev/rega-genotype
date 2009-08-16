/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.viruses.hiv;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.Genome;

public class HivGenome extends Genome {
	private Map<String, Color> colorMap = new HashMap<String, Color>();
	private OrganismDefinition od;
	
	public HivGenome(OrganismDefinition od) {
		colorMap.put("A1", new Color(0xff, 0, 0));
		colorMap.put("B", new Color(0, 0xaa, 0xff));
		colorMap.put("C", new Color(0xb0, 0x81, 0x55));
		colorMap.put("D", new Color(0xfa, 0xac, 0xd5));
		colorMap.put("F1", new Color(0xd0, 0xff, 0x00));
		colorMap.put("G", new Color(0x6b, 0xc7, 0x72));
		colorMap.put("H", new Color(0xff, 0xd4, 0x00));
		colorMap.put("J", new Color(0x00, 0xfa, 0xff));
		colorMap.put("K", new Color(0xb9, 0x5f, 0xff));
		colorMap.put("Group_O", new Color(0, 0, 0));
		colorMap.put("-", new Color(0xff, 0xff, 0xff));
		
		this.od = od;
	}
	
	public Map<String, Color> COLORS() {
		return colorMap;
	}

	public int GENOMEEND() {
		return 9700;
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
	
	public static void main(String [] args) {
//		HivGenome hiv = new HivGenome();
//
//		try {
//			hiv.getGenomePNG(new File("/home/plibin0/projects/subtypetool/genomePng"), 0, "F1", 2252, 3275, 0, "pure");
//			hiv.getSmallGenomePNG(new File("/home/plibin0/projects/subtypetool/genomePng"), 0, "F1", 2252, 3275, 0, "pure");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
}
