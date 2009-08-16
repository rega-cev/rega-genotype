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
			colorMap.put("-", new Color(0, 0xaa, 0xff));
			
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
