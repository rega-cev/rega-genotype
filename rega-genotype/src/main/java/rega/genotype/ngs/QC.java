package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import rega.genotype.ApplicationException;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.StreamReaderRuntime;

/**
 * Report the quality of input fastq files.
 * Goes to the user.
 */
public class QC {
	/**
	 * Results from fastqc summary.txt.
	 */
	public static class QcResults {
		public enum Result {
			Pass, Fail, Warn;

			public static Result fromString(String str){
				for (Result result: Result.values())
					if(result.name().toUpperCase().equals(str))
						return result;
				return null;
			}
		}

		Result basicStatistics;
		Result perbaseSequenceQuality;
		Result persequenceQualityScores;
		Result perbaseSequenceContent;
		Result persequenceGCContent;
		Result perbaseNcontent;
		Result sequenceLengthDistribution;
		Result sequenceDuplicationLevels;
		Result overrepresentedsequences;
		Result adapterContent;
		Result kmerContent;

		public QcResults(File fastqcFile) throws ApplicationException {

			String fileContent = FileUtil.getFileContent(fastqcFile, "summary.txt");
			if (fileContent == null)
				throw new ApplicationException("File to read FastQC report.");

			String[] lines = fileContent.split("\n");

			int lineNum = 0;
			for(String line: lines) {
				String result = line.split("\t")[0];

				switch (lineNum) {
				case 0:
					basicStatistics = Result.fromString(result);
					break;
				case 1:
					perbaseSequenceQuality = Result.fromString(result);
					break;
				case 2:
					persequenceQualityScores = Result.fromString(result);
					break;
				case 3:
					perbaseSequenceContent = Result.fromString(result);
					break;
				case 4:
					persequenceGCContent = Result.fromString(result);
					break;
				case 5:
					perbaseNcontent = Result.fromString(result);
					break;
				case 6:
					sequenceLengthDistribution = Result.fromString(result);
					break;
				case 7:
					sequenceDuplicationLevels = Result.fromString(result);
					break;
				case 8:
					overrepresentedsequences = Result.fromString(result);
					break;
				case 9:
					adapterContent = Result.fromString(result);
					break;
				case 10:
					kmerContent = Result.fromString(result);
					break;
				}

				lineNum++;
			}   

		}
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
		for (File f: sequenceFiles) {
			if (f == null)
				throw new ApplicationException("Pre-processing error: result file wes not created.");
			cmd += " " + f.getAbsolutePath();
		}

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

	public static List<QcResults> getResults(File reportDir) throws ApplicationException {
		List<QcResults> ans = new ArrayList<QcResults>();

		for(File reportFile: reportDir.listFiles()) {
			if (FilenameUtils.getExtension(reportFile.getAbsolutePath()).equals("zip")){
				QcResults qcResults = new QcResults(reportFile);
				ans.add(qcResults);
			}
		}

		return ans;
	}
}
