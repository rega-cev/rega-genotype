package rega.genotype.ui.viruses.nrv;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.Genome;

public class NrvGenome extends Genome {
	private Map<String, Color> colorMap = new HashMap<String, Color>();
	private OrganismDefinition od;
	
	public NrvGenome(OrganismDefinition od) {
		colorMap.put("-", new Color(0xff, 0xff, 0xff));
		
		this.od = od;
	}
	
	public Map<String, Color> COLORS() {
		return colorMap;
	}

	public int GENOMEEND() {
		return 7654;
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
