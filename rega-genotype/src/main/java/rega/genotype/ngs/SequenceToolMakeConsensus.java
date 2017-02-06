package rega.genotype.ngs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import rega.genotype.AbstractSequence;
import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.NgsModule;
import rega.genotype.ngs.model.Contig;
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
			File consensusWorkDir, NgsModule ngsModule, Logger logger) throws ApplicationException, IOException, FileFormatException, ParameterProblemException, InterruptedException {
		// make Consensus

		String sequencetoolPath = Settings.getInstance().getConfig().getGeneralConfig().getSequencetool();
		String clustalPath = Settings.getInstance().getConfig().getGeneralConfig().getClustalWCmd();
		
		File alingment = NgsFileSystem.consensusAlingmentFile(consensusWorkDir);
		alingment.getParentFile().mkdirs();

		File referenceFile = NgsFileSystem.consensusRefFile(consensusWorkDir);
		FileOutputStream fos = new FileOutputStream(referenceFile);
		reference.writeFastaOutput(fos);
		fos.close();

		File consensusUnusedContigsFile = NgsFileSystem.consensusUnusedContigsFile(consensusWorkDir);
		File clastalWorkDir = new File(consensusWorkDir, "clustal-work-dir");
		clastalWorkDir.mkdirs();

		String cmd = sequencetoolPath + " consensus-align"
		+ " --reference " + referenceFile.getAbsolutePath()
		+ " --target " + assembledContigs.getAbsolutePath()
		+ " --output " + alingment.getAbsolutePath()
		+ " --absolute-cutoff " + ngsModule.getConsensusToolAbsoluteCutoff()
		+ " --relative-cutoff " + ngsModule.getConsensusToolRelativeCutoff()
		+ " --min-single-seq-cov " + ngsModule.getConsensusToolMinSingleSeqCov()
		+ " --export-unused " + consensusUnusedContigsFile.getAbsolutePath()
		+ " --clustalw-path " + clustalPath
		+ " --work-dir " + consensusWorkDir;

		NgsFileSystem.executeCmd(cmd, consensusWorkDir, logger);

		return alingment;
	}

	public static File makeConsensus(File assembledContigs,
			File consensusWorkDir, NgsModule ngsModule, Logger logger) throws ApplicationException, IOException, FileFormatException, ParameterProblemException, InterruptedException {

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

		return out;
	}

	public static List<Contig> readCotigsData(File consensusWorkDir) {
		List<Contig> ans = new ArrayList<Contig>();
		File contigsFile = NgsFileSystem.consensusContigsFile(consensusWorkDir);
		try {
			SequenceAlignment contigsAlignment = new SequenceAlignment(new FileInputStream(contigsFile),
					SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA, false);
			for (AbstractSequence s: contigsAlignment.getSequences()) {
				String[] parts = s.getName().split("_");
				int len = Integer.parseInt(parts[3]);
				double cov = Double.parseDouble(parts[5]);
				int startPosition = Integer.parseInt(parts[7]);
				int endPosition = Integer.parseInt(parts[9]);
				ans.add(new Contig(parts[1], len, startPosition, endPosition, cov, s.getSequence()));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ParameterProblemException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FileFormatException e) {
			e.printStackTrace();
		}
		return ans;
	}
}
