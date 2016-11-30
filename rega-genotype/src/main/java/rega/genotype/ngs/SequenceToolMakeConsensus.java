package rega.genotype.ngs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.NgsModule;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.BlastUtil;

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
	public static File consensusAlign(File assembledContigs,
			File workDir, String virusName, NgsModule ngsModule) throws ApplicationException, IOException, FileFormatException, ParameterProblemException, InterruptedException {

		// Create make contigs in. format: first refseq then all the contigs. 

		// find nucleotide reference sequence for the basket.
		// TODO:find the species for the largest contigs.
		FileReader fileReader = new FileReader(assembledContigs.getAbsolutePath());
		LineNumberReader lnr = new LineNumberReader(fileReader);
		// Note: Spades orders results by length.
		Sequence longestContig = SequenceAlignment.readFastaFileSequence(lnr, SequenceAlignment.SEQUENCE_DNA);
		fileReader.close();
		lnr.close();
		if (longestContig == null)
			return null;

		workDir.mkdirs();

		File ncbiVirusesFasta = Settings.getInstance().getConfig().getNcbiVirusesDb();
		if (ncbiVirusesFasta == null)
			throw new ApplicationException("Ncbi Viruses Db Path needs to be set in global settings");
		File refrence = NgsFileSystem.consensusRefFile(workDir, virusName);
		File consensusDir = new File(workDir, NgsFileSystem.consensusDir(virusName));
		consensusDir.mkdirs();

		//BlastUtil.formatDB(ncbiVirusesFasta, consensusDir);
		BlastUtil.computeBestRefSeq(longestContig, consensusDir, refrence, ncbiVirusesFasta);

		// make Consensus

		String sequencetoolPath = Settings.getInstance().getConfig().getGeneralConfig().getSequencetool();

		File alingment = NgsFileSystem.consensusAlingmentFile(workDir, virusName);
		alingment.getParentFile().mkdirs();

		String cmd = sequencetoolPath + " consensus-align"
		+ " --reference " + refrence.getAbsolutePath()
		+ " --target " + assembledContigs.getAbsolutePath()
		+ " --output " + alingment.getAbsolutePath()
		+ " --cutoff " + ngsModule.getConsensusToolCutoff()
		+ " --min-single-seq-cov " + ngsModule.getConsensusToolMinSingleSeqCov();

		NgsFileSystem.executeCmd(cmd, workDir);

		return alingment;
	}

	public static File makeConsensus(File assembledContigs,
			File workDir, String virusName, NgsModule ngsModule) throws ApplicationException, IOException, FileFormatException, ParameterProblemException, InterruptedException {

		String sequencetoolPath = Settings.getInstance().getConfig().getGeneralConfig().getSequencetool();
		File out = NgsFileSystem.consensusFile(workDir, virusName);
		out.getParentFile().mkdirs();
		out.createNewFile();

		String cmd = sequencetoolPath + " make-consensus"
				+ " --input " + assembledContigs.getAbsolutePath()
				+ " --output " + out.getAbsolutePath()
				+ " --max-gap " + ngsModule.getConsensusToolMaxGap()
				+ " --max-missing " + ngsModule.getConsensusToolMaxMissing()
				+ " --min-count " + ngsModule.getConsensusToolMinCount();

		NgsFileSystem.executeCmd(cmd, workDir);

		return out;
	}
}
