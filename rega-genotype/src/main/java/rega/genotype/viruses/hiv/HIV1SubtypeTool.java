/*
 * Created on Feb 6, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.hiv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.ResultTracer;
import rega.genotype.ScanAnalysis;
import rega.genotype.AbstractAnalysis.Concludable;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.PhyloClusterAnalysis.Result;

public class HIV1SubtypeTool extends GenotypeTool {
    private final class ClusterLike implements Concludable {
		private final Result pureResult;
		private String id;
		private String name;

		private ClusterLike(Result result, String id, String name) {
			this.pureResult = result;
			this.id = id;
			this.name = name;
		}

		public Cluster getConcludedCluster() {
			return pureResult.getConcludedCluster();
		}

		public float getConcludedSupport() {
			return pureResult.getConcludedSupport();
		}

		public void writeConclusion(ResultTracer tracer) {
			tracer.printlnOpen("<assigned>");
		    tracer.add("id", id);
		    tracer.add("name", name);
		    tracer.add("support", getConcludedSupport());
		    if (getConcludedCluster().getDescription() != null) {
		       tracer.add("description", getConcludedCluster().getDescription());
		    }
		    tracer.printlnClose("</assigned>");
		}
	}

	private AlignmentAnalyses hiv1;
    private PhyloClusterAnalysis pureAnalysis;
    private ScanAnalysis scanAnalysis;
    private PhyloClusterAnalysis crfAnalysis;
    private PhyloClusterAnalysis purePuzzleAnalysis;

    private ScanAnalysis crfScanAnalysis;
    private PhyloClusterAnalysis crfScanPhyloAnalysis;

    public HIV1SubtypeTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        hiv1 = readAnalyses("HIV/hiv1.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) hiv1.getAnalysis("pure");
        purePuzzleAnalysis = (PhyloClusterAnalysis) hiv1.getAnalysis("pure-puzzle");
        scanAnalysis = (ScanAnalysis) hiv1.getAnalysis("scan-pure");
        crfScanAnalysis = (ScanAnalysis) hiv1.getAnalysis("scan-crf");
        crfScanPhyloAnalysis = (PhyloClusterAnalysis) crfScanAnalysis.getAnalysis();
        crfAnalysis = (PhyloClusterAnalysis) hiv1.getAnalysis("crf");
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        if (s.getLength() > 800) {
            final PhyloClusterAnalysis.Result pureResult = pureAnalysis.run(s);
            ScanAnalysis.Result scanResult = scanAnalysis.run(s);
            PhyloClusterAnalysis.Result crfResult = crfAnalysis.run(s);
            ScanAnalysis.Result scanCRFResult = null;
            PhyloClusterAnalysis.Result crfPhyloResult = null;
            double scanCRFSupport = 0;

            if (crfResult.haveSupport() && crfResult.getBestCluster().hasTag("CRF")) {
                crfScanPhyloAnalysis.getClusters().add(crfResult.getBestCluster());

                scanCRFResult = crfScanAnalysis.run(s);
                scanCRFSupport = scanCRFResult.getSupportedTypes().get(crfResult.getBestCluster().getId());
                
                pureAnalysis.getClusters().add(crfResult.getBestCluster());
                crfPhyloResult = pureAnalysis.run(s);

                crfScanPhyloAnalysis.getClusters().remove(crfResult.getBestCluster());
                pureAnalysis.getClusters().remove(crfResult.getBestCluster());
            }

            if (scanResult.haveSupport()) {
            	// No recombination in bootscan
                rule1(pureResult, crfResult);
            } else {                
                if (!crfResult.haveSupport() || !crfResult.getBestCluster().hasTag("CRF")) {
                	// those that span >0.1 of windows
                	Map<String,Integer> supported = scanResult.getSupportedTypes();
                	
                	if (supported.size() >= 2) {
                		//Rule 2a: recombinant of pure types
                		concludeRule("2a", "Recombinant of " + toRecombinantString(scanResult.getWindowCount(), supported), "Rule 2a");
                	} else if (scanResult.getBootscanSupport() >= 0.7) {
                		//Rule 2b=5 like rule 1
                		rule2b(pureResult, crfResult);
                	} else if (scanResult.getBootscanSupport() >= 0.5) {
                		//Rule 2c
                		Concludable concluded = new ClusterLike(pureResult, pureResult.getConcludedCluster().getId() + "-like",
                				pureResult.getConcludedCluster().getName() + ", potential recombinant");
                		concludeRule("2c", concluded, "Rule 2c");
                	} else {
                		//Rule 2d
                		concludeRule("2d", "Recombinant", "Rule 2d");
                	}
                } else {
                	Map<String,Integer> supported = scanCRFResult.getSupportedTypes();
                	
                    if (!scanCRFResult.haveSupport()) {
                    	if (supported.size() >= 2) {
                    		//Rule 3a: recombinant of CRF and pure types
                    		concludeRule("3a", "Recombinant of " + toRecombinantString(scanCRFResult.getWindowCount(), supported), "Rule 3a");
                    	} else if (scanCRFResult.getBootscanSupport() > 0.7) {
                    		//Rule 3b=5 like rule 1 but for the CRF now
                    		rule3b(crfPhyloResult);
                    	} else if (scanCRFResult.getBootscanSupport() > 0.5) {
                    		//Rule 3c
                    		Concludable concluded = new ClusterLike(pureResult, pureResult.getConcludedCluster().getId() + "-like",
                    				pureResult.getConcludedCluster().getName() + ", potential recombinant");
                    		concludeRule("3c", concluded, "Rule 3c");
                    	} else {
                    		//Rule 3d
                            concludeRule("3d", "Recombinant", "Rule 3d");
                    	}
                    } else {
                        // Rule 4
                        concludeRule("4", crfResult,
                                "Subtype assigned based on sequence > 800bp, " +
                                "clustering with a CRF or subtype with bootstrap > 70%, " +
                                "with detection of recombination in the pure subtype bootscan, " +
                                "and further confirmed as a CRF or sub-subtype by bootscan analysis.");
                    }
               }
            }
        } else {
            PhyloClusterAnalysis.Result pure = purePuzzleAnalysis.run(s);
            
            if (!pure.haveSupport()) {
                // Rule 9
                concludeRule("9","Check the report",
                        "Subtype unassigned based on sequence &lt; 800bp, " +
                        "and not clustering with a pure subtype with bootstrap >70 %.");
            } else {
                if ((pure.getSupportInner() - pure.getSupportOuter()) > -50) {
                    // Rule 11
                    concludeRule("11",pure,
                            "Subtype assigned based on sequence &lt; 800bp, " +
                            "clustering with a pure subtype with bootstrap > 70%, " +
                            "and clustering inside the pure subtype cluster.");
                } else {
                    // Rule 10
                    concludeRule("10","Check the report",
                            "Subtype assigned based on sequence &lt; 800bp, " +
                            "clustering with a pure subtype with bootstrap > 70%, " +
                            "however not clustering inside the pure subtype cluster.");
                }
            }
        }
    }
    
    private void rule1(Result pureResult, Result crfResult){
    	pureRules("1", pureResult, crfResult, true);
    }

    private void rule3b(Result crfPhyloResult){
    	pureRules("5", crfPhyloResult, null, true);
    }

    private void rule2b(Result pureResult, Result crfResult){
    	pureRules("6", pureResult, crfResult, false);
    }

    private void pureRules(String rule, Result pureResult, Result crfResult, boolean clearlyNoRecombination){
    	String recombinationConclusion = clearlyNoRecombination ? 
    			"without recombination in the bootscan." : "without significant recombination in the bootscan.";
    	
    	if (pureResult.haveSupport()) {
            if (crfResult != null && crfResult.haveSupport() && (crfResult.getSupportInner() > crfResult.getSupportOuter())) {
                // Rule 1a pure (crf)
                concludeRule(rule + "a", pureResult, crfResult,
                    "Subtype assigned based on sequence > 800 bps " +
                    "clustering with a pure subtype and CRF or sub-subtype with bootstrap > 70% " +
                    recombinationConclusion);  
            } else if (pureResult.getSupportInner() > pureResult.getSupportOuter()) {
            	// rule 1b 
                concludeRule(rule + "b", pureResult,
                        "Subtype assigned based on sequence > 800 bps "
                        + "clustering with a pure subtype with bootstrap > 70% "
                        + recombinationConclusion);
            } else {
                // Rule 1c
            	Concludable concluded = new ClusterLike(pureResult, pureResult.getConcludedCluster().getId() + "-like",
        				pureResult.getConcludedCluster().getName() + "-like");
                concludeRule(rule + "c", concluded,
                        "Subtype unassigned based on sequence > 800 bps " +
                        "failure to classify as pure subtype (Bootstrap Support)" +
                        recombinationConclusion);
            }
        } else {
        	// Rule 1d
            concludeRule(rule + "d", "Check the report",
               "Subtype unassigned based on sequence > 800 bps " +
               "failure to classify as pure subtype (insufficient bootstrap support)" +
               recombinationConclusion);
        }
    }
    
    private String toRecombinantString(int totalWindows, Map<String, Integer> m) {
    	
    	// Order by prevalence
    	List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String,Integer>>(m.entrySet());
    	Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
   
    	StringBuilder sb = new StringBuilder();

    	boolean first = true;
    	for(Map.Entry<String, Integer> me : entries){
    		if(!first)
    			sb.append(", ");
    		else
    			first = false;
    		sb.append(me.getKey() /* +" ("+ me.getValue() +"/"+ totalWindows +")" */);
    	}
    	
    	return sb.toString();
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) hiv1.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }

}

