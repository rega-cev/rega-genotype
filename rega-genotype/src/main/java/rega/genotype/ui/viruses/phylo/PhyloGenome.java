package rega.genotype.ui.viruses.phylo;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.Genome;

public class PhyloGenome extends Genome {

		private Map<String, Color> colorMap = new HashMap<String, Color>();
		private OrganismDefinition od;
		
		public PhyloGenome(OrganismDefinition od) {
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
	@Override
	public int GENOMEEND() {
			return 9700;
	}

	@Override
	public int GENOMESTART() {
			return 1;
	}

	@Override
	public int IMGGENOMEEND() {
		return 579;
	}

	@Override
	public int IMGGENOMESTART() {
		return -4;
	}

	@Override
	public OrganismDefinition getOrganismDefinition() {
		return od;
	}

}
