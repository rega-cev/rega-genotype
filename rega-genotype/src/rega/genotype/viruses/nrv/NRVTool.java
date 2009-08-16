/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.nrv;

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
import rega.genotype.AlignmentAnalyses.Region;
import rega.genotype.AlignmentAnalyses.Taxus;

public class NRVTool extends GenotypeTool {
	enum GroupRegion {
		GroupI_ORF1,
		GroupI_ORF2,
		GroupII_ORF1,
		GroupII_ORF2
	}

    private AlignmentAnalyses nrv;
	private AlignmentAnalyses phyloAnalyses[] = new AlignmentAnalyses[4];
    private BlastAnalysis blastAnalysis;
    
    public NRVTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        nrv = readAnalyses("NRV/nrvblast.xml", workingDir);
        blastAnalysis = (BlastAnalysis) nrv.getAnalysis("blast");

        phyloAnalyses[GroupRegion.GroupI_ORF2.ordinal()] = readAnalyses("NRV/nrvI-ORF2.xml", workingDir);
        phyloAnalyses[GroupRegion.GroupII_ORF2.ordinal()] = readAnalyses("NRV/nrvII-ORF2.xml", workingDir);
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        BlastAnalysis.Result blastResult = blastAnalysis.run(s);
        
        if (blastResult.haveSupport()) {
        	Cluster c = blastResult.getConcludedCluster();
        	Taxus t = c.getTaxa().get(0);

    		boolean phyloAssignment = false;

    		if (t.getRegions() != null) {
        		for (Region region:t.getRegions()) {
        			if (region.overlaps(blastResult.getStart(), blastResult.getEnd())) {
        				if (region.getName().equals("ORF2")) {
        					int rs = Math.max(1, region.getBegin() - blastResult.getStart());
        					int re = Math.min(s.getLength(), s.getLength() - (blastResult.getEnd() - region.getEnd()));
        					
        					AbstractSequence s2 = rs < re ? new SubSequence(s.getName(), s.getDescription(), s, rs, re) : s;
        					if (phyloAnalysis(s2, c.getId(), region.getName()))
        						phyloAssignment = true;
        				}
        			}
        		}
        	}
    		
    		if (!phyloAssignment)
    			conclude(blastResult, "Assigned based on BLAST score &gt;= " + blastAnalysis.getCutoff());
        } else {
            conclude("Unassigned", "Unassigned because of BLAST score &lt " + blastAnalysis.getCutoff());
        }
    }

	private boolean phyloAnalysis(AbstractSequence s, String groupId, String regionName) throws AnalysisException {
		System.err.println("Phylo analysis: GenoGroup = " + groupId + ", region = " + regionName);

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
			PhyloClusterAnalysis a = (PhyloClusterAnalysis) phylo.getAnalysis("phylo");
			
			PhyloClusterAnalysis.Result r = a.run(s);

			if (r.haveSupport())
				conclude(r, "Supported with phylogenetic analysis and bootstrap &gt;= 70");
			else {
				if (r.getSupportInner() >= 100)
					conclude(r, "Supported with phylogenetic analysis with bootstrap &lt; 70 but inner clustering support &gt;= 100");
				else
					conclude("Could not assign", "Not supported by phylogenetic analysis");
			}

			return true;
		} else
			return false;
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

