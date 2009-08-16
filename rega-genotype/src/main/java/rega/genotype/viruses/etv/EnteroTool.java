/*
 * Copyright (C) 2008 MyBioData
 * 
 * See the LICENSE file for terms of use.
 */

package rega.genotype.viruses.etv;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.SubSequence;
import rega.genotype.AlignmentAnalyses.Cluster;

/**
 * NoVTool implements the genotyping algorithm for norovirus.
 * 
 * @author koen
 */
public class EnteroTool extends GenotypeTool {
    private static final int MINIMUM_REGION_OVERLAP = 100;
	private AlignmentAnalyses picorna;
    private BlastAnalysis blastAnalysis;
    private Map<String, PhyloClusterAnalysis> serotypeAnalyses = new HashMap<String, PhyloClusterAnalysis>();

    public EnteroTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        picorna = readAnalyses("ETV/humanpicornagenusblast.xml", workingDir);
        blastAnalysis = (BlastAnalysis) picorna.getAnalysis("blast");

        for (Cluster c : picorna.getAllClusters()) {
        	String f = "ETV/" + c.getId() + "-VP1.xml";
        	
        	if (new File(GenotypeTool.getXmlBasePath() + f).canRead()) {
        		AlignmentAnalyses serotypingAnalyses = readAnalyses(f, workingDir);
        		serotypeAnalyses.put(c.getId(), (PhyloClusterAnalysis) serotypingAnalyses.getAnalysis("phylo-serotype"));
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

        	PhyloClusterAnalysis pca = serotypeAnalyses.get(c.getId());
        	if (pca != null && blastResult.getReference() != null) {
        		for (BlastAnalysis.Region region:blastResult.getReference().getRegions()) {
        			if (region.overlaps(blastResult.getStart(), blastResult.getEnd(), MINIMUM_REGION_OVERLAP)) {
        				int rs = Math.max(0, region.getBegin() - blastResult.getStart());
        				int re = Math.min(s.getLength(), s.getLength() - (blastResult.getEnd() - region.getEnd()));

        				/*
        				 * Cut the overlapping part to not confuse the alignment
        				 */
        				AbstractSequence s2 = re > rs ? new SubSequence(s.getName(), s.getDescription(), s, rs, re) : s;
        				if (phyloAnalysis(pca, s2))
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

	private boolean phyloAnalysis(PhyloClusterAnalysis pca, AbstractSequence s) throws AnalysisException {
		PhyloClusterAnalysis.Result r = pca.run(s);

		if (r.haveSupport())
			conclude(r.concludeForCluster(r.getConcludedCluster()), "Supported with phylogenetic analysis and bootstrap {1} (&gt;= 70)");
		else
			conclude("Could not assign", "Not supported by phylogenetic analysis");

		return true;
	}

    public void analyzeSelf() throws AnalysisException {
	}
}

