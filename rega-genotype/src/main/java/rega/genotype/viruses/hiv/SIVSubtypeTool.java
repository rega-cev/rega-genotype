package rega.genotype.viruses.hiv;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.PhyloClusterAnalysis;
import rega.genotype.ScanAnalysis;
import rega.genotype.ui.viruses.hiv.HivMain;

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
 
    public SIVSubtypeTool(File workingDir) throws FileFormatException, IOException, ParameterProblemException {
    	this(null, workingDir);
    }
    public SIVSubtypeTool(String toolid, File workingDir) throws FileFormatException, IOException, ParameterProblemException {
		super(toolid == null ? HivMain.HIV_TOOL_ID : toolid, workingDir);

		// TODO "hcv.xml" does not make sense to me ?
    	siv = readAnalyses("hcv.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) siv.getAnalysis("pure");
        purePuzzleAnalysis = (PhyloClusterAnalysis)  siv.getAnalysis("puzzle-pure");
        scanAnalysis = (ScanAnalysis) siv.getAnalysis("scan-pure");
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
     if (s.getLength() > 800) {
			PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);
			ScanAnalysis.Result scanresult = scanAnalysis.run(s);

			if (scanresult.haveSupport()) {
				if (pureresult.haveSupport()) {
					conclude(pureresult, "this is a pure subtype", null);

				} else {
					conclude("check the report", "this is a strange subtype", null);

				}

			}
		} else {
			conclude("check the report", "I am lazy to continuous", null);
		}
    }

     public void analyzeSelf() throws AnalysisException {
         ScanAnalysis scanPureSelfAnalysis
             = (ScanAnalysis) siv.getAnalysis("scan-pure-self");
         scanPureSelfAnalysis.run(null);
     }

	@Override
	protected String currentJob() {
		return null;
	}

	@Override
	protected boolean cancelAnalysis() {
		return false;
	}

	@Override
	protected void formatDB() throws ApplicationException {
		
	}
 }
