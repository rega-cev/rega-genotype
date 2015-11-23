/*
 * Copyright (C) 2008 MyBioData
 * 
 * See the LICENSE file for terms of use.
 */

package rega.genotype.parasites.giardia;

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

public class GiardiaTool extends GenotypeTool {
	private AlignmentAnalyses giardiaBlast;
    private BlastAnalysis blastAnalysis;
    private Map<String, PhyloClusterAnalysis> phyloAnalyses = new HashMap<String, PhyloClusterAnalysis>();

    public GiardiaTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        giardiaBlast = readAnalyses("GIARDIA/blast.xml", workingDir);
        blastAnalysis = (BlastAnalysis) giardiaBlast.getAnalysis("blast");

        for (Cluster c : giardiaBlast.getAllClusters()) {
        	String f = "GIARDIA/" + c.getId() + ".xml";
        	
        	if (new File(GenotypeTool.getXmlBasePath() + f).canRead()) {
        		AlignmentAnalyses serotypingAnalyses = readAnalyses(f, workingDir);
        		phyloAnalyses.put(c.getId(), (PhyloClusterAnalysis) serotypingAnalyses.getAnalysis("phylo-" + c.getId()));
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

        	PhyloClusterAnalysis pca = phyloAnalyses.get(c.getId());
			if (!phyloAnalysis(pca, s))
				conclude(blastResult, "Assigned based on BLAST score &gt;= " + blastAnalysis.getCutoff() + "." +
						" Further typing is not supported for the 16S region."); 
        } else {
            conclude("Unassigned", "Unassigned because of BLAST score &lt; " + blastAnalysis.getCutoff());
        }
    }

	private boolean phyloAnalysis(PhyloClusterAnalysis pca, AbstractSequence s) throws AnalysisException {
		
		if (pca == null)
			return false;
		
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

