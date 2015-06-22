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
 * User: Vagner
 * Date: Apr 21, 2015
 * Time: 14:14:42 AM
 * To change this template use File | Settings | File Templates.
 */

public class HTLV2SubtypeTool extends GenotypeTool {
	
	private AlignmentAnalyses htlv2;
    private PhyloClusterAnalysis pureAnalysis;


    public HTLV2SubtypeTool(File workingDir) throws FileFormatException, IOException, ParameterProblemException {
        htlv2 = readAnalyses("HTLV/htlv2.xml", workingDir);
        pureAnalysis = (PhyloClusterAnalysis) htlv2.getAnalysis("pure");

    }

    public void analyze(AbstractSequence s) throws AnalysisException {

           PhyloClusterAnalysis.Result pureresult = pureAnalysis.run(s);

                if (pureresult.haveSupport()) {
                    conclude (pureresult, 
                            "Subtype assigned based on sequence located in the LTR "
                            + "clustering with a HTLV2 subtype and/or subgroup with bootstrap > 70% ");
                    return;
                } else {
                    conclude ("check the report", "Subtype unassigned based on sequence located in the LTR do not clustering with HTLV2 subtype with bootstrap > 70%.");
                    return;
                }
    
    }

    public void analyzeSelf() throws AnalysisException {
        ScanAnalysis scanPureSelfAnalysis
            = (ScanAnalysis) htlv2.getAnalysis("scan-pure-self");
        scanPureSelfAnalysis.run(null);
    }

	@Override
	protected String currentJob() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean cancelAnalysis() {
		// TODO Auto-generated method stub
		return false;
	}
 }
