/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.hiv;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.ui.viruses.hiv.HivMain;

public class HIVTool extends GenotypeTool {	
    private AlignmentAnalyses hiv;
    private BlastAnalysis blastAnalysis;
    private HIV1SubtypeTool hiv1subtypetool;
	private HIV2SubtypeTool hiv2subtypetool;

	public HIVTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
		this(null, workingDir);
	}
	public HIVTool(String toolId, File workingDir) throws IOException, ParameterProblemException, FileFormatException {
		super(toolId == null ? HivMain.HIV_TOOL_ID : toolId, workingDir);
		
		String file = getXmlPathAsString() + File.separator + "hiv.xml";
		hiv = readAnalyses(file, workingDir);
        blastAnalysis = (BlastAnalysis) hiv.getAnalysis("blast");
        
        hiv1subtypetool = new HIV1SubtypeTool(workingDir);
        hiv1subtypetool.setParent(this);
        hiv2subtypetool = new HIV2SubtypeTool(workingDir);
        hiv2subtypetool.setParent(this);
    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        BlastAnalysis.Result result = blastAnalysis.run(s);
        
        if (result.haveSupport()) {
            if (result.getCluster().getId().equals("1"))
                hiv1subtypetool.analyze(s);
            else if (result.getCluster().getId().equals("4"))
            	hiv2subtypetool.analyze(s);
            else
                conclude(result, "Identified with BLAST score > 200", null);
        } else {

            conclude("Unassigned", "Unassigned because of BLAST score &lt; 200.", null);
        }
    }

	public void analyzeSelf() throws AnalysisException {
	}

	@Override
	protected String currentJob() {
		return workingDir.getName();
	}

	@Override
	protected boolean cancelAnalysis() {
		return new File(workingDir, ".CANCEL").exists();
	}
}

