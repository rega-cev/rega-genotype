/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.hhv8;

import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;

public class HHV8Tool extends GenotypeTool {

    private AlignmentAnalyses hhv8;
    private BlastAnalysis blastAnalysis;
    private HHV8SubtypeTool hhv8subtypetool;

    
    public HHV8Tool() throws IOException, ParameterProblemException, FileFormatException {
        hhv8 = readAnalyses("hhv8blast.xml");
        blastAnalysis = (BlastAnalysis) hhv8.getAnalysis("blast");
        
        hhv8subtypetool = new HHV8SubtypeTool();
        hhv8subtypetool.setParent(this);

    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        BlastAnalysis.Result result = blastAnalysis.run(s);
        
        if (result.haveSupport()) {
            if (result.getCluster().getId().equals("1")) 
            	if  (result.getStart() < 1000) {
                hhv8subtypetool.analyze(s);
                conclude(result, "K1 region Identified with BLAST score > 200");
            	}
                else
                conclude(result, "HHV8 genomic region not supported for subtyping analysis identified with BLAST score > 200");
        } else {

            conclude("Unassigned", "Unassigned because of BLAST score &lt; 200.");
        }
    }

	public void analyzeSelf() throws AnalysisException {
	}
}

