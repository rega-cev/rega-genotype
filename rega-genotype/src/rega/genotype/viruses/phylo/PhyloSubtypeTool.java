package rega.genotype.viruses.phylo;
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
public class PhyloSubtypeTool extends GenotypeTool {
    private AlignmentAnalyses phylo;
    private PhyloClusterAnalysis pureAnalysis;


    public PhyloSubtypeTool() throws FileFormatException, IOException, ParameterProblemException {
        phylo = readAnalyses("hivphylotypes1.xml");
        pureAnalysis = (PhyloClusterAnalysis) phylo.getAnalysis("pure");

    }

    public void analyze(AbstractSequence s) throws AnalysisException {

           PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);

                if (pureresult.haveSupport()) {
                	if ((pureresult.getSupportInner() - pureresult.getSupportOuter()) > -50) {
                        // Rule 11
                        conclude(pureresult,
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
                 else {
                    conclude ("check the report", "Subtype unassigned based on sequence located in the LTR do not clustering with phylo subtype with bootstrap > 60%.");
                    return;
                }
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) phylo.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }
 }
