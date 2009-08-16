package rega.genotype.viruses.hiv;

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

/**
 * Created by IntelliJ IDEA.
 * User: tulio
 * Date: Feb 9, 2006
 * Time: 4:38:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class HIV2SubtypeTool extends GenotypeTool {
    private AlignmentAnalyses hiv2;
    private PhyloClusterAnalysis pureAnalysis;
    private ScanAnalysis scanAnalysis;

    public HIV2SubtypeTool(File workingDir) throws FileFormatException, IOException, ParameterProblemException {
        hiv2 = readAnalyses("HIV/hiv2.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) hiv2.getAnalysis("pure");
        scanAnalysis = (ScanAnalysis) hiv2.getAnalysis("scan");
    }


    public void analyze(AbstractSequence s) throws AnalysisException {
        if (s.getLength() > 800) {
            PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);

            ScanAnalysis.Result scanresult = scanAnalysis.run(s);

            if (scanresult.haveSupport()) {
                if (pureresult.haveSupport()) {
                    if (pureresult.getBestCluster().getName().contains("SMM")) {
                        conclude ("Your sequence is not HIV2", "Use the SIV Tool");
                        return;

                    }
                    if (pureresult.getBestCluster().getName().contains("RCM")) {
                        conclude ("Your sequence is not HIV2", "Use the SIV Tool");
                        return;
                    }

                    conclude (pureresult,
                            "Supported by boots > 70 and Bootscan > 0.9");
                }

                else {
                    conclude ("check the report",
                            "not supported by boots > 70 and Bootscan >0.9");
                }
            }

            else {
                conclude ("check the bootscan", "Supported by boots > 70 with detection of recombination in the bootscan");
            }

        }   else {
            conclude ("tulio did not finish the program","koen will not finish because he is tired to work for free !");
        }

    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) hiv2.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }
}

