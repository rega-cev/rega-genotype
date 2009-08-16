package rega.genotype.util;

import java.awt.Color;
import java.util.Map;

public abstract class Genome {
	public abstract Map<String, Color> COLORS();
	public abstract int IMGGENOMESTART();
	public abstract int IMGGENOMEEND();
	public abstract int GENOMESTART();
	public abstract int GENOMEEND();
	public abstract String GENOMEIMAGE();
	
	public int imgX(int pos) {
		return (int)(IMGGENOMESTART() + ((double)pos - GENOMESTART())
		         /((double)GENOMEEND() - GENOMESTART())
		         *((double)IMGGENOMEEND() - IMGGENOMESTART()));
	}
}
