package rega.genotype.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rega.genotype.ApplicationException;
import rega.genotype.ngs.NgsFileSystem;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.framework.widgets.ObjectListModel;

/**
 * Utility for samtools
 * See: http://samtools.sourceforge.net/ and http://biobits.org/samtools_primer.html
 * 
 * @author michael
 */
public class SamtoolsUtil {
	public enum RefType {Consensus, Refrence}

	public static File samFile(final ConsensusBucket bucket, File jobDir, RefType refType) {
		if (refType == RefType.Refrence)
			return NgsFileSystem.samRefFile(jobDir, 
					bucket.getDiamondBucket(), bucket.getRefName());
		else
			return NgsFileSystem.samConsensusFile(jobDir, 
					bucket.getDiamondBucket(), bucket.getRefName());
	}

	private static File consensusFile(final ConsensusBucket bucket, File jobDir,  RefType refType) {
		return refType == RefType.Refrence ?
				NgsFileSystem.consensusRefFile(jobDir, bucket.getDiamondBucket(), bucket.getRefName())
				: NgsFileSystem.consensusFile(jobDir, bucket.getDiamondBucket(), bucket.getRefName());
	}

	public static void bwaAlign(final ConsensusBucket bucket, File jobDir, RefType refType) throws ApplicationException {
		String bwaPath = Settings.getInstance().getConfig().getGeneralConfig().getBwaCmd();

		File consensusFile = consensusFile(bucket, jobDir, refType);
		File consensusDir = NgsFileSystem.consensusRefSeqDir(
				NgsFileSystem.consensusDir(jobDir, bucket.getDiamondBucket()),
				bucket.getRefName());

		// ./bwa index ref.fa
		String cmd = bwaPath + " index " + consensusFile.getAbsolutePath();
		System.err.println(cmd);
		Utils.executeCmd(cmd, consensusDir);
	}

	public static File createSamFilePE(final ConsensusBucket bucket, File jobDir, 
			RefType refType) throws ApplicationException {
		
		bwaAlign(bucket, jobDir, refType);

		String bwaPath = Settings.getInstance().getConfig().getGeneralConfig().getBwaCmd();
		File out = samFile(bucket, jobDir, refType);

		File consensusFile = consensusFile(bucket, jobDir, refType);

		File pe1 = NgsFileSystem.diamodPe1File(
				jobDir, bucket.getDiamondBucket());
		File pe2 = NgsFileSystem.diamodPe2File(
				jobDir, bucket.getDiamondBucket());

		// ./bwa mem ref.fa read1.fq read2.fq > aln-pe.sam.gz
		String cmd = bwaPath + " mem " + consensusFile.getAbsolutePath()
				+ " " + pe1.getAbsolutePath() + " " + pe2.getAbsolutePath() 
				+ " > " + out.getAbsolutePath(); 
		Utils.execShellCmd(cmd, consensusFile);

		return out;
	}

	public static File createSamFileSE(final ConsensusBucket bucket, File jobDir,
			RefType refType) throws ApplicationException {

		bwaAlign(bucket, jobDir, refType);

		String bwaPath = Settings.getInstance().getConfig().getGeneralConfig().getBwaCmd();
		File out = samFile(bucket, jobDir, refType);

		File consensusFile = consensusFile(bucket, jobDir, refType);

		File se = NgsFileSystem.diamodSeFile(
				jobDir, bucket.getDiamondBucket());

		// ./bwa mem ref.fa read1.fq read2.fq > aln-pe.sam.gz
		String cmd = bwaPath + " mem " + consensusFile.getAbsolutePath()
				+ " " + se.getAbsolutePath()
				+ " > " + out.getAbsolutePath(); 
		Utils.execShellCmd(cmd, consensusFile);

		return out;
	}

