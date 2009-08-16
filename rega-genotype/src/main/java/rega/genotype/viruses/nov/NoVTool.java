/*
 * Copyright (C) 2008 MyBioData
 * 
 * See the LICENSE file for terms of use.
 */

package rega.genotype.viruses.nov;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.ScanAnalysis;
import rega.genotype.SubSequence;
import rega.genotype.AlignmentAnalyses.Cluster;

/**
 * NoVTool implements the genotyping algorithm for norovirus.
 * 
 * @author koen
 */
public class NoVTool extends GenotypeTool {
	enum GroupRegion {
		GroupI_ORF1,
		GroupI_ORF2,
		GroupII_ORF1,
		GroupII_ORF2
	}

    private AlignmentAnalyses nov;
	private AlignmentAnalyses phyloAnalyses[] = new AlignmentAnalyses[4];
    private BlastAnalysis blastAnalysis;
    
    public NoVTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        nov = readAnalyses("NoV/novblastaa.xml", workingDir);
        blastAnalysis = (BlastAnalysis) nov.getAnalysis("blast");

        phyloAnalyses[GroupRegion.GroupI_ORF1.ordinal()] = readAnalyses("NoV/nov-ORF1.xml", workingDir);
        phyloAnalyses[GroupRegion.GroupII_ORF1.ordinal()] = phyloAnalyses[GroupRegion.GroupI_ORF1.ordinal()];
        phyloAnalyses[GroupRegion.GroupI_ORF2.ordinal()] = readAnalyses("NoV/novI-ORF2.xml", workingDir);
        phyloAnalyses[GroupRegion.GroupII_ORF2.ordinal()] = readAnalyses("NoV/novII-ORF2.xml", workingDir);        
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
    	/*
    	 * First perform the blast analysis.
    	 */
        BlastAnalysis.Result blastResult = blastAnalysis.run(s);

