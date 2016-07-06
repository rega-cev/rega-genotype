package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.usadellab.trimmomatic.TrimmomaticSE;

import rega.genotype.ApplicationException;
import rega.genotype.ngs.NgsProgress.State;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.StreamReaderRuntime;

public class NgsAnalysis {
	public static final String FASTQ_FILES_DIR = "fastq_files";
	public static final String QC_REPORT_DIR = "qc_report";
	public static final String QC_REPORT_AFTER_PREPROCESS_DIR = "qc_report";
	public static final String ASSEMBALED_CONTIGS_DIR = "assembaled_contigs";
	public static final String DIAMOND_BLAST_DIR = "diamond_blast";
	
	public static final String PREPROCESSED_DIR = "preprocessed-fastq";
	public static final String PREPROCESSED_FILE_NAMR_PAIRD = "paird_";
	public static final String PREPROCESSED_FILE_NAMR_UNPAIRD = "unpaird_";
	public static final String SEQUENCES_FILE= "sequences.fasta";


	/**
	 * Contract long virus contigs from ngs output. 
	 * Steps:QC (FastQC), pre-processing (Trimmomatic),
	 * primary search (Diamond blast), assembly (Spades)
	 * @param workDir
	 */
	public static boolean analyze(File workDir) {
		NgsProgress ngsProgress = NgsProgress.read(workDir);

		// read fastq

		File fastqDir = new File(workDir, NgsAnalysis.FASTQ_FILES_DIR);
		if (!fastqDir.exists()) {
			ngsProgress.setState(State.UploadStarted);
			ngsProgress.setErrors("no fastq files");
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.FastQ_File_Uploaded);

		File fastqPE1 = null;
		File fastqPE2 = null;
		File fastqSE = null;
		
		if (ngsProgress.getFastqSEFileName() != null) {
			fastqSE = new File(fastqDir, ngsProgress.getFastqSEFileName());
			if (!fastqSE.exists()){
				ngsProgress.setErrors("FASTQ files could not be found.");
				ngsProgress.save(workDir);
				return false;
			}	
		} else if (ngsProgress.getFastqPE1FileName() != null 
				&& ngsProgress.getFastqPE2FileName() != null){
			fastqPE1 = new File(fastqDir, ngsProgress.getFastqPE1FileName());
			fastqPE2 = new File(fastqDir, ngsProgress.getFastqPE2FileName());
			if (!fastqPE1.exists() || !fastqPE2.exists()){
				ngsProgress.setErrors("FASTQ files could not be found.");
				ngsProgress.save(workDir);
				return false;
			}	
		}

		// QC

		try {
				NgsAnalysis.qcReport(fastqDir.listFiles(), workDir);
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("QC failed: " + e.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.QcFinished);

		// pre-process

		try {
			if (fastqSE != null) {
				// TODO
			} else {
			preprocessFastQ(fastqPE1, fastqPE2, workDir);
			}
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("pre-process of fastq file failed: " + e.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.PreprocessingFinished);

		// QC 2
		

		// diamond blast

		// spades

		ngsProgress.save(workDir);
		return false; // TODO: change to true when ready.
	}

	/**
	 * Use FastQC to generate quality control reports
	 * @param sequenceFiles
	 * @param workDir
	 * @return a list of result html files.
	 * @throws ApplicationException
	 */
	public static List<File> qcReport(File[] sequenceFiles, File workDir) throws ApplicationException {		
		File reportDir = new File(workDir, QC_REPORT_DIR);
		reportDir.mkdirs();

		String fastQCcmd = Settings.getInstance().getConfig().getGeneralConfig().getFastqcCmd();
		String cmd = fastQCcmd;
		for (File f: sequenceFiles) 
			cmd += " " + f.getAbsolutePath();

		cmd += " -outdir " + reportDir.getAbsolutePath();

		System.err.println(cmd);
		Process p = null;

		try {
			p = StreamReaderRuntime.exec(cmd, null, workDir.getAbsoluteFile());
			int exitResult = p.waitFor();

			if (exitResult != 0) {
				throw new ApplicationException("QC exited with error: " + exitResult);
			}
		} catch (IOException e) {
			throw new ApplicationException("QC failed error: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			if (p != null)
				p.destroy();
			throw new ApplicationException("QC failed error: " + e.getMessage(), e);
		}

		List<File> ans = new ArrayList<File>();

		for(File reportFile: reportDir.listFiles()) {
			if (FilenameUtils.getExtension(reportFile.getAbsolutePath()).equals("html")){
				if (!reportFile.exists())
					throw new ApplicationException("QC failed error: " + reportFile.getName() + " was not created.");
				ans.add(reportFile);
			}
		}

		return ans;
	}

	// TODO
	/**
	 * preprocess fastq file with trimmomatic.
	 * args = ILLUMINACLIP:adapters.fasta:2:10:7:1 LEADING:10 TRAILING:10  MINLEN:5
	 * @param sequenceFile
	 * @param workDir
	 * @return preprocessed fastq file
	 * @throws ApplicationException
	 */
	public static File preprocessFastQ(File sequenceFile, File workDir) throws ApplicationException {
		//time java -Xmx1000m -jar ./trimmomatic-0.36.jar SE
		// ../sratoolkit.2.6.3-ubuntu64/bin/SRR1106548_1.fastq  out.fq 
		// ILLUMINACLIP:./adapters.fasta:2:10:7:1 LEADING:10 TRAILING:10  MINLEN:5

		File out = new File(workDir, "preprocessed.fq");
		String[] args = {out.getAbsolutePath(),
				"ILLUMINACLIP:./adapters.fasta:2:10:7:1", // TODO: add adapters.fasta
				"LEADING:10", "TRAILING:10", "MINLEN:5"};
		try {
			TrimmomaticSE.run(args);
		} catch (IOException e) {
			throw new ApplicationException("Preprocessing failed error: " + e.getMessage(), e);
		}

		return out;
	}
	
	/**
	 * preprocess fastq file with trimmomatic.
	 * args = ILLUMINACLIP:adapters.fasta:2:10:7:1 LEADING:10 TRAILING:10  MINLEN:5
	 * @param sequenceFile
	 * @param workDir
	 * @return preprocessed fastq file
	 * @throws ApplicationException
	 */
	public static List<File> preprocessFastQ(File sequenceFile1, File sequenceFile2, File workDir) throws ApplicationException {
		/*
		 * java -Xmx1000m -jar /home/koen/project/virogenesis/iva/Trimmomatic-0.36/trimmomatic-0.36.jar 
		 * PE -threads 1 ../sratoolkit.2.6.3-ubuntu64/bin/SRR1106548_1.fastq 
		 * ../sratoolkit.2.6.3-ubuntu64/bin/SRR1106548_2.fastq
		 * reads.trimmed99_1.fq 
		 * reads.trimmed.unpaired99_1.fq 
		 * reads.trimmed99_2.fq 
		 * reads.trimmed.unpaired99_2.fq 
		 * LEADING:10 TRAILING:10 SLIDINGWINDOW:4:20 MINLEN:50
		 */
		//TODO
		String trimmomaticCmd = "java -Xmx1000m -jar /home/koen/project/virogenesis/iva/Trimmomatic-0.36/trimmomatic-0.36.jar PE -threads 1 ";
		String trimmomaticOptions = " LEADING:10 TRAILING:10 SLIDINGWINDOW:4:20 MINLEN:50";

		String inputFileNames = sequenceFile1.getAbsolutePath() + " " + sequenceFile2.getAbsolutePath();
		
		File preprocessedDir = new File(workDir, PREPROCESSED_DIR);
		preprocessedDir.mkdirs();
		File paired1 = new File(preprocessedDir, PREPROCESSED_FILE_NAMR_PAIRD + sequenceFile1.getName());
		File paired2 = new File(preprocessedDir, PREPROCESSED_FILE_NAMR_PAIRD + sequenceFile2.getName());
		File unpaired1 = new File(preprocessedDir, PREPROCESSED_FILE_NAMR_UNPAIRD + sequenceFile1.getName());
		File unpaired2 = new File(preprocessedDir, PREPROCESSED_FILE_NAMR_UNPAIRD + sequenceFile2.getName());

		String outoutFileNames = paired1.getAbsolutePath()
				+ " " + unpaired1 .getAbsolutePath()
				+ " " + paired2.getAbsolutePath()
				+ " " + unpaired2.getAbsolutePath();

		String cmd = trimmomaticCmd + " " + inputFileNames + " " + outoutFileNames + " " + trimmomaticOptions;

		System.err.println(cmd);
		Process p = null;

		try {
			p = StreamReaderRuntime.exec(cmd, null, workDir.getAbsoluteFile());
			int exitResult = p.waitFor();

			if (exitResult != 0) {
				throw new ApplicationException("preprocessing exited with error: " + exitResult);
			}
		} catch (IOException e) {
			throw new ApplicationException("preprocessing failed error: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			if (p != null)
				p.destroy();
			throw new ApplicationException("preprocessing failed error: " + e.getMessage(), e);
		}

		List<File> ans = new ArrayList<File>();
		ans.add(paired1);
		ans.add(paired2);
		ans.add(unpaired1);
		ans.add(unpaired2);
		return ans;
	}
	
	/**
	 * Diamond blast will remove non virus reads and separate the virus
	 * reads to different files.
	 * The result files will be saved in DIAMOND_BLAST_DIR
	 * 
	 * @param sequenceFile1 File with forward reads.
	 * @param sequenceFile2 File with reverse reads.
	 * @param workDir
	 * @return
	 * @throws ApplicationException
	 */
	public static List<File> runDiamondBlast(File sequenceFile1, File sequenceFile2,
			File workDir) throws ApplicationException {		
		File diamondDir = new File(workDir, DIAMOND_BLAST_DIR);

		return null; //TODO
	}

	public static String contigsDir(String virusName) {
		return ASSEMBALED_CONTIGS_DIR + "_" + virusName;
	}

	/**
	 * Assemble many shot reads fastq to long contigs.
	 * 
	 * @param sequenceFile1 File with forward reads.
	 * @param sequenceFile2 File with reverse reads.
	 * @param workDir
	 * @param virusName as defined by diamond blast
	 * @return the contig file
	 * @throws ApplicationException
	 */
	public static File assemble(File sequenceFile1, File sequenceFile2,
			File workDir, String virusName) throws ApplicationException {		
		File contigsDir = new File(workDir, contigsDir(virusName));
		contigsDir.mkdirs();

		String cmd = Settings.getInstance().getConfig().getGeneralConfig().getSpadesCmd();
		// TODO: now only pair-ends
		cmd += " -1 " + sequenceFile1.getAbsolutePath();
		cmd += " -2 " + sequenceFile2.getAbsolutePath();

		cmd += " -o " + contigsDir.getAbsolutePath();

		System.err.println(cmd);
		Process p = null;

		try {
			p = StreamReaderRuntime.exec(cmd, null, workDir.getAbsoluteFile());
			int exitResult = p.waitFor();

			if (exitResult != 0) {
				throw new ApplicationException("Spades exited with error: " + exitResult);
			}
		} catch (IOException e) {
			throw new ApplicationException("Spades failed error: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			if (p != null)
				p.destroy();
			throw new ApplicationException("Spades failed error: " + e.getMessage(), e);
		}

		File ans = new File(contigsDir, "contigs.fasta");
		if (!ans.exists())
			throw new ApplicationException("Spades did not create contigs file.");

		return ans;
	}
}
