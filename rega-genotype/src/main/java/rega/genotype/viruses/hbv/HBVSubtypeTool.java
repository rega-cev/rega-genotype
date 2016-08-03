/*
 * Created on Feb 6, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.hbv;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.ScanAnalysis;

public class HBVSubtypeTool extends GenotypeTool {
    private AlignmentAnalyses hbv;
    private PhyloClusterAnalysis pureAnalysis;
    private ScanAnalysis scanAnalysis;
    private PhyloClusterAnalysis purePuzzleAnalysis;



    public HBVSubtypeTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        hbv = readAnalyses("HBV/hbv.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) hbv.getAnalysis("pure");
        purePuzzleAnalysis = (PhyloClusterAnalysis) hbv.getAnalysis("pure-puzzle");
        scanAnalysis = (ScanAnalysis) hbv.getAnalysis("scan-pure");

    }

    public void analyze(AbstractSequence s) throws AnalysisException {
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
                    } 
                 else {
//                	 Rule 3
                    conclude("Check the Report",
                       "Subtype unassigned based on sequence > 800 bps " +
                       "failure to classify as pure subtype (Bootstrap Support)" +
                       "without recombination in the bootscan.");
                } 
 
            }  
            if (!scanResult.haveSupport()) {                
            	// Rule 2 & 4
                conclude ("Check the Bootscan",
                        "Subtype unassigned based on sequence > 800 bps " +
                        "with recombination in the bootscan.");

               } 
        }else {
            
        	PhyloClusterAnalysis.Result pure = purePuzzleAnalysis.run(s);
            
            if (!pure.haveSupport()) {
                // Rule 7
                conclude("Check the report",
                        "Subtype unassigned based on sequence &lt; 800bp, " +
                        "and not clustering with a pure subtype with bootstrap >70 %.");
            } else {
                if (pure.getSupportInner() > pure.getSupportOuter()) {
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
            }
        }
    }
    
    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) hbv.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }

}

