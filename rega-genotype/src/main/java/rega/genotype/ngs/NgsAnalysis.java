package rega.genotype.ngs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.usadellab.trimmomatic.TrimmomaticSE;

import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;
import rega.genotype.ngs.NgsProgress.State;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.StreamReaderRuntime;

public class NgsAnalysis {
	public static final String FASTQ_FILES_DIR = "fastq_files";
	public static final String QC_REPORT_DIR = "qc_report";
	public static final String QC_REPORT_AFTER_PREPROCESS_DIR = "qc_report";
	public static final String ASSEMBALED_CONTIGS_DIR = "assembaled_contigs";
	public static final String DIAMOND_BLAST_DIR = "diamond_blast";
	public static final String DIAMOND_RESULT_DIR = "diamond_result";

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

		File diamondDir = new File(workDir, NgsAnalysis.DIAMOND_BLAST_DIR);
		if (!(diamondDir.exists())){
			diamondDir.mkdirs();
		}
		File fastqMerge = null;
		if (fastqDir.listFiles().length > 1){
			try {
				fastqMerge = megerFiles(diamondDir, fastqDir.listFiles());
			} catch (IOException e) {
				e.printStackTrace();
				ngsProgress.setErrors("diamond files could not be merged.");
				ngsProgress.save(workDir);
				return false;
			} catch (FileFormatException e) {
				e.printStackTrace();
				ngsProgress.setErrors("diamond files could not be merged.");
				ngsProgress.save(workDir);
				return false;
			}
		} else {
			//continue;
		}

		File matches = null;
		File view = null;
		try {
			matches = diamondBlastX(diamondDir, fastqMerge, 50.0);
			view = diamondView(diamondDir, matches);
			File resultDiamondDir = new File(workDir, NgsAnalysis.DIAMOND_RESULT_DIR);
			if (!(resultDiamondDir.exists())){
				resultDiamondDir.mkdirs();
			}
			try {
				boolean x = creatFastqResultDiamond(resultDiamondDir, view, fastqDir.listFiles());
			} catch (FileFormatException e) {
				e.printStackTrace();
				ngsProgress.setErrors("diamond files could not be merged." + e.getMessage());
				ngsProgress.save(workDir);
				return false;
			}
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("blastX: " + e.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.DiamondFinished);

		// spades

		ngsProgress.save(workDir);
		return false; // TODO: change to true when ready.
	}

	private static File diamondBlastX(File workDir, File query, double limiteScore) throws ApplicationException {
		Process blastx = null;
		File matches = new File(workDir.getAbsolutePath() + File.separator + "matches.daa");
		try {
			String cmd = Settings.getInstance().getConfig().getGeneralConfig().getDiamondPath() + " blastx -d "
					+ Settings.getInstance().getConfig().getGeneralConfig().getDbDiamondPath() + " -q " + query.getAbsolutePath()
					+ " -a " + matches + " -k 1 --min-score "+ limiteScore +" --quiet";
			System.err.println(cmd);
			blastx = StreamReaderRuntime.exec(cmd, null, workDir);
			int exitResult = blastx.waitFor();
			
			if (exitResult != 0) {
				throw new ApplicationException("blastx exited with error: "
						+ exitResult);
			}
			return matches;
		} catch (FileNotFoundException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		} catch (IOException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		} catch (InterruptedException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		}
	}
	
	private static File diamondBlastP(File workDir, File query, double limiteScore) throws ApplicationException {
		Process blastx = null;
		File matches = new File(workDir.getAbsolutePath() + File.separator + "matches.daa");
		try {
			String cmd = Settings.getInstance().getConfig().getGeneralConfig().getDiamondPath() + " blastp -d "
					+ Settings.getInstance().getConfig().getGeneralConfig().getDbDiamondPath() + " -q " + query.getAbsolutePath()
					+ " -a " + matches + " -k 1 --min-score "+ limiteScore +" --quiet";
			System.err.println(cmd);
			blastx = StreamReaderRuntime.exec(cmd, null, workDir);
			int exitResult = blastx.waitFor();
			
			if (exitResult != 0) {
				throw new ApplicationException("blastx exited with error: "
						+ exitResult);
			}
			return matches;
		} catch (FileNotFoundException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		} catch (IOException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		} catch (InterruptedException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		}
	}
	
