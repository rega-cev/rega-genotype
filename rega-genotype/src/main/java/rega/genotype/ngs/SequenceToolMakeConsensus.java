package rega.genotype.ngs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import rega.genotype.AbstractSequence;
import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.config.NgsModule;
import rega.genotype.singletons.Settings;

public class SequenceToolMakeConsensus {
	/**
	 * Try to connect all the contigs.
	 * Reference sequence: use blast on ncbi viruses db with query = longest contig  
	 * @param assembledContigs: will be aligned
	 * @param workDir
	 * @param virusName
	 * @return
	 * @throws ApplicationException
	 * @throws IOException
	 * @throws FileFormatException
	 * @throws ParameterProblemException
	 * @throws InterruptedException
	 */
	public static File consensusAlign(File assembledContigs, AbstractSequence reference,
			File consensusWorkDir, String virusName, NgsModule ngsModule, Logger logger) throws ApplicationException, IOException, FileFormatException, ParameterProblemException, InterruptedException {
		// make Consensus

		String sequencetoolPath = Settings.getInstance().getConfig().getGeneralConfig().getSequencetool();

		File alingment = NgsFileSystem.consensusAlingmentFile(consensusWorkDir);
		alingment.getParentFile().mkdirs();

		File referenceFile = NgsFileSystem.consensusRefFile(consensusWorkDir);
		FileOutputStream fos = new FileOutputStream(referenceFile);
		reference.writeFastaOutput(fos);
		fos.close();

		String cmd = sequencetoolPath + " consensus-align"
		+ " --reference " + referenceFile.getAbsolutePath()
		+ " --target " + assembledContigs.getAbsolutePath()
		+ " --output " + alingment.getAbsolutePath()
		+ " --absolute-cutoff " + ngsModule.getConsensusToolAbsoluteCutoff()
		+ " --relative-cutoff " + ngsModule.getConsensusToolRelativeCutoff()
		+ " --min-single-seq-cov " + ngsModule.getConsensusToolMinSingleSeqCov();

		NgsFileSystem.executeCmd(cmd, consensusWorkDir, logger);

		return alingment;
	}

	public static File makeConsensus(File assembledContigs,
			File consensusWorkDir, String virusName, NgsModule ngsModule, Logger logger) throws ApplicationException, IOException, FileFormatException, ParameterProblemException, InterruptedException {

		String sequencetoolPath = Settings.getInstance().getConfig().getGeneralConfig().getSequencetool();
		File outContigs = NgsFileSystem.consensusContigsFile(consensusWorkDir);
		outContigs.getParentFile().mkdirs();
		outContigs.createNewFile();

		File out = NgsFileSystem.consensusFile(consensusWorkDir);
		out.getParentFile().mkdirs();
		out.createNewFile();
		
		String cmd = sequencetoolPath + " make-consensus"
				+ " --input " + assembledContigs.getAbsolutePath()
				+ " --output " + out.getAbsolutePath()
				+ " --output-contigs " + outContigs.getAbsolutePath()
				+ " --max-gap " + ngsModule.getConsensusToolMaxGap()
				+ " --max-missing " + ngsModule.getConsensusToolMaxMissing()
				+ " --min-count " + ngsModule.getConsensusToolMinCount()
				+ " --mixture-min-pct " + ngsModule.getConsensusToolMixtureMinPct();

		NgsFileSystem.executeCmd(cmd, consensusWorkDir, logger);

		return outContigs;
	}
}
