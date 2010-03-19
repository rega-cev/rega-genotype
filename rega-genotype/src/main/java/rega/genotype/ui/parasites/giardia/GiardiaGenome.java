/*
 * Copyright (C) 2008 MyBioData, Rotselaar
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.parasites.giardia;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.DefaultGenomeAttributes;

/**
 * Enterovirus genome map drawing implementation.
 */
public class GiardiaGenome extends DefaultGenomeAttributes {
	private OrganismDefinition od;
	
	public static String regions[] = { "16S", "B-giardin", "GDH", "TPI" };
	public static int imgGenomeStart[] = { 1, 1602, 2550, 4049 }; 
	public static int imgGenomeEnd[] = { 1454, 2421, 3900, 4823 }; 
	
	public GiardiaGenome(OrganismDefinition od) {
		super();
		
		getColors().put("-", new Color(0xff, 0xff, 0xff));
		getColors().put("A", new Color(0xff, 0x33, 0x00));
		getColors().put("AIII", new Color(0xff, 0x66, 0x00));
		getColors().put("B", new Color(0x00, 0x33, 0xcc));
		getColors().put("C", new Color(0x00, 0x99, 0x00));
		getColors().put("D", new Color(0x00, 0xff, 0x00));
		getColors().put("E", new Color(0xff, 0xff, 0x00));
		getColors().put("F", new Color(0xff, 0x00, 0xff));
		getColors().put("G", new Color(0xcc, 0x66, 0x00));
		
		this.od = od;
	}

	public int getGenomeEnd() {
		return 4823;
	}

	public int getGenomeStart() {
		return 1;
	}

	public int getGenomeImageEndX() {
		return 580;
	}

	public int getGenomeImageStartX() {
		return 20;
	}

	public int getGenomeImageEndY() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getGenomeImageStartY() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}

	public static int mapToImageGenome(int blastPos, String region) {
		int regionIndex = Arrays.asList(regions).indexOf(region);
		
		int result = imgGenomeStart[regionIndex] + blastPos;
		return Math.max(imgGenomeStart[regionIndex], Math.min(imgGenomeEnd[regionIndex], result));
	}
}
