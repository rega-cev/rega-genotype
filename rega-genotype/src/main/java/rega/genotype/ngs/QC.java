package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import rega.genotype.ApplicationException;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.LogUtils;
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

	public static class QcData {
		private Integer readLength = null;

		public QcData(File fastqcFile) throws ApplicationException {
			String fileContent = FileUtil.getFileContent(fastqcFile, "fastqc_data.txt");
			if (fileContent == null)
				throw new ApplicationException("File to read FastQC report.");		

			String[] lines = fileContent.split("\n");

			for(String line: lines) {
				String[] parts = line.split("\t");
				if (parts.length == 2 && parts[0].equals("Sequence length")) {
					String len = parts[1];
					if (len.contains("-"))
						len = len.split("-")[1];
					try {
						readLength = Integer.parseInt(len);
					} catch (NumberFormatException e) {
						readLength = null;
					}
					break; // for now we do not need more data.
				}
			}
		}

		public Integer getReadLength() {
			return readLength;
		}
	}

	/**
	 * Use FastQC to generate quality control reports
	 * @param sequenceFiles
	 * @param workDir
	 * @return a list of result html files.
	 * @throws ApplicationException
	 */
	public static List<File> qcReport(File[] sequenceFiles, File reportDir, File workDir) throws ApplicationException {		
		reportDir.mkdirs();

		String fastQCcmd = Settings.getInstance().getConfig().getGeneralConfig().getFastqcCmd();
		String cmd = fastQCcmd;
		for (File f: sequenceFiles) {
			if (f == null)
				throw new ApplicationException("Pre-processing error: result file wes not created.");
			cmd += " " + f.getAbsolutePath();
		}

		cmd += " -outdir " + reportDir.getAbsolutePath();

		LogUtils.getLogger(workDir).info(cmd);
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

	public static File qcReportFile(File workDir) {
		File reportDir = new File(workDir, NgsFileSystem.QC_REPORT_DIR);
		for(File reportFile: reportDir.listFiles()) {
			if (FilenameUtils.getExtension(reportFile.getAbsolutePath()).equals("zip")){
				return reportFile;
			}
		}

		return null;
	}

	public static File qcPreprocessedReportFile(File workDir) {
		File reportDir = new File(workDir, NgsFileSystem.QC_REPORT_AFTER_PREPROCESS_DIR);
		for(File reportFile: reportDir.listFiles()) {
			if (FilenameUtils.getExtension(reportFile.getAbsolutePath()).equals("zip")){
				return reportFile;
			}
		}

		return null;
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

	public static Integer readLen(File jobDir) {
		Integer readLen = null;

		try {
			File qcReportFile = QC.qcPreprocessedReportFile(jobDir);
			if (qcReportFile == null || !qcReportFile.exists())
				qcReportFile = QC.qcReportFile(jobDir); // some times we do not do preprocessing.
			if (qcReportFile != null) {
				QcData qcData = new QC.QcData(qcReportFile);
				readLen = qcData.getReadLength();
			}
		} catch (ApplicationException e1) {
			e1.printStackTrace();
		}

		return readLen;
	}
}
