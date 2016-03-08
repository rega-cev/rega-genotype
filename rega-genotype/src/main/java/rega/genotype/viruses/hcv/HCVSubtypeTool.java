/*
 * Created on Feb 6, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.hcv;

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
import rega.genotype.ScanAnalysis;
import rega.genotype.SubSequence;

public class HCVSubtypeTool extends GenotypeTool {
    private AlignmentAnalyses hcv;
    private PhyloClusterAnalysis pureAnalysis;
    private ScanAnalysis scanAnalysis;
    private PhyloClusterAnalysis purePuzzleAnalysis;
    
    private Map<String, PhyloClusterAnalysis> provisionalAnalyses = new HashMap<String, PhyloClusterAnalysis>();
	private File workingDir;    
    public HCVSubtypeTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
    	this(null, workingDir);
    }
    public HCVSubtypeTool(String toolId, File workingDir) throws IOException, ParameterProblemException, FileFormatException {
    	super(toolId == null ? HCVTool.HCV_TOOL_ID : toolId, workingDir);
    	this.workingDir = workingDir;
    	
		String xmlPath = getXmlPathAsString() + File.separator + "hcv.xml";
    	hcv = readAnalyses(xmlPath, workingDir);
        pureAnalysis = (PhyloClusterAnalysis) hcv.getAnalysis("pure");
        purePuzzleAnalysis = (PhyloClusterAnalysis) hcv.getAnalysis("pure-puzzle");
        scanAnalysis = (ScanAnalysis) hcv.getAnalysis("scan-pure");
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
    	analyze(s, null);
    }

    public void analyze(AbstractSequence s, BlastAnalysis.Result blastResult) throws AnalysisException {
    	if (s.getLength() > 800) {
            PhyloClusterAnalysis.Result pureResult = pureAnalysis.run(s);
            ScanAnalysis.Result scanResult = scanAnalysis.run(s);

            if (scanResult.haveSupport()) {
                if (pureResult.haveSupport()) {
                        // Rule 1
                        conclude(pureResult,
                            "Subtype assigned based on sequence > 800 bps " +
                            "clustering with a pure subtype with bootstrap > 70% " +
                            "without recombination in the bootscan.");  
                 } else {
                	// Rule 3
                    conclude("Check the Report",
                       "Subtype unassigned based on sequence > 800 bps " +
                       "failure to classify as pure subtype (Bootstrap Support)" +
                       "without recombination in the bootscan.");
                }
            } else {                
            	// Rule 2 & 4
                conclude ("Check the Bootscan",
                        "Subtype unassigned based on sequence > 800 bps " +
                        "with recombination in the bootscan.");

            } 
        } else {
    		PhyloClusterAnalysis sPureAnalysis = purePuzzleAnalysis;
 
    		if (blastResult != null && blastResult.getReference() != null) {
        		for (BlastAnalysis.Region region:blastResult.getReference().getRegions()) {
        			int a = region.getBegin();
        			int b = region.getEnd();
        			int c = blastResult.getStart();
        			int d = blastResult.getEnd();
        			
        			int nonOverlap = Math.max(0, a - c) - Math.max(0, a - d)
        				+ Math.max(0, d - b) - Math.max(0, c - b);
        			
        			if (nonOverlap < 200) {
        				if (findAnalyses(region.getName())) {
        					sPureAnalysis = provisionalAnalyses.get(region.getName());

        					int rs = Math.max(0, region.getBegin() - blastResult.getStart());
        					int re = Math.min(s.getLength(), s.getLength() - (blastResult.getEnd() - region.getEnd()));

        					// Cut the overlapping part to not confuse the alignment
        					if (re > rs)
        						s = new SubSequence(s.getName(), s.getDescription(), s, rs, re);
        				}
        			}    			
        		}
    		}

        	PhyloClusterAnalysis.Result pure = sPureAnalysis.run(s);
            
            if (!pure.haveSupport()) {
                // Rule 7
                conclude("Check the report",
                        "Subtype unassigned based on sequence &lt; 800bp, " +
                        "and not clustering with a pure subtype with bootstrap >70 %.");
            } else {
            	if (sPureAnalysis == purePuzzleAnalysis) {
                    if ((pure.getSupportInner() - pure.getSupportOuter()) > -50) {
                        // Rule 5
                        conclude(pure,
                                "Subtype assigned based on sequence &lt; 800bp, " +
                                "clustering with a pure subtype with bootstrap > 70%, " +
                                "and clustering inside the pure subtype cluster.");
                    } else {
                        // Rule 6
                        conclude("Check the report",
                                "Subtype assigned based on sequence &lt; 800bp, " +
                                "clustering with a pure subtype with bootstrap > 70%, " +
                                "however not clustering inside the pure subtype cluster.");
                    }
            	} else {
            		// Rule 5
                    conclude(pure,
                            "Subtype assigned based on sequence &lt; 800bp, " +
                            "clustering with a pure subtype with bootstrap > 70%.");
            	}
            }
        }
    }
    
    private boolean findAnalyses(String name) {
    	if (provisionalAnalyses.get(name) == null) {
    		String file = getXmlPathAsString() + File.separator +  name + ".xml";
    		try {
 				AlignmentAnalyses analyses = readAnalyses(file, workingDir);

        		PhyloClusterAnalysis pure = (PhyloClusterAnalysis) analyses.getAnalysis("pure-puzzle");
    			if (pure == null)
    				return false;
    		
    			provisionalAnalyses.put(name, pure);
			
    			return true;
    		} catch (IOException e) {
    			return false;
    		} catch (ParameterProblemException e) {
    			System.err.println("Error loading analyses from: " + file);
    			e.printStackTrace();
    			return false;
			} catch (FileFormatException e) {
    			System.err.println("Error loading analyses from: " + file);
    			e.printStackTrace();
    			return false;
			}
    	} else
    		return true;
	}

	public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) hcv.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
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

