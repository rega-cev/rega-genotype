package rega.genotype.ngs;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import rega.genotype.ApplicationException;
import rega.genotype.ngs.model.NgsResultsModel.State;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Utils;

/**
 * All the Files that are used by this job are saved in the job dir of this job 
 * you can find it in path/to/base-work-dir/job/{tool-id}{tool-version}/{job-number}/
 * 
 * fastq files: are stored in FASTQ_FILES_DIR
 * QC: results are stored in QC_REPORT_DIR
 * preprocessed: fastq stored in PREPROCESSED_PE1_DIR and PREPROCESSED_PE2_DIR
 * primary search: result are stored in DIAMOND_RESULT_DIR the folder contains a folder for every 
 *   detected virus, this folder contains all the reads for that virus.
 * Assembly: ASSEMBALED_CONTIGS_DIR contains a folder ({virus taxonomy_id}_{virus name})for every 
 *   detected virus from the primary (we do not try to assemble if there is a primary search found 
 *   very low amount of sequences)
 * Consensus: CONSENSUS_DIR contains a folder ({virus taxonomy_id}_{virus name})for every 
 *   detected virus (same as assembly)
 * 
 * @author michael
 */
public class NgsFileSystem {
	public static final String FASTQ_FILES_DIR = "fastq_files";

	public static final String PE1 = "PE1.fastq";
	public static final String PE2 = "PE2.fastq";
	public static final String SE = "SE.fastq";

	public static final String QC_PE1 = "PE1_fastqc.html";
	public static final String QC_PE2 = "PE2_fastqc.html";
	public static final String QC_SE = "SE_fastqc.html";

	public static final String QC_REPORT_DIR = "qc_report";
	public static final String QC_REPORT_AFTER_PREPROCESS_DIR = "qc2_report";
	public static final String ASSEMBALED_CONTIGS_DIR = "assembaled_contigs";
	public static final String DIAMOND_BLAST_DIR = "diamond_blast";
	public static final String DIAMOND_RESULT_DIR = "diamond_result";
	public static final String CONSENSUS_DIR = "consensus";

	public static final String PREPROCESSED_DIR = "preprocessed-fastq";

	public static final String PREPROCESSED_FILE_NAMR_UNPAIRD = "unpaird_";
	public static final String CONTIGS_FILE = "contigs.fasta"; // contigs will be passed to sub typing tool
	public static final String CONSENSUSES_FILE = "consensuses.fasta";

	public static final String CONSENSUS_CONTIGS_FILE = "consensus-contigs.fasta";
	public static final String CONSENSUS_FILE = "consensus.fasta";
	public static final String CONSENSUS_ALINGMENT_FILE = "consensus-alignment.fasta";
	public static final String CONSENSUS_REF_FILE = "consensus-ref.fasta";
	public static final String CONSENSUS_UNUSED_CONTIGS_FILE = "consensus-unused-contigs.fasta";
	public static final String ALINGMENT_CONSENSUS_SAM_FILE = "consensus-alignment.sam";
	public static final String ALINGMENT_CONSENSUS_BAM_FILE = "consensus-alignment.bam";
	public static final String ALINGMENT_CONSENSUS_BAM_SORTED_FILE = "consensus-alignment-sorted.bam";
	public static final String ALINGMENT_CONSENSUS_COV_FILE = "consensus-alignment.cov";
	public static final String ALINGMENT_REF_SAM_FILE = "ref-alignment.sam";
	public static final String ALINGMENT_REF_BAM_FILE = "ref-alignment.bam";
	public static final String ALINGMENT_REF_BAM_SORTED_FILE = "ref-alignment-sorted.bam";
	public static final String ALINGMENT_REF_COV_FILE = "ref-alignment.cov";

	public static boolean addFastqFiles(File workDir, File fastqPE1, File fastqPE2) {
		return addFastqFiles(workDir, fastqPE1, fastqPE2, false);
	}

	public static boolean addFastqFiles(File workDir,
			File fastqPE1, File fastqPE2,
			boolean deleteOldFile) {
		if (!fastqPE1.exists() || !fastqPE2.exists()) 
    		return false;

		File fastqDir = new File(workDir, FASTQ_FILES_DIR);
		fastqDir.mkdirs();

		try {
			File fastqPE1Copy = new File(fastqDir, PE1);
			File fastqPE2Copy = new File(fastqDir, PE2);

			if (!fastqPE1.getAbsolutePath().equals(fastqPE1Copy.getAbsolutePath()))
				if (deleteOldFile)
					FileUtils.moveFile(fastqPE1, fastqPE1Copy);
				else
					FileUtils.copyFile(fastqPE1, fastqPE1Copy);
			if (!fastqPE2.getAbsolutePath().equals(fastqPE2Copy.getAbsolutePath()))
				if (deleteOldFile)
					FileUtils.moveFile(fastqPE2, fastqPE2Copy);
				else
					FileUtils.copyFile(fastqPE2, fastqPE2Copy);
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}

		return true;
	}
	public static boolean addFastqSE(File workDir, File fastqSE) {
		return addFastqSE(workDir, fastqSE, false);
	}

