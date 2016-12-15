package rega.genotype.ngs;

import java.io.File;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import rega.genotype.ApplicationException;
import rega.genotype.ngs.NgsProgress.State;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.LogUtils;
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

	public static final String QC_REPORT_DIR = "qc_report";
	public static final String QC_REPORT_AFTER_PREPROCESS_DIR = "qc2_report";
	public static final String ASSEMBALED_CONTIGS_DIR = "assembaled_contigs";
	public static final String DIAMOND_BLAST_DIR = "diamond_blast";
	public static final String DIAMOND_RESULT_DIR = "diamond_result";
	public static final String CONSENSUS_DIR = "consensus";

	public static final String PREPROCESSED_DIR = "preprocessed-fastq";
	public static final String PREPROCESSED_PE1_DIR = PREPROCESSED_DIR + File.separator + "PE1";
	public static final String PREPROCESSED_PE2_DIR = PREPROCESSED_DIR + File.separator + "PE2";
	
	public static final String PREPROCESSED_FILE_NAMR_UNPAIRD = "unpaird_";
	public static final String SEQUENCES_FILE = "sequences.fasta";

	public static final String CONSENSUS_CONTIGS_FILE = "consensus-contigs.fasta";
	public static final String CONSENSUS_FILE = "consensus.fasta";
	public static final String CONSENSUS_ALINGMENT_FILE = "consensus-alingemnt.fasta";
	public static final String CONSENSUS_REF_FILE = "consensus-ref.fasta";

	public static boolean addFastqFiles(File workDir, File fastqPE1, File fastqPE2) {
		return addFastqFiles(workDir, fastqPE1, fastqPE2, false);
	}

	public static boolean addFastqFiles(File workDir, File fastqPE1, File fastqPE2, boolean deleteOldFile) {
		if (!fastqPE1.exists() || !fastqPE2.exists()) 
    		return false;

		File fastqDir = new File(workDir, FASTQ_FILES_DIR);
		fastqDir.mkdirs();

		NgsProgress ngsProgress = new NgsProgress();

		//PE
		ngsProgress.setFastqPE1FileName(fastqPE1.getName());
		ngsProgress.setFastqPE2FileName(fastqPE2.getName());

		// SE TODO

		ngsProgress.setState(State.Init);
		ngsProgress.save(workDir);

		try {
			File fastqPE1Copy = new File(fastqDir, fastqPE1.getName());
			File fastqPE2Copy = new File(fastqDir, fastqPE2.getName());

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

	public static boolean downloadSrrFile(String srrName, File workDir) throws ApplicationException {
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
						fastqPE1 = new File(fastqDir, srrName + "_1.fastq");
						FileUtil.unGzip1File(f, fastqPE1);
					}
					if (f.getName().equals(srrName + "_2.fastq.gz")){
						fastqPE2 = new File(fastqDir, srrName + "_2.fastq");
						FileUtil.unGzip1File(f, fastqPE2);
					}
				}
		}

		// if not found download them
		if (fastqPE1 == null || fastqPE2 == null) {
			String srrToolKitPath = Settings.getInstance().getConfig().getGeneralConfig().getSrrToolKitPath();
			Logger logger = LogUtils.createLogger(workDir);
			String cmd = srrToolKitPath + "fastq-dump --split-files " + srrName;
			executeCmd(cmd, workDir, logger);

			fastqPE1 = new File(workDir, srrName + "_1.fastq");
			fastqPE2 = new File(workDir, srrName + "_2.fastq");
		}

		return addFastqFiles(workDir, fastqPE1, fastqPE2, true);
	}

	public static File fastqDir(File workDir) {
		NgsProgress ngsProgress = NgsProgress.read(workDir);

		File fastqDir = new File(workDir, FASTQ_FILES_DIR);
		if (!fastqDir.exists()) {
			ngsProgress.setState(State.Init);
			ngsProgress.setErrors("no fastq files");
			ngsProgress.save(workDir);
			return null;
		}

		return fastqDir;
	}

	public static File fastqSE(File workDir) {
		File fastqDir = fastqDir(workDir);
		if (fastqDir == null)
			return null;

		NgsProgress ngsProgress = NgsProgress.read(workDir);

		if (ngsProgress.getFastqSEFileName() != null) {
			File fastqSE = new File(fastqDir, ngsProgress.getFastqSEFileName());
			if (!fastqSE.exists()){
				ngsProgress.setErrors("FASTQ files could not be found.");
				ngsProgress.save(workDir);
				return null;
			}
			return fastqSE;
		} else
			return null;
	}

	public static File fastqPE1(File workDir) {
		File fastqDir = fastqDir(workDir);
		if (fastqDir == null)
			return null;

		NgsProgress ngsProgress = NgsProgress.read(workDir);

		if (ngsProgress.getFastqPE1FileName() != null 
				&& ngsProgress.getFastqPE2FileName() != null){
			File fastqPE1 = new File(fastqDir, ngsProgress.getFastqPE1FileName());
			if (!fastqPE1.exists()){
				ngsProgress.setErrors("FASTQ files could not be found.");
				ngsProgress.save(workDir);
				return null;
			}
			return fastqPE1;
		}

		return null;
	}

	public static File fastqPE2(File workDir) {
		File fastqDir = fastqDir(workDir);
		if (fastqDir == null)
			return null;

		NgsProgress ngsProgress = NgsProgress.read(workDir);

		if (ngsProgress.getFastqPE1FileName() != null 
				&& ngsProgress.getFastqPE2FileName() != null){
			File fastqPE2 = new File(fastqDir, ngsProgress.getFastqPE2FileName());
			if (!fastqPE2.exists()){
				ngsProgress.setErrors("FASTQ files could not be found.");
				ngsProgress.save(workDir);
				return null;
			}
			return fastqPE2;
		}

		return null;
	}

	public static File preprocessedDir(File workDir) {
		return new File(workDir, PREPROCESSED_DIR);
	}

	public static File preprocessedPE1Dir(File workDir) {
		File ans = new File(workDir, PREPROCESSED_PE1_DIR);
		if (!ans.exists())
			ans.mkdirs();
		return ans;
	}

	public static File preprocessedPE2Dir(File workDir) {
		File ans = new File(workDir, PREPROCESSED_PE2_DIR);
		if (!ans.exists())
			ans.mkdirs();
		return ans;
	}

	public static File createPreprocessedPE1(File workDir, String fileName) {
		return new File(preprocessedPE1Dir(workDir), fileName);
	}

	public static File createPreprocessedPE2(File workDir, String fileName) {
		return new File(preprocessedPE2Dir(workDir), fileName);
	}

	public static File preprocessedPE1(File workDir) {
		if (NgsProgress.read(workDir).getSkipPreprocessing())
			return fastqPE1(workDir);

		File preprocessedPE1Dir = preprocessedPE1Dir(workDir);
		if (preprocessedPE1Dir.listFiles().length != 1)
			return null;

		return preprocessedPE1Dir.listFiles()[0];
	}

	public static File preprocessedPE2(File workDir) {
		if (NgsProgress.read(workDir).getSkipPreprocessing())
			return fastqPE2(workDir);

		File preprocessedPE2Dir = preprocessedPE2Dir(workDir);
		if (preprocessedPE2Dir.listFiles().length != 1)
			return null;
		return preprocessedPE2Dir.listFiles()[0];
	}

	public static void executeCmd(String cmd, File workDir, Logger logger) throws ApplicationException{
		logger.info(cmd);
		Utils.executeCmd(cmd, workDir);
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
		return new File(virusDir, refseq);
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
}