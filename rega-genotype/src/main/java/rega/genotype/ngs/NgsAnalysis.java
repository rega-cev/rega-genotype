package rega.genotype.ngs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.Config.GeneralConfig;
import rega.genotype.ngs.NgsProgress.State;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.StreamReaderRuntime;

/**
 * Contract long virus contigs from ngs output. 
 * Steps:QC (FastQC), pre-processing (Trimmomatic),
 * primary search (Diamond blast), assembly (Spades) 
 * 
 * @author michael and vagner
 */
public class NgsAnalysis {
	public static final String FASTQ_FILES_DIR = "fastq_files";
	public static final String QC_REPORT_DIR = "qc_report";
	public static final String QC_REPORT_AFTER_PREPROCESS_DIR = "qc2_report";
	public static final String ASSEMBALED_CONTIGS_DIR = "assembaled_contigs";
	public static final String DIAMOND_BLAST_DIR = "diamond_blast";
	public static final String DIAMOND_RESULT_DIR = "diamond_result";

	public static final String PREPROCESSED_DIR = "preprocessed-fastq";
	public static final String PREPROCESSED_FILE_NAMR_PAIRD = "paird_";
	public static final String PREPROCESSED_FILE_NAMR_UNPAIRD = "unpaird_";
	public static final String SEQUENCES_FILE = "sequences.fasta";
	public static final String CUT_ADAPTER_FILE_END = "CA_";


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
			ngsProgress.setState(State.Uploading);
			ngsProgress.setErrors("no fastq files");
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.QC);
		ngsProgress.save(workDir);

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
			NgsAnalysis.qcReport(fastqDir.listFiles(),
					new File(workDir, QC_REPORT_DIR));
		} catch (ApplicationException e1) {
			e1.printStackTrace();
			ngsProgress.setErrors("QC failed: " + e1.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.Preprocessing);
		ngsProgress.save(workDir);

		// pre-process

		File preprocessed1 = new File(workDir, CUT_ADAPTER_FILE_END + fastqPE1.getName());
		File preprocessed2 = new File(workDir, CUT_ADAPTER_FILE_END + fastqPE2.getName());

		try {
			cutAdapters(fastqPE1, preprocessed1);
			cutAdapters(fastqPE2, preprocessed2);
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("pre-process of fastq file failed: " + e.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.QC2);
		ngsProgress.save(workDir);

		// QC 2

		try {
			NgsAnalysis.qcReport(new File[] {preprocessed1, preprocessed2}, 
					new File(workDir, QC_REPORT_AFTER_PREPROCESS_DIR));
		} catch (ApplicationException e1) {
			e1.printStackTrace();
			ngsProgress.setErrors("QC failed: " + e1.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.Diamond);
		ngsProgress.save(workDir);

		// diamond blast

		File diamondDir = new File(workDir, NgsAnalysis.DIAMOND_BLAST_DIR);
		if (!(diamondDir.exists())){
			diamondDir.mkdirs();
		}
		File fastqMerge = null;
		if (fastqDir.listFiles().length > 1){
			try {
				fastqMerge = megerFiles(diamondDir, preprocessed1, preprocessed2);
			} catch (IOException e) {
				e.printStackTrace();
				ngsProgress.setErrors("diamond files could not be merged. " + e.getMessage());
				ngsProgress.save(workDir);
				return false;
			} catch (FileFormatException e) {
				e.printStackTrace();
				ngsProgress.setErrors("diamond files could not be merged. " + e.getMessage());
				ngsProgress.save(workDir);
				return false;
			} catch (ApplicationException e) {
				e.printStackTrace();
				ngsProgress.setErrors("diamond files could not be merged. " + e.getMessage());
				ngsProgress.save(workDir);
				return false;
			}
		} else {
			//continue;
		}

		File matches = null;
		File view = null;
		try {
			matches = diamondBlastX(diamondDir, fastqMerge);
			view = diamondView(diamondDir, matches);
			File resultDiamondDir = new File(workDir, NgsAnalysis.DIAMOND_RESULT_DIR);
			if (!(resultDiamondDir.exists())){
				resultDiamondDir.mkdirs();
			}
			try {
				creatDiamondResults(resultDiamondDir, view, fastqDir.listFiles()) ;
			} catch (FileFormatException e) {
				e.printStackTrace();
				ngsProgress.setErrors("diamond files could not be merged." + e.getMessage());
				ngsProgress.save(workDir);
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				ngsProgress.setErrors("diamond analysis failed: " + e.getMessage());
				ngsProgress.save(workDir);
				return false;
			}
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("blastX: " + e.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.Spades);
		ngsProgress.save(workDir);

		// spades

		File dimondResultDir = new File(workDir, DIAMOND_RESULT_DIR);
		for (File d: dimondResultDir.listFiles()){
			if (!d.isDirectory())
				continue;

			File sequenceFile1 = new File(d, fastqPE1.getName());
			File sequenceFile2 = new File(d, fastqPE2.getName());;

			File ca1 = null;
			File ca2 = null;
			// remove adapters:
			try {
				ca1 = new File(sequenceFile1.getParentFile(), CUT_ADAPTER_FILE_END + sequenceFile1.getName());
				ca2 = new File(sequenceFile2.getParentFile(), CUT_ADAPTER_FILE_END + sequenceFile2.getName());

				cutAdapters(sequenceFile1, ca1);
				cutAdapters(sequenceFile1, ca2);

			} catch (ApplicationException e2) {
				e2.printStackTrace();
				ngsProgress.getSpadesErrors().add("cut adpters failed." + e2.getMessage());
				ngsProgress.save(workDir);
				continue;
			}
			
			try {
				File assemble = assemble(ca1, ca2, workDir, d.getName());
				if (assemble == null)
					continue;

				// fill sequences.xml'
				File sequences = new File(workDir, SEQUENCES_FILE);
				if (!sequences.exists())
					try {
						sequences.createNewFile();
					} catch (IOException e1) {
						e1.printStackTrace();
						ngsProgress.setErrors("assemble failed, could not create sequences.xml");
						ngsProgress.save(workDir);
						return false;
					}

				FileUtil.appendToFile(assemble, sequences);
			} catch (ApplicationException e) {
				e.printStackTrace();
				ngsProgress.getSpadesErrors().add("assemble failed." + e.getMessage());
				ngsProgress.save(workDir);
			} catch (IOException e1) {
				e1.printStackTrace();
				ngsProgress.getSpadesErrors().add("assemble failed." + e1.getMessage());
				ngsProgress.save(workDir);
			}
		}

		File sequences = new File(workDir, SEQUENCES_FILE);
		if (!sequences.exists())
			ngsProgress.setErrors("No assembly results.");
		else
			ngsProgress.setState(State.FinishedAll);

		ngsProgress.save(workDir);
		return true; 
	}

	private static File diamondBlastX(File workDir, File query) throws ApplicationException {
		Process blastx = null;
		File matches = new File(workDir.getAbsolutePath() + File.separator + "matches.daa");
		try {
			GeneralConfig gc = Settings.getInstance().getConfig().getGeneralConfig();
			String cmd = gc.getDiamondPath() + " blastx -d "
					+ gc.getDbDiamondPath() + " -q " + query.getAbsolutePath()
					+ " -a " + matches + " -k 1  --quiet";
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

	private static File megerFiles(File workDir, File pe1, File pe2) throws IOException, FileFormatException, ApplicationException {
		File result = new File(workDir.getAbsolutePath() + File.separator +"query.fna");
		FileWriter megerFile = new FileWriter(result);
	    PrintWriter saveFile = new PrintWriter(megerFile);
		FileReader fr1 = new FileReader(pe1.getAbsolutePath());
		LineNumberReader lnr1 = new LineNumberReader(fr1);
		FileReader fr2 = new FileReader(pe2.getAbsolutePath());
		LineNumberReader lnr2 = new LineNumberReader(fr2);
		while (true){
			Sequence s1 = SequenceAlignment.readFastqFileSequence(lnr1, SequenceAlignment.SEQUENCE_DNA);
			Sequence s2 = SequenceAlignment.readFastqFileSequence(lnr2, SequenceAlignment.SEQUENCE_DNA);
			if (s1 == null || s2 == null){
				if (s1 != null || s2 != null) {
					megerFile.close();
					saveFile.close();
					throw new ApplicationException("Fastq files different");
				}
				break;
			}
			String[] name1 = s1.getName().split(" ");
			String[] name2 = s2.getName().split(" ");
			if (name1[0].equalsIgnoreCase(name2[0])){
				saveFile.println("@" + s1.getName());
				saveFile.println(s1.getSequence() + s2.getSequence());
				saveFile.println("+");
				saveFile.println(s1.getQuality() + s2.getQuality());
			}
		}
		saveFile.close();
		megerFile.close();
		return result;
	}

	// assume that the map is not very big.
	private static String getTaxusId(Map<String, LinkedHashSet<String>> taxoId, String taxusName) {
		for (Map.Entry<String, LinkedHashSet<String>> e: taxoId.entrySet()) {
			if (e.getValue().contains(taxusName))
				return e.getKey();
		}

		return null;
	}

	private static void creatDiamondResults(File workDir, File view, File[] files) throws FileFormatException, IOException {
		String line = "";

		BufferedReader bf;
		bf = new BufferedReader(new FileReader(view.getAbsolutePath()));
		Map<String, String> taxoNameId = new HashMap<String, String>();
		while ((line = bf.readLine()) != null)  {
			String[] name = line.split("\\t");
			String[] taxon = name[1].split("_");
			taxoNameId.put(name[0], taxon[0]);
		}
		bf.close();

		for (Map.Entry<String, String> s: taxoNameId.entrySet())
			System.err.println(s.getKey() + " = " + s.getValue());

		for(File f : files){
			FileReader fileReader = new FileReader(f.getAbsolutePath());
			LineNumberReader lnr = new LineNumberReader(fileReader);
			while(true){
				Sequence s = SequenceAlignment.readFastqFileSequence(lnr, SequenceAlignment.SEQUENCE_DNA);
				if (s == null){
					break;
				}

				String[] name = s.getName().split(" "); // TODO was name ?

				String taxosId = taxoNameId.get(name[0]);
				if (taxosId == null)
					continue; // TODO ??
				File taxonDir = new File(workDir, taxosId);
				taxonDir.mkdirs();

				FileWriter fastq = new FileWriter(taxonDir.getAbsoluteFile() + File.separator + f.getName(), true);
				PrintWriter saveFastq = new PrintWriter(fastq);

				saveFastq.println("@" + s.getName());
				saveFastq.println(s.getSequence());
				saveFastq.println(s.getDescription());
				saveFastq.println(s.getQuality());

				fastq.close();
			}
			fileReader.close();
			lnr.close();
		}
	}

	public static Map<String, Integer> countDiamondREsults(File workDir) {
		File view = new File(workDir + File.separator + DIAMOND_BLAST_DIR, "matches.fasta");
		BufferedReader bf;
		Map<String, Integer> taxoIdCount = new HashMap<String, Integer>();
		try {
			bf = new BufferedReader(new FileReader(view.getAbsolutePath()));
			String line;
			while ((line = bf.readLine()) != null)  {
				String[] name = line.split("\\t");
				String[] taxon = name[1].split("_");
				String key = taxon[0] + "_" + taxon[1];
				if (!taxoIdCount.containsKey(key))
					taxoIdCount.put(key, 0);
				taxoIdCount.put(key, taxoIdCount.get(key) + 1);
			}
			bf.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return taxoIdCount;
	}

	/**
	 * Use FastQC to generate quality control reports
	 * @param sequenceFiles
	 * @param workDir
	 * @return a list of result html files.
	 * @throws ApplicationException
	 */
	public static List<File> qcReport(File[] sequenceFiles, File reportDir) throws ApplicationException {		
		reportDir.mkdirs();

		String fastQCcmd = Settings.getInstance().getConfig().getGeneralConfig().getFastqcCmd();
		String cmd = fastQCcmd;
		for (File f: sequenceFiles) 
			cmd += " " + f.getAbsolutePath();

		cmd += " -outdir " + reportDir.getAbsolutePath();

		System.err.println(cmd);
		Process p = null;

		try {
			p = StreamReaderRuntime.exec(cmd, null, reportDir.getAbsoluteFile());
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
	 * cutadapt -b ADAPTER -o output.fastq ~/install/fasta-examples/hiv_1.fastq
	 * @param fastqFile
	 * @param workDir
	 * @param outFile
	 * @throws ApplicationException
	 */
	public static void cutAdapters(File fastqFile, File outFile) throws ApplicationException {
		String cmd = Settings.getInstance().getConfig().getGeneralConfig().getCutAdaptCmd();
		cmd += " -b ADAPTER -o " + outFile.getAbsolutePath() + " " + fastqFile.getAbsolutePath();

		System.err.println(cmd);
		Process p = null;

		try {
			p = StreamReaderRuntime.exec(cmd, null, fastqFile.getParentFile());
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
		cmd += " --phred-offset 33 ";

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
