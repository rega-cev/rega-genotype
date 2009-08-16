/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.hpv;

import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;

public class HPVTool extends GenotypeTool {

    private AlignmentAnalyses hpv;
    private BlastAnalysis blastAnalysis;
    private HPVSubtypeTool hpvsubtypetool;

    
    public HPVTool() throws IOException, ParameterProblemException, FileFormatException {
        hpv = readAnalyses("hpvblast.xml");
        blastAnalysis = (BlastAnalysis) hpv.getAnalysis("blast");
        
        hpvsubtypetool = new HPVSubtypeTool();
        hpvsubtypetool.setParent(this);

    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        BlastAnalysis.Result result = blastAnalysis.run(s);
        
        if (result.haveSupport()) {
            if (result.getCluster().getId().equals("1")) 
            	if  (result.getStart() > 5000 && result.getEnd() <7000) {
                hpvsubtypetool.analyze(s);
                conclude(result, "L1 ORF region Identified with BLAST score > 200");
            	}
                else
                conclude(result, "HPV genomic region not supported for subtyping analysis identified with BLAST score > 200");
        } else {

            conclude("Unassigned", "Unassigned because of BLAST score &lt; 200.");
        }
    }

	public void analyzeSelf() throws AnalysisException {
	}
}

