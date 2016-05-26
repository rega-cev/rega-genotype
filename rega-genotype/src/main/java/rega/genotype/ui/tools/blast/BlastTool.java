package rega.genotype.ui.tools.blast;

import java.io.File;
import java.io.IOException;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;
import rega.genotype.viruses.generic.GenericTool;

/**
 * BlastTool is a GenericTool tool that performs blast analysis that 
 * identifies all the viruses of given fasta file and can redirect 
 * a virus sequence to the virus tool.
 *
 * @author michael
 */
public class BlastTool extends GenericTool {

	public BlastTool(ToolConfig toolConfig, File workDir) throws IOException,
	ParameterProblemException, FileFormatException {
		super(toolConfig, workDir);
	}

	/**
	 * Blast tool will order the results buy tool ids, so later the specific tool
	 * can make analysis only on the related sequence.
	 */
	@Override
	protected void analyseClaster(Cluster c, AbstractSequence s) {
		try {
			String fasta = ">" + s.getName() + "\n" + s.getSequence() + "\n";

			File f = new File(getWorkingDir().getAbsolutePath(), c.getToolId() + ".fasta");
			FileUtil.writeStringToFile(f, fasta, true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static File sequenceFileInBlastTool(String blastJobId, String toolId, String blastToolVersion){
		String blastJobDir = Settings.getInstance().getConfig().getBlastTool(blastToolVersion).getJobDir();
		return new File(blastJobDir + File.separator + blastJobId, toolId + ".fasta");
	}
}
