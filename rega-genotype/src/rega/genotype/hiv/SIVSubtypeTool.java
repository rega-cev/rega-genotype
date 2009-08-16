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
 * Date: Feb 9, 2006
 * Time: 5:50:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class SIVSubtypeTool extends GenotypeTool{
    private AlignmentAnalyses siv;
    private PhyloClusterAnalysis purePuzzleAnalysis;
    private PhyloClusterAnalysis pureAnalysis;
    private ScanAnalysis scanAnalysis;

    public SIVSubtypeTool() throws FileFormatException, IOException, ParameterProblemException {
        siv = readAnalyses("hcv.xml");
        pureAnalysis = (PhyloClusterAnalysis) siv.getAnalysis("pure");
        purePuzzleAnalysis = (PhyloClusterAnalysis)  siv.getAnalysis("puzzle-pure");
        scanAnalysis = (ScanAnalysis) siv.getAnalysis("scan");
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
     if (s.getLength() > 800) {
			PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);
			ScanAnalysis.Result scanresult = scanAnalysis.run(s);

			if (scanresult.haveSupport()) {
				if (pureresult.haveSupport()) {
					conclude(pureresult, "this is a pure subtype");

				} else {
					conclude("check the report", "this is a strange subtype");

				}

			}
		} else {
			conclude("check the report", "I am lazy to continuous");
		}
    }

     public void analyzeSelf() throws AnalysisException {
         ScanAnalysis scanPureSelfAnalysis
             = (ScanAnalysis) siv.getAnalysis("scan-pure-self");
         scanPureSelfAnalysis.run(null);
     }
 }
