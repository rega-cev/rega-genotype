package rega.genotype.hiv;
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
    private ScanAnalysis scanAnalysis;
    private PhyloClusterAnalysis purePuzzleAnalysis;
    private PhyloClusterAnalysis pureAnalysis;


    public HTLV1SubtypeTool() throws FileFormatException, IOException, ParameterProblemException {
        htlv1 = readAnalyses("htlv.xml");
        pureAnalysis = (PhyloClusterAnalysis) htlv1.getAnalysis("pure");
        purePuzzleAnalysis = (PhyloClusterAnalysis) htlv1.getAnalysis("pure-puzzle");
        scanAnalysis = (ScanAnalysis) htlv1.getAnalysis("scan");
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
       if (s.getLength() >800 ) {
           PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);
           ScanAnalysis.Result scanresult = scanAnalysis.run(s);
            if (scanresult.haveSupport()) {
                if (pureresult.haveSupport()) {
                    conclude (pureresult, "this is a pure subtype");
                    return;
                } else {
                    conclude ("check the report", "not supported by bootstrap values");
                    return;
                }
            }

       } else {
           PhyloClusterAnalysis.Result pure = purePuzzleAnalysis.run(s);
            if (pure.haveSupport()) {
                conclude (pure, "This is a pure subtype");
                return;
            } else {
                conclude ("check the report", "not supported by bootstrap values II" );
                return;
            }
       }
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) htlv1.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }
 }