        if (blastResult.haveSupport()) {
        	Cluster c = blastResult.getConcludedCluster();

        	/*
        	 * Reverse complement the sequence for subsequent analyses.
        	 */
        	if (blastResult.isReverseCompliment())
        		s = s.reverseCompliment();

        	boolean haveConclusion = false;
        	
        	/*
        	 * See if any of the phylogenetic analyses are available given
        	 * the sequence region, and if they can give some conclusion
        	 */
    		if (blastAnalysis.getRegions() != null) {
        		for (BlastAnalysis.Region region:blastAnalysis.getRegions()) {
        			if (region.overlaps(blastResult.getStart(), blastResult.getEnd(), 100)) {
        				int rs = Math.max(0, region.getBegin() - blastResult.getStart());
        				int re = Math.min(s.getLength(), s.getLength() - (blastResult.getEnd() - region.getEnd()));

        				/*
        				 * Cut the overlapping part to not confuse the alignment
        				 */
        				AbstractSequence s2 = re > rs ? new SubSequence(s.getName(), s.getDescription(), s, rs, re) : s;
        				if (phyloAnalysis(s2, c.getId(), region.getName()))
            				haveConclusion = true;
        			}        			
        		}
        	}
    		
			/*
			 * If no conclusion: conclude the blast result
			 */
    		if (!haveConclusion)
    			conclude(blastResult, "Assigned based on BLAST score &gt;= " + blastAnalysis.getCutoff());
        } else {
            conclude("Unassigned", "Unassigned because of BLAST score &lt; " + blastAnalysis.getCutoff());
        }
    }

    /**
     * Does the phylogenetic analyses for given genogroup and region.
     * 
     * @return wheter a conclusion could be made based on a phylogenetic analysis.
     */
	private boolean phyloAnalysis(AbstractSequence s, String groupId, String regionName) throws AnalysisException {
		System.err.println("Phylo analysis: GenoGroup = " + groupId + ", region = " + regionName);

		/*
		 * Get the right phylogenetic analyses
		 */
		AlignmentAnalyses phylo = null;
		if (groupId.equals("I")) {
			if (regionName.equals("ORF1"))
				phylo = phyloAnalyses[GroupRegion.GroupI_ORF1.ordinal()];
			else if (regionName.equals("ORF2"))
				phylo = phyloAnalyses[GroupRegion.GroupI_ORF2.ordinal()];			
		} else if (groupId.equals("II")) {
			if (regionName.equals("ORF1"))
				phylo = phyloAnalyses[GroupRegion.GroupII_ORF1.ordinal()];
			else if (regionName.equals("ORF2"))
				phylo = phyloAnalyses[GroupRegion.GroupII_ORF2.ordinal()];			
		}
		
		if (phylo != null) {
			/*
			 * Get the phylogenetic analysis for identifying genotype
			 */
			PhyloClusterAnalysis a = (PhyloClusterAnalysis) phylo.getAnalysis("phylo-" + regionName);
			
			PhyloClusterAnalysis.Result r = a.run(s);

			String phyloName = "phylogenetic analysis (" + regionName + ")";

			if (r.haveSupport()) {
				Cluster major = null, variant = null;

				major = r.getConcludedCluster().getParent();
				if (major == null || major.hasTag("no-subclusters"))
					major = r.getConcludedCluster();
				else
					variant = r.getConcludedCluster();

				conclude(r.concludeForCluster(major), "Supported with " + phyloName + " and bootstrap {1} (&gt;= 70)", regionName);

				boolean haveSubClusters = major.getClusters().size() > 0 && !major.hasTag("no-subclusters");
				
				if (haveSubClusters && variant != null)
					conclude(r.concludeForCluster(variant), "Supported with " + phyloName + " and bootstrap {1} (&gt;= 70)", regionName + "-variant");
				else {
					boolean haveVariantAnalysis = variantPhyloAnalysis(s, phylo, regionName, r.getConcludedCluster());
				
					if (!haveVariantAnalysis && haveSubClusters)
						conclude("Could not assign", "Not supported by " + phyloName, regionName + "-variant");
				}
			} else
				conclude("Could not assign", "Not supported by " + phyloName, regionName);

			return true;
		} else
			return false;
	}

    /**
     * Does the variant analysis for a particular genotype.
     * 
     * @return whether a variant could be detected.
     */
	private boolean variantPhyloAnalysis(AbstractSequence s, AlignmentAnalyses phylo, String regionName, Cluster cluster) throws AnalysisException {
		String analysisName = "phylo-" + regionName + "-" + cluster.getId();

		/*
		 * Only if that analysis is described in the XML file.
		 */
		if (!phylo.haveAnalysis(analysisName))
			return false;

		PhyloClusterAnalysis a = (PhyloClusterAnalysis) phylo.getAnalysis(analysisName);

		PhyloClusterAnalysis.Result r = a.run(s);
		
		String phyloName = "phylogenetic sub clustering analysis (" + regionName + ")";

		/*
		 * If we are clustering with the outgroup, then clearly we could not identify a variant.
		 * 
		 * WARNING: the following test is based on a variant cluster to start with the same
		 * name as the genotype for which it is a variant!
		 * This is to differentiate with the outgroup. It would be better to mark the
		 * outgroup with some attribute ?
		 */
		if (r == null
			|| r.getConcludedCluster() == null
			|| !r.getConcludedCluster().getId().startsWith(cluster.getId())
			|| !r.haveSupport())
			conclude("Could not assign", "Not supported by " + phyloName, regionName + "-variant");
		else
			conclude(r, "Supported with " + phyloName + " and bootstrap {1} (&gt;= 70)", regionName + "-variant");

		return true;
	}

	public void analyzeSelf() throws AnalysisException {
		for (int i = 0; i < 4; ++i) {
			if (phyloAnalyses[i] != null) {
				ScanAnalysis scan = (ScanAnalysis) phyloAnalyses[i].getAnalysis("scan-self");
				scan.run(null);
			}
		}
	}
}

