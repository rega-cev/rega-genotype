package rega.genotype.viruses.hhv8;
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
 * Date: Feb 10, 2006
 * Time: 8:45:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class HHV8SubtypeTool extends GenotypeTool {
    private AlignmentAnalyses hhv8;
    private PhyloClusterAnalysis pureAnalysis;


    public HHV8SubtypeTool(File workingDir) throws FileFormatException, IOException, ParameterProblemException {
        hhv8 = readAnalyses("hhv8K1.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) hhv8.getAnalysis("pure-puzzle");

    }

    public void analyze(AbstractSequence s) throws AnalysisException {

           PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);

                if (pureresult.haveSupport()) {
                    conclude (pureresult, 
                            "Subtype assigned based on sequence located in the K1"
                            + "clustering with a HHV8 subtype and/or subgroup with bootstrap > 75% ");
                    return;
                } else {
                    conclude ("check the report", "Subtype unassigned based on sequence located in the K1 do not clustering with HHV8 subtype with bootstrap > 75%.");
                    return;
                }
    
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) hhv8.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }
 }