	public static Integer countReads(File sortedBamFile) throws ApplicationException {
		// samtools view -F 0x4 foo.sorted.bam | cut -f 1 | sort | uniq | wc -l
		String samtoolsCmd = Settings.getInstance().getConfig().getGeneralConfig().getSamtoolsCmd();
		String cmd = samtoolsCmd + " view -F 0x4 " + sortedBamFile.getAbsolutePath()
		+ " | cut -f 1 | sort | uniq | wc -l";

		try {
			return Integer.parseInt(Utils.execShellCmd(cmd));
		} catch (NumberFormatException e){
			e.printStackTrace();
			return null;
		}
	}

	public static void samToBam(File samFile, File out, File workDir) throws ApplicationException {
		// samtools view -b -S -o alignments/sim_reads_aligned.bam alignments/sim_reads_aligned.sam
		String samtoolsCmd = Settings.getInstance().getConfig().getGeneralConfig().getSamtoolsCmd();
		String cmd = samtoolsCmd + " view -b -S -o " + out.getAbsolutePath() + " " + samFile.getAbsolutePath();
		System.err.println(cmd);
		Utils.executeCmd(cmd, workDir);
	}

	public static void sortBamFile(File bamFile, File out, File workDir) throws ApplicationException {
		// samtools sort alignments/sim_reads_aligned.bam alignments/sim_reads_aligned.sorted
		String samtoolsCmd = Settings.getInstance().getConfig().getGeneralConfig().getSamtoolsCmd();
		String cmd = samtoolsCmd + " sort " + bamFile.getAbsolutePath() + " -o " + out.getAbsolutePath();
		System.err.println(cmd);
		Utils.execShellCmd(cmd, workDir);
	}

	public static void createCovMap(File bamFile, File out, File workDir) throws ApplicationException {
		// samtools depth deduped_MA605.bam > deduped_MA605.coverage
		String samtoolsCmd = Settings.getInstance().getConfig().getGeneralConfig().getSamtoolsCmd();
		String cmd = samtoolsCmd + " depth " + bamFile.getAbsolutePath() + " > " + out.getAbsolutePath();
		System.err.println(cmd);
		Utils.execShellCmd(cmd, workDir);
	}

	public static void samToCovMap(File samFile, File out, File consensusWorkDir, RefType refType) throws ApplicationException {
		File bamFile;
		File sortedBamFile;
		if (refType == RefType.Refrence) {
			bamFile = new File(consensusWorkDir, NgsFileSystem.ALINGMENT_REF_BAM_FILE);
			sortedBamFile = new File(consensusWorkDir, NgsFileSystem.ALINGMENT_REF_BAM_SORTED_FILE);
		} else {
			bamFile = new File(consensusWorkDir, NgsFileSystem.ALINGMENT_CONSENSUS_BAM_FILE);
			sortedBamFile = new File(consensusWorkDir, NgsFileSystem.ALINGMENT_CONSENSUS_BAM_SORTED_FILE);
		}
		samToBam(samFile, bamFile, consensusWorkDir);
		sortBamFile(bamFile, sortedBamFile, consensusWorkDir);
		createCovMap(sortedBamFile, out, consensusWorkDir);
	}

	public static ObjectListModel<Integer> covMapModel(File covMapFile, int consensusLength) {
		BufferedReader br = null;
		String line = "";
		List<Integer> ans = new ArrayList<Integer>();
		try {
			br = new BufferedReader(new FileReader(covMapFile));
			while ((line = br.readLine()) != null) {
				String[] split = line.split("\t");
				if (split.length == 3) {
					try {
						int pos = Integer.parseInt(split[1]);
						if (ans.size() < pos) { 
							// fill 0 cov area
							ans.addAll(Collections.nCopies(pos - ans.size(), 0));
						}
						int peCov = Integer.parseInt(split[2]);
						assert(peCov % 2 == 0); // Samtools does not know about pair ends.
						ans.add(peCov / 2);
					} catch (NumberFormatException e) {
						e.printStackTrace(); // should not get here!!
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (ans.size() < consensusLength) { 
			ans.addAll(Collections.nCopies(consensusLength - ans.size(), 0));
		}
		return new ObjectListModel<Integer>(ans) {
			@Override
			public Object render(Integer t) {
				return t;
			}
		};
	}
}