	private static File diamondView(File workDir, File query) throws ApplicationException {
		File matches = new File(workDir.getAbsolutePath() + File.separator + "matches.fasta");
		Process diamond = null;
		try {
			String cmd = Settings.getInstance().getConfig().getGeneralConfig().getDiamondPath() + " view -a " + query.getAbsolutePath()
					+ " -o " + matches +" --quiet";
			System.err.println(cmd);
			diamond = StreamReaderRuntime.exec(cmd, null, workDir);
			int exitResult = diamond.waitFor();
			
			if (exitResult != 0) {
				throw new ApplicationException("blastx exited with error: "
						+ exitResult);
			}
			return matches;
		} catch (IOException e) {
			if (diamond != null)
				diamond.destroy();
			throw new ApplicationException(
					": " + e.getMessage());
		} catch (InterruptedException e) {
			if (diamond != null)
				diamond.destroy();
			throw new ApplicationException(": "
					+ e.getMessage(), e);
		}
	}

	private static File megerFiles(File workDir, File[] listFiles) throws IOException, FileFormatException {
		NgsProgress ngsProgress = new NgsProgress();
		File result = new File(workDir.getAbsolutePath() + File.separator +"query.fna");
		if (listFiles[0].length() != listFiles[1].length()){
			ngsProgress.setErrors("Fastq files different");
			return null;
		}
		FileWriter megerFile = new FileWriter(result);
	    PrintWriter saveFile = new PrintWriter(megerFile);
		FileReader fr1 = new FileReader(listFiles[0].getAbsolutePath());
		LineNumberReader lnr1 = new LineNumberReader(fr1);
		FileReader fr2 = new FileReader(listFiles[1].getAbsolutePath());
		LineNumberReader lnr2 = new LineNumberReader(fr2);
		while (true){
			Sequence s1 = SequenceAlignment.readFastqFileSequence(lnr1, SequenceAlignment.SEQUENCE_DNA);
			Sequence s2 = SequenceAlignment.readFastqFileSequence(lnr2, SequenceAlignment.SEQUENCE_DNA);
			if (s1 == null || s2 == null){
				break;
			}
			String[] name1 = s1.getName().split(" ");
			String[] name2 = s2.getName().split(" ");
			if (name1[0].equalsIgnoreCase(name2[0])){
				String[] leng1 = name1[name1.length - 1].split("=");
				String[] length1 = leng1[1].split("/");
				String[] leng2 = name1[name1.length - 1].split("=");
				String[] length2 = leng2[1].split("/");
				int length = Integer.parseInt(length1[0]) + Integer.parseInt(length2[0]);
				saveFile.println("@"+name1[0] +" " + name1[1] + " length="+length);
				saveFile.println(s1.getSequence() + s2.getSequence());
				saveFile.println("+");
				saveFile.println(s1.getQuality() + s2.getQuality());
			}
		}
		megerFile.close();
		return result;
	}
	
	private static boolean creatFastqResultDiamond(File workDir, File view, File[] files) throws FileFormatException {
		String line = "";
		 try {
			BufferedReader  bf = new BufferedReader(new FileReader(view.getAbsolutePath()));
			Collection taxoId = new ArrayList();
			while ((line = bf.readLine()) != null)  {
				String[] name = line.split("\\t");
				String[] taxon = name[1].split("_");
				taxoId.add(taxon[0]);
			}
			line = "";
			Collection taxoIdOnly = new LinkedHashSet(taxoId);
			for (Iterator i =  taxoIdOnly.iterator(); i.hasNext();){
				String taxosId = i.next().toString();
				File taxonDir = new File(workDir, taxosId);
				if (!taxonDir.exists()){
					taxonDir.mkdirs();
				}
				BufferedReader txBf = new BufferedReader(new FileReader(view.getAbsolutePath()));
				Collection nameVirus = new ArrayList();
				FileWriter matches = new FileWriter(taxonDir.getAbsoluteFile() + File.separator + view.getName());
			    PrintWriter saveFile = new PrintWriter(matches);
				while ((line = txBf.readLine()) != null)  {
					String[] name = line.split("\\t");
					String[] taxon = name[1].split("_");
					if (taxosId.equalsIgnoreCase(taxon[0])){
						nameVirus.add(name[0]);
						saveFile.println(line);
					}
				}
				matches.close();
				Collection nameVirusOnly = new LinkedHashSet(nameVirus);
				for(File f : files){
					FileWriter fastq = new FileWriter(taxonDir.getAbsoluteFile() + File.separator + f.getName());
				    PrintWriter saveFastq = new PrintWriter(fastq);
					FileReader fr = new FileReader(f.getAbsolutePath());
					LineNumberReader lnr = new LineNumberReader(fr);
					while(true){
						Sequence s = SequenceAlignment.readFastqFileSequence(lnr, SequenceAlignment.SEQUENCE_DNA);
						if (s == null){
							break;
						}
						String[] name = s.getName().split(" ");
						if (nameVirusOnly.contains(name[0])){
							saveFastq.println("@" + s.getName());
							saveFastq.println(s.getSequence());
							saveFastq.println(s.getDescription());
							saveFastq.println(s.getQuality());
						}
					}
					fastq.close();
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
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
