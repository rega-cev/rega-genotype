package rega.genotype.viruses.recombination;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

public class RegionUtils {
	public static class Region {
		public int start;
		public int end;
		public String assignment;
		public double support;
		public String tree;
	};
	
	public static List<Region> getSupportedRegions(Element recombination, int start) {
		List<Region> regions = new ArrayList<Region>();
		
		for (Object o : recombination.getChildren("region")) {
	    	Element region = (Element)o;
			Element result = (Element)region.getChild("result");
			Element best = (Element)result.getChild("best");
		
			double support = Double.valueOf(best.getChildText("support"));

			if (support > 70) { // FIXME hardcoded !!
				Region r = new Region();
				r.start = start + Integer.parseInt(region.getChildTextTrim("start"));
				r.end = start + Integer.parseInt(region.getChildTextTrim("end"));
				r.assignment = best.getChildText("name");
				r.support = support;
				r.tree = result.getChildText("tree");
				regions.add(r);
			}
		}
		
		return regions;
	}
}
