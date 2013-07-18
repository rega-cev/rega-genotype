/*
 * Copyright (C) 2008 MyBioData
 * 
 * See the LICENSE file for terms of use.
 */

package rega.genotype.viruses.generic;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.SubSequence;

/**
 * A generic typing tool.
 * 
 * Starts with a blast file "blast.xml"
 * 
 * It will continue with a first phylogenetic analysis when there is one for the given cluster which overlaps
 * with a region described in a reference match.
 * 
 * Finally, it will continue to a sub-clustering analysis if possible.
 * 
 * @author koen
 */
public class GenericTool extends GenotypeTool {
    private static final int MINIMUM_REGION_OVERLAP = 100;

	private AlignmentAnalyses blastXml;
    private BlastAnalysis blastAnalysis;
    private Map<String, PhyloClusterAnalysis> phyloAnalyses = new HashMap<String, PhyloClusterAnalysis>();

    public GenericTool(String xmlSubDir, File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        blastXml = readAnalyses(xmlSubDir + "/blast.xml", workingDir);
        blastAnalysis = (BlastAnalysis) blastXml.getAnalysis("blast");

        for (Cluster c : blastXml.getAllClusters()) {
        	String f = xmlSubDir + "/phylo-" + c.getId() + ".xml";
        	
        	if (new File(GenotypeTool.getXmlBasePath() + f).canRead()) {
        		AlignmentAnalyses analyses = readAnalyses(f, workingDir);
        		phyloAnalyses.put(c.getId(), (PhyloClusterAnalysis) analyses.getAnalysis("phylo-major"));
        	}
        }
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

        	PhyloClusterAnalysis pca = phyloAnalyses.get(c.getId());
        	if (pca != null && blastResult.getReference() != null) {
        		for (BlastAnalysis.Region region : blastResult.getReference().getRegions()) {
        			if (region.overlaps(blastResult.getStart(), blastResult.getEnd(), MINIMUM_REGION_OVERLAP)) {
        				int rs = Math.max(0, region.getBegin() - blastResult.getStart());
        				int re = Math.min(s.getLength(), s.getLength() - (blastResult.getEnd() - region.getEnd()));

        				/*
        				 * Cut the overlapping part to not confuse the alignment
        				 */
        				AbstractSequence s2 = re > rs ? new SubSequence(s.getName(), s.getDescription(), s, rs, re) : s;
        				if (phyloAnalysis(pca, s2, region.getName()))
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

	private boolean phyloAnalysis(PhyloClusterAnalysis pca, AbstractSequence s, String regionName) throws AnalysisException {
		PhyloClusterAnalysis.Result r = pca.run(s);

		if (r.haveSupport()) {
			conclude(r, "Supported with phylogenetic analysis and bootstrap {1} (&gt;= 70)", "serotype");

			subgenogroupPhyloAnalysis(s, pca.getOwner(), regionName, r.getConcludedCluster());
		} else
			conclude("Could not assign", "Not supported by phylogenetic analysis", "serotype");

		return true;
	}

    private boolean subgenogroupPhyloAnalysis(AbstractSequence s, AlignmentAnalyses phylo, String regionName, Cluster cluster) throws AnalysisException {
    	String analysisName = "phylo-minor-" + cluster.getId();
    	
		/*
		 * Only if that analysis is described in the XML file.
		 */
		if (!phylo.haveAnalysis(analysisName))
			return false;

		PhyloClusterAnalysis a = (PhyloClusterAnalysis) phylo.getAnalysis(analysisName);

		PhyloClusterAnalysis.Result r = a.run(s);
		
		String phyloName = "phylogenetic subgenogroup analysis within " + cluster.getId();

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
			conclude("Could not assign", "Not supported by " + phyloName, "subgenogroup");
		else
			conclude(r, "Supported with " + phyloName + " and bootstrap {1} (&gt;= 70)", "subgenogroup");

		return true;
	}

	public void analyzeSelf() throws AnalysisException {
	}
	

	@Override
	protected boolean cancelAnalysis() {
		return false;
	}

	@Override
	protected String currentJob() {
		return null;
	}
}
