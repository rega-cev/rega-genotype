package rega.genotype.viruses.dengue;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.viruses.dengue.DENGUE1SubtypeTool;
import rega.genotype.viruses.dengue.DENGUE2SubtypeTool;
import rega.genotype.viruses.dengue.DENGUE3SubtypeTool;
import rega.genotype.viruses.dengue.DENGUE4SubtypeTool;

public class DENGUETool extends GenotypeTool {

	private File workingDir;
    private AlignmentAnalyses dengue;
    private BlastAnalysis blastAnalysis;
    private DENGUE1SubtypeTool dengue1subtypetool;
    private DENGUE2SubtypeTool dengue2subtypetool;
    private DENGUE3SubtypeTool dengue3subtypetool;
    private DENGUE4SubtypeTool dengue4subtypetool;
    
    public DENGUETool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
    	this.workingDir = workingDir;
        
    	dengue = readAnalyses("DENGUE/dengueblast.xml", workingDir);
        blastAnalysis = (BlastAnalysis) dengue.getAnalysis("blast");
        
        dengue1subtypetool = new DENGUE1SubtypeTool(workingDir);
        dengue1subtypetool.setParent(this);
		dengue2subtypetool = new DENGUE2SubtypeTool(workingDir);
        dengue2subtypetool.setParent(this);
        dengue3subtypetool = new DENGUE3SubtypeTool(workingDir);
        dengue3subtypetool.setParent(this);
        dengue4subtypetool = new DENGUE4SubtypeTool(workingDir);
        dengue4subtypetool.setParent(this);

    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        BlastAnalysis.Result result = blastAnalysis.run(s);
        
        if (result.haveSupport()) {
            if (result.getCluster().getId().equals("1")) 
            	dengue1subtypetool.analyze(s);
            else if (result.getCluster().getId().equals("2"))
				dengue2subtypetool.analyze(s);
            else if (result.getCluster().getId().equals("3"))
				dengue3subtypetool.analyze(s);
            else if (result.getCluster().getId().equals("4"))
				dengue4subtypetool.analyze(s);
			else
				conclude(result, "DENGUE genomic region not supported for subtyping analysis identified with BLAST score > 200");
        } else {
            conclude("Unassigned", "Unassigned because of BLAST score &lt; 200.");
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
