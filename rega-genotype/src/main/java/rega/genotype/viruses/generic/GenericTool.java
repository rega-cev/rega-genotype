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
import rega.genotype.BlastAnalysis.Region;
import rega.genotype.BlastAnalysis.Result;
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

	private String xmlSubDir;

    public GenericTool(String xmlSubDir, File workingDir) throws IOException, ParameterProblemException, FileFormatException {
    	this.xmlSubDir = xmlSubDir;
        blastXml = readAnalyses(xmlSubDir + "/blast.xml", workingDir);
        blastAnalysis = (BlastAnalysis) blastXml.getAnalysis("blast");
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

       		try {
       			BlastAnalysis.Region region = null;

				if (blastResult.getReference() != null)
					/*
					 * Perhaps we have an analysis for the region covered by the input sequence ?
					 */
					region = findOverlappingRegion(blastResult);

				/*
				 * Perhaps we have a full-genome analysis?
				 */
				PhyloClusterAnalysis pca = getAnalysis(c.getId(), null, null);
				if (pca != null) {
					phyloAnalysis(pca, s, blastResult, region, false);
					haveConclusion = true;
				} else if (region != null) {
		        	pca = getAnalysis(c.getId(), null, region);
		        	if (pca != null) {
		        		AbstractSequence s2 = cutRegion(s, blastResult,	region);
		        		phyloAnalysis(pca, s2, blastResult, region, true);
		        		haveConclusion = true;
			        }
				}
			} catch (IOException e) {
				throw new AnalysisException("", s, e);
			} catch (ParameterProblemException e) {
				throw new AnalysisException("", s, e);
			} catch (FileFormatException e) {
				throw new AnalysisException("", s, e);
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

	private AbstractSequence cutRegion(AbstractSequence s, BlastAnalysis.Result blastResult, BlastAnalysis.Region region) {
		int rs = Math.max(0, region.getBegin() - blastResult.getStart());
		int re = Math.min(s.getLength(), s.getLength() - (blastResult.getEnd() - region.getEnd()));
		AbstractSequence s2 = re > rs ? new SubSequence(s.getName(), s.getDescription(), s, rs, re) : s;
		return s2;
	}

	private Region findOverlappingRegion(Result blastResult) {
		for (BlastAnalysis.Region region : blastResult.getReference().getRegions())
			if (region.overlaps(blastResult.getStart(), blastResult.getEnd(), MINIMUM_REGION_OVERLAP))
				return region;

		return null;
	}

	private PhyloClusterAnalysis getAnalysis(String genusId, String typeId, Region region) throws IOException, ParameterProblemException, FileFormatException {
		String alignmentId = genusId;

		if (region != null)
			alignmentId += "-" + region.getName();	
		
		String analysisId;
   		if (typeId != null)
   			analysisId = "phylo-minor-" + typeId;		
   		else
   			analysisId = "phylo-major";
		
		PhyloClusterAnalysis result = phyloAnalyses.get(alignmentId + "-" + analysisId);

		if (result == null) {
           	String f = xmlSubDir + "/phylo-" + alignmentId + ".xml";
           	
           	if (new File(GenotypeTool.getXmlBasePath() + f).canRead()) {
           		AlignmentAnalyses analyses = readAnalyses(f, new File(workingDir));
           		if (analyses.haveAnalysis(analysisId)) {
           			result = (PhyloClusterAnalysis) analyses.getAnalysis(analysisId);
           			phyloAnalyses.put(alignmentId + "-" + analysisId, result);
           		}
           	}
		}

		return result;
	}

	private void phyloAnalysis(PhyloClusterAnalysis pca, AbstractSequence s, Result blastResult, Region region, boolean cutToRegion) throws AnalysisException, IOException, ParameterProblemException, FileFormatException {
		PhyloClusterAnalysis.Result r = pca.run(s);

		if (r.haveSupport()) {
			conclude(r, "Supported with phylogenetic analysis and bootstrap {1} (&gt;= 70)", "serotype");

			subgenogroupPhyloAnalysis(s, blastResult, region, r.getConcludedCluster(), cutToRegion);
		} else
			conclude("Could not assign", "Not supported by phylogenetic analysis", "serotype");
	}

    private boolean subgenogroupPhyloAnalysis(AbstractSequence s, Result blastResult, Region region, Cluster typeCluster, boolean cutToRegion) throws AnalysisException, IOException, ParameterProblemException, FileFormatException {
    	PhyloClusterAnalysis a = getAnalysis(blastResult.getConcludedCluster().getId(), typeCluster.getId(), region);
    	
    	if (a == null) {
    		if (region != null) {
    			region = null;
    			a = getAnalysis(blastResult.getConcludedCluster().getId(), typeCluster.getId(), region);
    		}
    		
    		if (a == null)
    			return false;
    	}

    	if (region != null && !cutToRegion)
    		s = cutRegion(s, blastResult, region);
    	
		PhyloClusterAnalysis.Result r = a.run(s);
		
		String phyloName = "phylogenetic subgenogroup analysis within " + typeCluster.getId();

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
			|| !r.getConcludedCluster().getId().startsWith(typeCluster.getId())
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

