package rega.genotype.viruses.htlv;
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
public class HTLV1SubtypeTool extends GenotypeTool {
    private AlignmentAnalyses htlv1;
    private PhyloClusterAnalysis pureAnalysis;


    public HTLV1SubtypeTool(File workingDir) throws FileFormatException, IOException, ParameterProblemException {
        htlv1 = readAnalyses("htlv.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) htlv1.getAnalysis("pure");

    }

    public void analyze(AbstractSequence s) throws AnalysisException {

           PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);

                if (pureresult.haveSupport()) {
                    conclude (pureresult, 
                            "Subtype assigned based on sequence located in the LTR "
                            + "clustering with a HTLV1 subtype and/or subgroup with bootstrap > 75% ");
                    return;
                } else {
                    conclude ("check the report", "Subtype unassigned based on sequence located in the LTR do not clustering with HTLV1 subtype with bootstrap > 75%.");
                    return;
                }
    
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) htlv1.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }
 }
