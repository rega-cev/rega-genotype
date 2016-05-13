/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.hcv;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;

public class HCVTool extends GenotypeTool {
	public static final String HCV_TOOL_ID = "hcv"; // TODO: Temporary till hiv becomes generic tool.

    private AlignmentAnalyses hcv;
    private BlastAnalysis blastAnalysis;
    private HCVSubtypeTool hcvsubtypetool;
    public HCVTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
    	this(null, workingDir);
    }
    public HCVTool(String toolId, File workingDir) throws IOException, ParameterProblemException, FileFormatException {
    	super(toolId == null ? HCVTool.HCV_TOOL_ID : toolId, workingDir);

		String file = getXmlPathAsString() + File.separator + "hcvblast.xml";
    	hcv = readAnalyses(file, workingDir);
        blastAnalysis = (BlastAnalysis) hcv.getAnalysis("blast");
        
        hcvsubtypetool = new HCVSubtypeTool(workingDir);
        hcvsubtypetool.setParent(this);

    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        BlastAnalysis.Result result = blastAnalysis.run(s);
        
        if (result.haveSupport()) {
            if (result.getCluster().getId().equals("1"))
                hcvsubtypetool.analyze(s, result);
            else
                conclude(result, "Identified with BLAST score > 200", null);
        } else {
            conclude("Unassigned", "Unassigned because of BLAST score &lt; 200.", null);
        }
    }

	public void analyzeSelf() throws AnalysisException {
	}
	
	@Override
	protected boolean cancelAnalysis() {
		return false;
	}

	@Override
	protected String currentJob() {
		return null;
	}
}

