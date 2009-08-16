/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.htlv;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;

public class HTLVTool extends GenotypeTool {

    private AlignmentAnalyses htlv;
    private BlastAnalysis blastAnalysis;
    private HTLV1SubtypeTool htlv1subtypetool;

    
    public HTLVTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        htlv = readAnalyses("htlvblast.xml", workingDir);
        blastAnalysis = (BlastAnalysis) htlv.getAnalysis("blast");
        
        htlv1subtypetool = new HTLV1SubtypeTool(workingDir);
        htlv1subtypetool.setParent(this);

    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        BlastAnalysis.Result result = blastAnalysis.run(s);
        
        if (result.haveSupport()) {
            if (result.getCluster().getId().equals("1")) 
            	if  (result.getStart() < 750) {
                htlv1subtypetool.analyze(s);
                conclude(result, "LTR region Identified with BLAST score > 200");
            	}
                else
                conclude(result, "HTLV1 genomic region not supported for subtyping analysis identified with BLAST score > 200");
        } else {

            conclude("Unassigned", "Unassigned because of BLAST score &lt; 200.");
        }
    }

	public void analyzeSelf() throws AnalysisException {
	}
}

