package rega.genotype.ngs;

import java.io.File;
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
	public static void trimomatic(NgsResultsTracer ngsResults, Logger logger) throws ApplicationException {
		File ngsModulePath = Settings.getInstance().getConfig().trimomaticPath();
		if (ngsModulePath == null)
			throw new ApplicationException("NGS module is missing contact server admin.");

		File preprocessedDir = new File(ngsResults.getWorkDir(),
				NgsFileSystem.PREPROCESSED_DIR);
		preprocessedDir.mkdirs();

		String inputFileNames;
		String outoutFileNames;
		String inType = ngsResults.getModel().isPairEnd() ? " PE " : " SE ";
		if (ngsResults.getModel().isPairEnd()) {
			File sequenceFile1 = NgsFileSystem.fastqPE1(ngsResults);
			File sequenceFile2 = NgsFileSystem.fastqPE2(ngsResults);

			File paired1 = NgsFileSystem.createPreprocessedPE1(ngsResults.getWorkDir());
			File paired2 = NgsFileSystem.createPreprocessedPE2(ngsResults.getWorkDir());

			File unpaired1 = new File(preprocessedDir, NgsFileSystem.PREPROCESSED_FILE_NAMR_UNPAIRD + sequenceFile1.getName());
			File unpaired2 = new File(preprocessedDir, NgsFileSystem.PREPROCESSED_FILE_NAMR_UNPAIRD + sequenceFile2.getName());

			inputFileNames = sequenceFile1.getAbsolutePath() + " " + sequenceFile2.getAbsolutePath();

			outoutFileNames = paired1.getAbsolutePath()
					+ " " + unpaired1 .getAbsolutePath()
					+ " " + paired2.getAbsolutePath()
					+ " " + unpaired2.getAbsolutePath();
		} else {
			File sequenceFile = NgsFileSystem.fastqSE(ngsResults);
			File out = NgsFileSystem.createPreprocessedSE(ngsResults.getWorkDir());
			inputFileNames = sequenceFile.getAbsolutePath();
			outoutFileNames = out.getAbsolutePath();
		}

		String trimmomaticPath = Settings.getInstance().getConfig().trimomaticPath().getAbsolutePath();
		String adaptersFilePath = Settings.getInstance().getConfig().adaptersFilePath().getAbsolutePath();

		String trimmomaticCmd = "java -Xmx1000m -jar " + trimmomaticPath + inType + " -threads 4 ";
		String trimmomaticOptions = " ILLUMINACLIP:" + adaptersFilePath + ":2:30:10 HEADCROP:15 LEADING:10 TRAILING:10 SLIDINGWINDOW:4:20 MINLEN:50";

		String cmd = trimmomaticCmd + " " + inputFileNames + " " + outoutFileNames + " " + trimmomaticOptions;

		NgsFileSystem.executeCmd(cmd, ngsResults.getWorkDir(), logger);
	}
}
