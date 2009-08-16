package rega.genotype.viruses.hpv;
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
public class HPVSubtypeTool extends GenotypeTool {
    private AlignmentAnalyses hpv;
    private PhyloClusterAnalysis pureAnalysis;


    public HPVSubtypeTool(File workingDir) throws FileFormatException, IOException, ParameterProblemException {
        hpv = readAnalyses("PVgeneraL1v2.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) hpv.getAnalysis("pure");

    }

    public void analyze(AbstractSequence s) throws AnalysisException {

           PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);

                if (pureresult.haveSupport()) {
                    conclude (pureresult, 
                            "Subtype assigned based on sequence located in the L1 ORF "
                            + "clustering with a hpv subtype and/or subgroup with bootstrap > 70% ");
                    return;
                } else {
                    conclude ("check the report", "Subtype unassigned based on sequence located in the LTR do not clustering with hpv subtype with bootstrap > 60%.");
                    return;
                }
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) hpv.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }
 }
