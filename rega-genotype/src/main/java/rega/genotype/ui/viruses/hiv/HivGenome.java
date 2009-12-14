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
		Map<String, Color> colors = getColors();

		colors.put("A1", new Color(0xff, 0, 0));
		colors.put("B", new Color(0, 0xaa, 0xff));
		colors.put("C", new Color(0xb0, 0x81, 0x55));
		colors.put("D", new Color(0xfa, 0xac, 0xd5));
		colors.put("F1", new Color(0xd0, 0xff, 0x00));
		colors.put("G", new Color(0x6b, 0xc7, 0x72));
		colors.put("H", new Color(0xff, 0xd4, 0x00));
		colors.put("J", new Color(0x00, 0xfa, 0xff));
		colors.put("K", new Color(0xb9, 0x5f, 0xff));
		colors.put("CRF", new Color(0x47, 0x5c, 0x7b));
		colors.put("Group_O", new Color(0, 0, 0));
		colors.put("-", new Color(0xff, 0xff, 0xff));

		Map<String, Color> likeColors = new HashMap<String, Color>();
		for (Map.Entry<String, Color> entry : colors.entrySet()) {
			Color lighter = lighter(entry.getValue());
			likeColors.put(entry.getKey() + "-like", lighter);
		}
		
		colors.putAll(likeColors);		
	}
	
	private Color lighter(Color value) {
		int r = Math.min(255, (int)(value.getRed() + 50));
		int g = Math.min(255, (int)(value.getGreen() + 50));
		int b = Math.min(255, (int)(value.getBlue() + 50));
		
		return new Color(r, g, b);
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
