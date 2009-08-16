/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.hiv;

import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;

public class PhylotypeTool extends GenotypeTool {

    private AlignmentAnalyses hiv;
    private BlastAnalysis blastAnalysis;
    private HIV1PhylotypeTool hiv1subtypetool;
	private HIV2SubtypeTool hiv2subtypetool;

    
    public PhylotypeTool() throws IOException, ParameterProblemException, FileFormatException {
        hiv = readAnalyses("hiv.xml");
        blastAnalysis = (BlastAnalysis) hiv.getAnalysis("blast");
        
        hiv1subtypetool = new HIV1PhylotypeTool();
        hiv1subtypetool.setParent(this);
        hiv2subtypetool = new HIV2SubtypeTool();
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
                conclude(result, "Identified with BLAST score > 200");
        } else {

            conclude("Unassigned", "Unassigned because of BLAST score &lt; 200.");
        }
    }

	public void analyzeSelf() throws AnalysisException {
	}
}

