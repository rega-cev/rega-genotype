/*
 * Created on Feb 6, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.hiv;

import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.ScanAnalysis;

public class HIV1SubtypeTool extends GenotypeTool {
    private AlignmentAnalyses hiv1;
    private PhyloClusterAnalysis pureAnalysis;
    private ScanAnalysis scanAnalysis;
    private PhyloClusterAnalysis crfAnalysis;
    private PhyloClusterAnalysis purePuzzleAnalysis;

    private ScanAnalysis crfScanAnalysis;
    private PhyloClusterAnalysis crfScanPhyloAnalysis;

    public HIV1SubtypeTool() throws IOException, ParameterProblemException, FileFormatException {
        hiv1 = readAnalyses("HIV/hiv1.xml");
        pureAnalysis = (PhyloClusterAnalysis) hiv1.getAnalysis("pure");
        purePuzzleAnalysis = (PhyloClusterAnalysis) hiv1.getAnalysis("pure-puzzle");
        scanAnalysis = (ScanAnalysis) hiv1.getAnalysis("scan");
        crfScanAnalysis = (ScanAnalysis) hiv1.getAnalysis("crfscan");
        crfScanPhyloAnalysis = (PhyloClusterAnalysis) crfScanAnalysis.getAnalysis();
        crfAnalysis = (PhyloClusterAnalysis) hiv1.getAnalysis("crf");
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        if (s.getLength() > 800) {
            PhyloClusterAnalysis.Result pureResult = pureAnalysis.run(s);
            ScanAnalysis.Result scanResult = scanAnalysis.run(s);
            PhyloClusterAnalysis.Result crfResult = crfAnalysis.run(s);
            ScanAnalysis.Result scanCRFResult = null;

            if (crfResult.haveSupport() && crfResult.getBestCluster().haveTag("CRF")) {
                crfScanPhyloAnalysis.getClusters().add(crfResult.getBestCluster());

                scanCRFResult = crfScanAnalysis.run(s);

                crfScanPhyloAnalysis.getClusters().remove(crfResult.getBestCluster());
            }

            if (scanResult.haveSupport()) {
                if (pureResult.haveSupport()) {
                    if (crfResult.haveSupport()
                        && (crfResult.getSupportInner() > crfResult.getSupportOuter())) {
                        // Rule 1a
                        conclude(pureResult,crfResult,
                            "Subtype assigned based on sequence > 800 bps " +
                            "clustering with a pure subtype and CRF or sub-subtype with bootstrap > 70% " +
                            "without recombination in the bootscan.");  
                    } else {
                        // 1b
                        conclude(pureResult,
                            "Subtype assigned based on sequence > 800 bps "
                            + "clustering with a pure subtype with bootstrap > 70% "
                            + "without recombination in the bootscan.");
                    }
                } else {
                    conclude("Check the Report",
                       "Subtype unassigned based on sequence > 800 bps " +
                       "failure to classify as pure subtype (Bootstrap Support)" +
                       "without recombination in the bootscan.");
                }
            } else {                
                if (!crfResult.haveSupport()) {
                    // Rule 2
                    conclude("Check the bootscan",
                            "Subtype unassigned based on sequence > 800 bps " +
                            "clustering with a pure subtype with bootstrap > 70% " +
                            "with detection of recombination in the bootscan, " +
                            "and failure to classify as a CRF or sub-subtype (bootstrap support).");
                } else {
                    if (crfResult.getBestCluster().haveTag("CRF")) {
                        if (!scanCRFResult.haveSupport()) {
                            if (pureResult.haveSupport()) {
                                //Rule 3
                                conclude("Check the bootscan",
                                        "Subtype unassigned based on sequence > 800bp, " +
                                        "clustering with a pure subtype and CRF or sub-subtype with bootstrap >70 %, " +
                                        "with detection of recombination in the pure subtype bootscan, " +
                                        "and failure to classify as a CRF or sub-subtype by bootscan analysis.");
                            } else {
                                // Rule 7
                                conclude("Check the bootscan",
                                        "Subtype unassigned based on sequence > 800bp, " +
                                        "clustering with a CRF or sub-subtype with bootstrap >70 %, " +
                                        "with detection of recombination in the pure subtype bootscan, " +
                                        "and failure to classify as a CRF or sub-subtype by bootscan analysis.");
                               
                            }
                        } else {
                            // Rule 4-8
                            conclude(crfResult,
                                    "Subtype assigned based on sequence > 800bp, " +
                                    "clustering with a CRF or sub-subtype with bootstrap > 70%, " +
                                    "with detection of recombination in the pure subtype bootscan, " +
                                    "and further confirmed as a CRF or sub-subtype by bootscan analysis.");
                        }
                    } else {
                        conclude("Check the bootscan", 
                        		  	"Subtype unassigned based on sequence > 800bp, " +
                        			"not clustering with a pure subtype with bootstrap >70 %," +
                        			"with or without detection of recombination in the pure subtype bootscan");
                    }
                }
            }
        } else {
            PhyloClusterAnalysis.Result pure = purePuzzleAnalysis.run(s);
            
            if (!pure.haveSupport()) {
                // Rule 9
                conclude("Check the report",
                        "Subtype unassigned based on sequence &lt; 800bp, " +
                        "and not clustering with a pure subtype with bootstrap >70 %.");
            } else {
                if ((pure.getSupportInner() - pure.getSupportOuter()) > -50) {
                    // Rule 11
                    conclude(pure,
                            "Subtype assigned based on sequence &lt; 800bp, " +
                            "clustering with a pure subtype with bootstrap > 70%, " +
                            "and clustering inside the pure subtype cluster.");
                } else {
                    // Rule 10
                    conclude("Check the report",
                            "Subtype assigned based on sequence &lt; 800bp, " +
                            "clustering with a pure subtype with bootstrap > 70%, " +
                            "however not clustering inside the pure subtype cluster.");
                }
            }
        }
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) hiv1.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }

}

