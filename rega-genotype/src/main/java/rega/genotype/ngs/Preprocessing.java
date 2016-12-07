package rega.genotype.ngs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import rega.genotype.ApplicationException;
import rega.genotype.singletons.Settings;

public class Preprocessing{

	/**
	 * Execute pre-processing from command line. (Any pre-processing software can be used this way)
	 * 
	 * When using this function you should know fastq files are stored in NgsFileSystem.FASTQ_FILES_DIR
	 * The result (pre-processed) files need to be stored in 
	 * NgsFileSystem.PREPROCESSED_PE1_DIR and NgsFileSystem.PREPROCESSED_PE2_DIR
	 * Then the next step knows where to find them.
	 * @See NgsFileSystem documentation for more information.
	 * 
	 * @param cmd 
	 * @param workDir - the virus job dir
	 * @throws ApplicationException
	 */
	public static void generalPreprocessing(String cmd, File workDir, Logger logger) throws ApplicationException {
		NgsFileSystem.executeCmd(cmd, workDir, logger);
	}

	/**
	 * preprocess fastq file with trimmomatic.
	 * args = ILLUMINACLIP:adapters.fasta:2:10:7:1 LEADING:10 TRAILING:10  MINLEN:5
	 * @param sequenceFile
	 * @param workDir - the virus job dir
	 * @return preprocessed fastq file
	 * @throws ApplicationException
	 * TODO: delete - not used
	 */
	public static List<File> trimomatic(File workDir, Logger logger) throws ApplicationException {
		File sequenceFile1 = NgsFileSystem.fastqPE1(workDir);
		File sequenceFile2 = NgsFileSystem.fastqPE2(workDir);

		File ngsModulePath = Settings.getInstance().getConfig().trimomaticPath();
		if (ngsModulePath == null)
			throw new ApplicationException("NGS module is missing contact server admin.");

		String trimmomaticPath = Settings.getInstance().getConfig().trimomaticPath().getAbsolutePath();
		String adaptersFilePath = Settings.getInstance().getConfig().adaptersFilePath().getAbsolutePath();

		String trimmomaticCmd = "java -Xmx1000m -jar " + trimmomaticPath + " PE -threads 1 ";
		String trimmomaticOptions = " ILLUMINACLIP:" + adaptersFilePath + ":2:30:10 HEADCROP:15 LEADING:10 TRAILING:10 SLIDINGWINDOW:4:20 MINLEN:50";

		String inputFileNames = sequenceFile1.getAbsolutePath() + " " + sequenceFile2.getAbsolutePath();

		File preprocessedDir = new File(workDir, NgsFileSystem.PREPROCESSED_DIR);
		preprocessedDir.mkdirs();
		NgsFileSystem.preprocessedPE1(workDir);
		
		File paired1 = NgsFileSystem.createPreprocessedPE1(workDir, sequenceFile1.getName());
		File paired2 = NgsFileSystem.createPreprocessedPE2(workDir, sequenceFile2.getName());

		File unpaired1 = new File(preprocessedDir, NgsFileSystem.PREPROCESSED_FILE_NAMR_UNPAIRD + sequenceFile1.getName());
		File unpaired2 = new File(preprocessedDir, NgsFileSystem.PREPROCESSED_FILE_NAMR_UNPAIRD + sequenceFile2.getName());

		String outoutFileNames = paired1.getAbsolutePath()
				+ " " + unpaired1 .getAbsolutePath()
				+ " " + paired2.getAbsolutePath()
				+ " " + unpaired2.getAbsolutePath();

		String cmd = trimmomaticCmd + " " + inputFileNames + " " + outoutFileNames + " " + trimmomaticOptions;

		NgsFileSystem.executeCmd(cmd, workDir, logger);

		List<File> ans = new ArrayList<File>();
		ans.add(paired1);
		ans.add(paired2);
		ans.add(unpaired1);
		ans.add(unpaired2);
		return ans;
	}
}
