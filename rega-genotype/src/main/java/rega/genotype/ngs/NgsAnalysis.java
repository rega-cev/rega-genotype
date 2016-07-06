package rega.genotype.ngs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.usadellab.trimmomatic.TrimmomaticSE;

import rega.genotype.AbstractSequence;
import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;
import rega.genotype.AbstractAnalysis.Result;
import rega.genotype.ngs.NgsProgress.State;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.StreamReaderRuntime;
import sun.security.util.Length;

public class NgsAnalysis {
	public static final String FASTQ_FILES_DIR = "fastq_files";
	public static final String QC_REPORT_DIR = "qc_report";
	public static final String ASSEMBALED_CONTIGS_DIR = "assembaled_contigs";
	public static final String DIAMOND_BLAST_DIR = "diamond_blast";
	public static final String DIAMOND_RESULT_DIR = "diamond_result";

	/**
	 * Contract long virus contigs from ngs output. 
	 * Steps:QC (FastQC), pre-processing (Trimmomatic),
	 * primary search (Diamond blast), assembly (Spades)
	 * @param workDir
	 * @throws FileFormatException 
	 * @throws IOException 
	 * @throws ApplicationException 
	 */
	public static boolean analyze(File workDir) throws IOException, FileFormatException {
		NgsProgress ngsProgress = new NgsProgress();

		// read fastq

		File fastqDir = new File(workDir, NgsAnalysis.FASTQ_FILES_DIR);
		if (!fastqDir.exists()) {
			ngsProgress.setState(State.UploadStarted);
			ngsProgress.setErrors("no fastq files");
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.FastQ_File_Uploaded);

		// QC
		/**
		try {
			NgsAnalysis.qcReport(fastqDir.listFiles(), workDir);
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("QC failed: " + e.getMessage());
			ngsProgress.save(workDir);
			return false;
		}
		/**/
		ngsProgress.setState(State.QcFinished);

		// pre-process

		// QC 2
		
		
		// diamond blast
		File diamondDir = new File(workDir, NgsAnalysis.DIAMOND_BLAST_DIR);
		if (!(diamondDir.exists())){
			diamondDir.mkdirs();
		}
		File fastqMerge = null;
		if (fastqDir.listFiles().length > 1){
			fastqMerge = megerFiles(diamondDir, fastqDir.listFiles());
		}else{
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
			boolean x = creatFastqResultDiamond(resultDiamondDir, view, fastqDir.listFiles());
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("blastX: " + e.getMessage());
			ngsProgress.save(workDir);
			return false;
		}
		
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
							saveFastq.println(s.getName());
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
