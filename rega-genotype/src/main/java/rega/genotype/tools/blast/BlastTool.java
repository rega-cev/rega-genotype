package rega.genotype.tools.blast;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.AnalysisException;
import rega.genotype.ApplicationException;
import rega.genotype.BlastAnalysis;
import rega.genotype.BlastAnalysis.Result;
import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;

/**
 * BlastTool is a GenericTool tool that performs blast analysis that 
 * identifies all the viruses of given fasta file and can redirect 
 * a virus sequence to the virus tool.
 *
 * @author michael
 */
public class BlastTool extends GenotypeTool {
	private AlignmentAnalyses blastXml;
	private BlastAnalysis blastAnalysis;

    public BlastTool(ToolConfig toolConfig, File workDir) throws IOException, ParameterProblemException, FileFormatException {
    	super(toolConfig, workDir);

    	blastXml = readAnalyses(getXmlPathAsString() + "blast.xml", getWorkingDir());
    	blastAnalysis = (BlastAnalysis) blastXml.getAnalysis("blast");
    }

    @Override
    public void analyze(AbstractSequence s) throws AnalysisException {
    	Result blastResult = blastAnalysis.run(s);
    	if (blastResult.haveSupport() && blastResult.getConcludedCluster() != null) {
    		Cluster c = blastResult.getConcludedCluster();
    		analyseClaster(c, s);
    	} 
    	conclude(blastAnalysis, blastResult);
    }

	/**
	 * Blast tool will order the results buy tool ids, so later the specific tool
	 * can make analysis only on the related sequence.
	 */
	protected void analyseClaster(Cluster c, AbstractSequence s) {
		try {
			String fasta = ">" + s.getName() + "\n" + s.getSequence() + "\n";

			if (c != null) {
				File f = new File(getWorkingDir().getAbsolutePath(), c.getToolId() + ".fasta");
				FileUtil.writeStringToFile(f, fasta, true);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static File sequenceFileInBlastTool(String blastJobId, String toolId, String blastToolVersion){
		String blastJobDir = Settings.getInstance().getConfig().getBlastTool(blastToolVersion).getJobDir();
		return new File(blastJobDir + File.separator + blastJobId, toolId + ".fasta");
	}

	@Override
	public void analyzeSelf(String traceFile, String analysisFile,
			int windowSize, int stepSize, String analysisId)
			throws AnalysisException {
		// TODO Auto-generated method stub
		super.analyzeSelf(traceFile, analysisFile, windowSize, stepSize, analysisId);
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
		blastAnalysis.formatDB(blastXml.getAlignment());		
	}
}
