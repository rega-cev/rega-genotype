/*
 * Copyright (C) 2008 MyBioData
 * 
 * See the LICENSE file for terms of use.
 */

package rega.genotype.viruses.generic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.AbstractAnalysis;
import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.AnalysisException;
import rega.genotype.ApplicationException;
import rega.genotype.BlastAnalysis;
import rega.genotype.BlastAnalysis.Region;
import rega.genotype.BlastAnalysis.Result;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.ScanAnalysis;
import rega.genotype.SubSequence;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.singletons.Settings;
import eu.webtoolkit.jwt.WString;

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

    // called from GenotypeTool main!
    public GenericTool(String url, File workDir) throws IOException, ParameterProblemException, FileFormatException {
    	this(Settings.getInstance().getConfig().getToolConfigByUrlPath(url), workDir);
    }
    
    public GenericTool(ToolConfig toolConfig, File workDir) throws IOException, ParameterProblemException, FileFormatException {
    	super(toolConfig, workDir);
  
        blastXml = readAnalyses(getXmlPathAsString() + "blast.xml", getWorkingDir());
        blastAnalysis = (BlastAnalysis) blastXml.getAnalysis("blast");
    }

    /**
     * overwrite this function for specialized cluster analysis.
     * @param c
     * @param s 
     */
    protected void analyseClaster(Cluster c, AbstractSequence s) {
    }

	@Override
	protected void formatDB() throws ApplicationException {
		blastAnalysis.formatDB(blastXml.getAlignment());
	}

	@Override
    public void analyze(AbstractSequence s) throws AnalysisException {
    	/*
    	 * First perform the blast analysis.
    	 */
        BlastAnalysis.Result blastResult = blastAnalysis.run(s);

        if (blastResult.haveSupport()) {
        	Cluster c = blastResult.getConcludedCluster();

        	analyseClaster(c, s);

        	/*
        	 * Reverse complement the sequence for subsequent analyses.
        	 */
        	if (blastResult.isReverseComplement())
        		s = s.reverseCompliment();

        	boolean haveConclusion = false;

        	try {
        		/*
        		 * Perhaps we have a full-genome analysis?
        		 */
        		PhyloClusterAnalysis pca = getPhyloAnalysis(c.getId(), null, null);
        		if (pca != null) {
        			phyloAnalysis(pca, s, blastResult, null);
        			haveConclusion = true;
        		}

        		List<Region> regions = Collections.emptyList();

        		if (blastResult.getReference() != null)
        			/*
        			 * Perhaps we have an analysis for the region covered by the input sequence ?
        			 */
        			regions = findOverlappingRegions(blastResult);

        		if (!regions.isEmpty()) {
        			for (Region region : regions) {
        				pca = getPhyloAnalysis(c.getId(), null, region);
        				if (pca != null) {
        					AbstractSequence s2 = cutRegion(s, blastResult,	region);
        					phyloAnalysis(pca, s2, blastResult, region);
        					haveConclusion = true;
        				}
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
       			if (blastAnalysis.getAbsCutoff() != null && blastAnalysis.getRelativeCutoff() != null)
       				conclude(blastResult, "Assigned based on BLAST absolute score &gt;= " + blastAnalysis.getAbsCutoff() 
       						+ " and relative score &gt;= " + blastAnalysis.getRelativeCutoff(), null);
       			else if (blastAnalysis.getAbsCutoff() != null)
       				conclude(blastResult, "Assigned based on BLAST absolute score &gt;= " + blastAnalysis.getAbsCutoff(), null); 
       			else if (blastAnalysis.getRelativeCutoff() != null)
       				conclude(blastResult, "Assigned based on BLAST relative score &gt;= " + blastAnalysis.getAbsCutoff(), null);
        } else {
        	if (blastAnalysis.getAbsCutoff() != null && blastAnalysis.getRelativeCutoff() != null)
   				conclude("Unassigned", "Unassigned based on BLAST absolute score &gt;= " + blastAnalysis.getAbsCutoff() 
   						+ " and relative score &gt;= " + blastAnalysis.getRelativeCutoff(), null);
   			else if (blastAnalysis.getAbsCutoff() != null)
   				conclude("Unassigned", "Unassigned based on BLAST absolute score &gt;= " + blastAnalysis.getAbsCutoff(), null); 
   			else if (blastAnalysis.getRelativeCutoff() != null)
   				conclude("Unassigned", "Unassigned based on BLAST relative score &gt;= " + blastAnalysis.getAbsCutoff(), null);
        }
    }

	private AbstractSequence cutRegion(AbstractSequence s, BlastAnalysis.Result blastResult, BlastAnalysis.Region region) {
		int rs = Math.max(0, region.getBegin() - blastResult.getStart());
		int re = Math.min(s.getLength(), s.getLength() - (blastResult.getEnd() - region.getEnd()));
		AbstractSequence s2 = re > rs ? new SubSequence(s.getName(), s.getDescription(), s, rs, re) : s;
		return s2;
	}

	private List<Region> findOverlappingRegions(Result blastResult) {
		List<Region> result = new ArrayList<Region>();
		for (BlastAnalysis.Region region : blastResult.getReference().getRegions())
			if (region.overlaps(blastResult.getStart(), blastResult.getEnd(), MINIMUM_REGION_OVERLAP))
				result.add(region);

		return result;
	}

	private PhyloClusterAnalysis getPhyloAnalysis(String genusId, String typeId, Region region) throws IOException, ParameterProblemException, FileFormatException {
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
           	String f = "phylo-" + alignmentId + ".xml";
           	if (new File(getXmlPathAsString() + f).canRead()) {
           		AlignmentAnalyses analyses = readAnalyses(getXmlPathAsString() + f, getWorkingDir());
           		analyses.setRegion(region);

           		if (analyses.haveAnalysis(analysisId)) {
           			result = (PhyloClusterAnalysis) analyses.getAnalysis(analysisId);
           			phyloAnalyses.put(alignmentId + "-" + analysisId, result);
           		}
           	}
		}

		return result;
	}
	
	/**
	 * @param pca          phylogenetic analysis to run
	 * @param s            sequence
	 * @param blastResult  genus matched
	 * @param region       region used for this analysis
	 */
	private void phyloAnalysis(PhyloClusterAnalysis pca, AbstractSequence s, Result blastResult, Region region) throws AnalysisException, IOException, ParameterProblemException, FileFormatException {
		PhyloClusterAnalysis.Result r = pca.run(s);
		ScanAnalysis.Result scanResult = checkBootScan(pca, s);
		
		if (r.haveSupport() && (scanResult == null || scanResult.haveSupport())) {
			conclude(r, new WString("Supported with phylogenetic analysis and bootstrap {1} (&gt;= {2})").arg(r.getConcludedSupport()).arg(pca.getCutoff()), "type", region);

			subgenogroupPhyloAnalysis(s, blastResult, region, r.getConcludedCluster());
		} else
			conclude("Could not assign", "Not supported by phylogenetic analysis", "type", region);
	}

    private ScanAnalysis.Result checkBootScan(PhyloClusterAnalysis pca, AbstractSequence s) throws AnalysisException {
    	ScanAnalysis sa = null;
    	try {
    		sa = (ScanAnalysis) pca.getOwner().getAnalysis(pca.getId() + "-scan");
    	} catch (Exception e) { /* If not available */
    		return null;
    	}
    	
   		return sa.run(s);
	}

	private boolean subgenogroupPhyloAnalysis(AbstractSequence s, Result blastResult, Region region, Cluster typeCluster) throws AnalysisException, IOException, ParameterProblemException, FileFormatException {
    	PhyloClusterAnalysis a = getPhyloAnalysis(blastResult.getConcludedCluster().getId(), typeCluster.getId(), region);
    	
    	if (a == null) {
    		if (region != null) {
    			region = null;
    			a = getPhyloAnalysis(blastResult.getConcludedCluster().getId(), typeCluster.getId(), region);
        		
        		if (a == null)
        			return false;
    		} else {
       			List<Region> regions = Collections.emptyList();

				if (blastResult.getReference() != null)
					/*
					 * Perhaps we have an analysis for the region covered by the input sequence ?
					 */
					regions = findOverlappingRegions(blastResult);

				boolean result = false;
				for (Region r : regions)
					if (subgenogroupPhyloAnalysis(s, blastResult, r, typeCluster))
						result = true;
				
				return result;
    		}
    	}

    	if (region != null)
    		s = cutRegion(s, blastResult, region);
    	
		PhyloClusterAnalysis.Result r = a.run(s);
		ScanAnalysis.Result scanResult = checkBootScan(a, s);
		
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
			|| (scanResult != null && !scanResult.haveSupport())
			|| r.getConcludedCluster() == null
			|| !r.getConcludedCluster().getId().contains(typeCluster.getId())
			|| !r.haveSupport())
			conclude("Could not assign", "Not supported by " + phyloName, "subtype", region);
		else
			conclude(r, new WString("Supported with " + phyloName + " and bootstrap {1} (&gt;= {2})").arg(r.getConcludedSupport()).arg(a.getCutoff()), "subtype", region);

		return true;
	}

	@Override
	public void analyzeSelf(String traceFile, String analysisFile, int windowSize, int stepSize, String analysisId)
			throws AnalysisException {

		try {
			AlignmentAnalyses analyses = readAnalyses(analysisFile, getWorkingDir());

			if (analysisId == null) {
				for (AbstractAnalysis a : analyses.analyses()) {
					if (a instanceof ScanAnalysis && a.getId().endsWith("-self-scan")) {
						ScanAnalysis sa = (ScanAnalysis) a;
						sa.setWindowSize(windowSize);
						sa.setStepSize(stepSize);
						sa.run(null);
					}
				}
			} else {
				ScanAnalysis sa = (ScanAnalysis) analyses.getAnalysis(analysisId);
				sa.run(null);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ParameterProblemException e) {
			throw new RuntimeException(e);
		} catch (FileFormatException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected boolean cancelAnalysis() {
		return new File(workingDir, ".CANCEL").exists();
	}

	@Override
	protected String currentJob() {
		return null;
	}
}