	public static boolean addFastqSE(File workDir, File fastqSE, boolean deleteOldFile) {
		if (!fastqSE.exists()) 
    		return false;

		File fastqDir = new File(workDir, FASTQ_FILES_DIR);
		fastqDir.mkdirs();

		try {
			File fastqSECopy = new File(fastqDir, SE);

			if (!fastqSE.getAbsolutePath().equals(fastqSECopy.getAbsolutePath()))
				if (deleteOldFile)
					FileUtils.moveFile(fastqSE, fastqSECopy);
				else
					FileUtils.copyFile(fastqSE, fastqSECopy);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public static enum DownloadSrrState {
		Init("Searching for srr files"),
		Local1("Paired end reads 1 found on local server, extracting."),
		Local2("Paired end reads 2 found on local server, extracting."),
		Download1("Download Paired end reads 1, this can take some time."),
		Download2("Download Paired end reads 2, this can take some time."),
		Unzip1("Unzip Paired end reads 1."),
		Unzip2("Unzip Paired end reads 2."),
		Finished("Finished"),
		Failed("Failed, Could not download.");

		DownloadSrrState(String msg) {
			this.msg = msg;
		}
		public String msg(){
			return msg;
		}
		public String err() {
			return err;
		}
		public void setErr(String err) {
			this.err = err;
		}

		String msg = "";
		String err = "";
	}

	/**
	 * Allows to see the current state.
	 */
	public static class DownloadSrrStateTracer {
		public DownloadSrrStateTracer(){}
		public DownloadSrrState state = DownloadSrrState.Init;
	}
	
	public static boolean downloadSrrFile(File workDir, String srrName,
			DownloadSrrStateTracer downloadSrrStateTracer) throws ApplicationException {
		String srrDatabase = Settings.getInstance().getConfig().getGeneralConfig().getSrrDatabasePath();
		File fastqPE1 = null;
		File fastqPE2 = null;
		// find the file in local db
		if (!srrDatabase.isEmpty()) {
			File fastqDir = new File(workDir, FASTQ_FILES_DIR);
			fastqDir.mkdirs();
			File srrDatabaseDir = new File(srrDatabase);
			if (srrDatabaseDir.listFiles() != null)
				for (File f: srrDatabaseDir.listFiles()){
					if (f.getName().equals(srrName + "_1.fastq.gz")){
						downloadSrrStateTracer.state = DownloadSrrState.Local1;
						fastqPE1 = new File(fastqDir, srrName + "_1.fastq");
						FileUtil.unGzip1File(f, fastqPE1);
					}
					if (f.getName().equals(srrName + "_2.fastq.gz")){
						downloadSrrStateTracer.state = DownloadSrrState.Local2;
						fastqPE2 = new File(fastqDir, srrName + "_2.fastq");
						FileUtil.unGzip1File(f, fastqPE2);
					}
				}
		}

		// if not found download them
		if (fastqPE1 == null || fastqPE2 == null) {
			fastqPE1 = new File(workDir, srrName + "_1.fastq");
			fastqPE2 = new File(workDir, srrName + "_2.fastq");
			downloadSrr(srrName, workDir, fastqPE1, fastqPE2, downloadSrrStateTracer);
		}

		return addFastqFiles(workDir, fastqPE1, fastqPE2, true);
	}

	public static void downloadSrr(String srrAccessionNumber, File workDir,
			File fastqPE1, File fastqPE2, DownloadSrrStateTracer downloadSrrStateTracer) throws ApplicationException {
		// wget --output-document=$i.ftp.txt "http://www.ebi.ac.uk/ena/data/warehouse/filereport?accession=$i&result=read_run&fields=fastq_ftp"
		String url = "http://www.ebi.ac.uk/ena/data/warehouse/filereport?accession=" 
		+ srrAccessionNumber
		+ "&result=read_run&fields=fastq_ftp";

		String ftpTxt = Utils.wget(url);

		if (ftpTxt == null)
			throw new ApplicationException("Failed to download srr file " + srrAccessionNumber);

		System.err.println(ftpTxt);
		
		ftpTxt = ftpTxt.replace("fastq_ftp", "");
		String[] ftpTxtSplit = ftpTxt.split(";");
		if (ftpTxtSplit.length != 2)
			throw new ApplicationException("Failed to download srr file " + srrAccessionNumber);

		String pe1Url = "http://" +ftpTxtSplit[0];
		String pe2Url = "http://" + ftpTxtSplit[1];

		File pe1Compressed = new File(workDir, srrAccessionNumber + "_1.gz");
		File pe2Compressed = new File(workDir, srrAccessionNumber + "_2.gz");

		downloadSrrStateTracer.state = DownloadSrrState.Download1;
		if (!Utils.wget(pe1Url, pe1Compressed))
			throw new ApplicationException("Failed to download srr file " + srrAccessionNumber);

		downloadSrrStateTracer.state = DownloadSrrState.Download2;
		if (!Utils.wget(pe2Url, pe2Compressed))
			throw new ApplicationException("Failed to download srr file " + srrAccessionNumber);
	
		downloadSrrStateTracer.state = DownloadSrrState.Unzip1;
		FileUtil.unGzip1File(pe1Compressed, fastqPE1);
		downloadSrrStateTracer.state = DownloadSrrState.Unzip2;
		FileUtil.unGzip1File(pe2Compressed, fastqPE2);
	}
	
	public static File fastqDir(NgsResultsTracer ngsProgress) {
		File fastqDir = new File(ngsProgress.getWorkDir(), FASTQ_FILES_DIR);
		if (!fastqDir.exists()) {
			ngsProgress.setStateStart(State.Init);
			ngsProgress.printFatalError("no fastq files");
			return null;
		}

		return fastqDir;
	}
	public static File fastqDir(File workDir) {
		return new File(workDir, FASTQ_FILES_DIR);
	}

	public static File fastqSE(File jobDir) {
		return new File(jobDir, FASTQ_FILES_DIR + File.separator + SE);
	}

	public static File fastqPE1(File jobDir) {
		return new File(jobDir, FASTQ_FILES_DIR + File.separator + PE1);
	}

	public static File fastqPE2(File jobDir) {
		return new File(jobDir, FASTQ_FILES_DIR + File.separator + PE2);
	}

	public static File fastqSE(NgsResultsTracer ngsProgress) {
		File fastqDir = fastqDir(ngsProgress);
		if (fastqDir == null)
			return null;

		if (ngsProgress.getModel().getFastqSEFileName() != null) {
			File fastqSE = new File(fastqDir, SE);
			if (!fastqSE.exists()){
				ngsProgress.printFatalError("FASTQ files could not be found.");
				return null;
			}
			return fastqSE;
		} else
			return null;
	}

	public static File fastqPE1(NgsResultsTracer ngsResults) {
		File fastqDir = fastqDir(ngsResults);
		if (fastqDir == null)
			return null;

		if (ngsResults.getModel().getFastqPE1FileName() != null 
				&& ngsResults.getModel().getFastqPE2FileName() != null){
			File fastqPE1 = new File(fastqDir, PE1);
			if (!fastqPE1.exists()){
				ngsResults.printFatalError("FASTQ files could not be found.");
				return null;
			}
			return fastqPE1;
		}

		return null;
	}

	public static File fastqPE2(NgsResultsTracer ngsResults) {
		File fastqDir = fastqDir(ngsResults);
		if (fastqDir == null)
			return null;

		if (ngsResults.getModel().getFastqPE1FileName() != null 
				&& ngsResults.getModel().getFastqPE2FileName() != null){
			File fastqPE2 = new File(fastqDir, PE2);
			if (!fastqPE2.exists()){
				ngsResults.printFatalError("FASTQ files could not be found.");
				return null;
			}
			return fastqPE2;
		}

		return null;
	}

	public static File preprocessedDir(File workDir) {
		return new File(workDir, PREPROCESSED_DIR);
	}

	public static File createPreprocessedPE1(File workDir) {
		return new File(preprocessedDir(workDir), PE1);
	}

	public static File createPreprocessedPE2(File workDir) {
		return new File(preprocessedDir(workDir), PE2);
	}

	public static File createPreprocessedSE(File workDir) {
		return new File(preprocessedDir(workDir), SE);
	}

	public static File preprocessedPE1(File workDir) {
		return new File(workDir, PREPROCESSED_DIR + File.separator + PE1);
	}

	public static File preprocessedPE2(File workDir) {
		return new File(workDir, PREPROCESSED_DIR + File.separator + PE2);
	}

	public static File preprocessedSE(File workDir) {
		return new File(workDir, PREPROCESSED_DIR + File.separator + SE);

	}

	public static void executeCmd(String cmd, File workDir, Logger logger) throws ApplicationException{
		logger.info(cmd);
		Utils.executeCmd(cmd, workDir);
	}

	public static File qcPE1File(File qcDir) {
		return new File(qcDir, QC_PE1);
	}

	public static File qcPE2File(File qcDir) {
		return new File(qcDir, QC_PE2);
	}

	public static File qcSEFile(File qcDir) {
		return new File(qcDir, QC_SE);
	}

	public static File diamondResutlsDir(File jobDir) {
		return new File(jobDir, DIAMOND_RESULT_DIR);
	}

	public static File diamodBucketDir(File jobDir, String diamondBucketName) {
		return new File(diamondResutlsDir(jobDir), diamondBucketName);
	}

	public static File diamodPeFile(File jobDir, String diamondBucketName, String peFileName) {			
		return new File(diamodBucketDir(jobDir, diamondBucketName), peFileName);
	}

	public static File diamodPe1File(File jobDir, String diamondBucketName) {			
		return new File(diamodBucketDir(jobDir, diamondBucketName), PE1);
	}

	public static File diamodPe2File(File jobDir, String diamondBucketName) {			
		return new File(diamodBucketDir(jobDir, diamondBucketName), PE2);
	}

	public static File diamodSeFile(File jobDir, String diamondBucketName) {			
		return new File(diamodBucketDir(jobDir, diamondBucketName), SE);
	}

	public static String contigsDir(String virusName) {
		return ASSEMBALED_CONTIGS_DIR + File.separator + virusName;
	}

	public static File consensusDir(File workDir, String virusName) {
		return new File(new File(workDir, CONSENSUS_DIR), virusName) ;
	}

	public static File consensusContigDir(File virusDir, String contig) {
		return new File(virusDir, contig);
	}

	public static File consensusRefSeqDir(File virusDir, String refseq) {
		return new File(virusDir, refseq.replaceAll("\\|", "_"));
	}

	public static File consensusUnusedContigsFile(File workDir) {
		return new File(workDir, CONSENSUS_UNUSED_CONTIGS_FILE);
	}

	public static File consensusContigsFile(File workDir) {
		return new File(workDir, CONSENSUS_CONTIGS_FILE);
	}
	
	public static File consensusFile(File workDir) {
		return new File(workDir, CONSENSUS_FILE);
	}

	public static File consensusAlingmentFile(File workDir) {
		return new File(workDir, CONSENSUS_ALINGMENT_FILE);
	}

	public static File consensusRefFile(File workDir) {
		return new File(workDir, CONSENSUS_REF_FILE);
	}
	public static File consensusRefFile(File jobDir, String diamondBucket, String refName) {
		File consensusDir = consensusDir(jobDir, diamondBucket);
		File consensusRefSeqDir = consensusRefSeqDir(consensusDir, refName);
		return new File(consensusRefSeqDir, CONSENSUS_REF_FILE);
	}
	public static File consensusRefDir(File jobDir, String diamondBucket, String refName) {
		File consensusDir = consensusDir(jobDir, diamondBucket);
		return new File(consensusDir, refName.replaceAll("\\|", "_"));
	}
	public static File consensusFile(File jobDir, String diamondBucket, String refName) {
		File consensusRefSeqDir = consensusRefDir(jobDir, diamondBucket, refName);
		return new File(consensusRefSeqDir, CONSENSUS_FILE);
	}

	public static File samConsensusFile(File jobDir, String diamondBucket, String refName) {
		File consensusRefSeqDir = consensusRefDir(jobDir, diamondBucket, refName);
		return new File(consensusRefSeqDir, ALINGMENT_CONSENSUS_SAM_FILE);
	}
	public static File bamConsensusFile(File jobDir, String diamondBucket, String refName) {
		File consensusRefSeqDir = consensusRefDir(jobDir, diamondBucket, refName);
		return new File(consensusRefSeqDir, ALINGMENT_CONSENSUS_BAM_FILE);
	}
	public static File bamConsensusSortedFile(File jobDir, String diamondBucket, String refName) {
		File consensusRefSeqDir = consensusRefDir(jobDir, diamondBucket, refName);
		return new File(consensusRefSeqDir, ALINGMENT_CONSENSUS_BAM_SORTED_FILE);
	}
	public static File samRefFile(File jobDir, String diamondBucket, String refName) {
		File consensusRefSeqDir = consensusRefDir(jobDir, diamondBucket, refName);
		return new File(consensusRefSeqDir, ALINGMENT_REF_SAM_FILE);
	}
	public static File bamRefFile(File jobDir, String diamondBucket, String refName) {
		File consensusRefSeqDir = consensusRefDir(jobDir, diamondBucket, refName);
		return new File(consensusRefSeqDir, ALINGMENT_REF_BAM_FILE);
	}
	public static File bamRefSortedFile(File jobDir, String diamondBucket, String refName) {
		File consensusRefSeqDir = consensusRefDir(jobDir, diamondBucket, refName);
		return new File(consensusRefSeqDir, ALINGMENT_REF_BAM_SORTED_FILE);
	}
}