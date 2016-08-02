/*
 * Created on Feb 8, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype.viruses.phylo;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AnalysisException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;

public class PhyloTool extends GenotypeTool {

    private AlignmentAnalyses phylo;
    private BlastAnalysis blastAnalysis;
    private PhyloSubtypeTool phylosubtypetool;

    
    public PhyloTool(File workingDir) throws IOException, ParameterProblemException, FileFormatException {
        phylo = readAnalyses("PHYLO/hiv.xml", workingDir);
        blastAnalysis = (BlastAnalysis) phylo.getAnalysis("blast");
        
        phylosubtypetool = new PhyloSubtypeTool(workingDir);
        phylosubtypetool.setParent(this);

    }

    public void analyze(AbstractSequence s) throws AnalysisException {
        BlastAnalysis.Result result = blastAnalysis.run(s);
        
        if (result.haveSupport()) {
            if (result.getCluster().getId().equals("1")) 
                phylosubtypetool.analyze(s);
           } else {

            conclude("Unassigned", "Unassigned because of BLAST score &lt; 200.");
        }
    }

	public void analyzeSelf() throws AnalysisException {
	}
}

