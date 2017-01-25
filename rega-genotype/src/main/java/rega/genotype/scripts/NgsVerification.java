package rega.genotype.scripts;

import java.io.File;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import rega.genotype.ApplicationException;
import rega.genotype.ngs.NgsResultsTracer;
import rega.genotype.ngs.QC;
import rega.genotype.ngs.QC.QcData;
import rega.genotype.ngs.model.NgsResultsModel.State;
import rega.genotype.tools.blast.BlastJobOverviewForm.BlastResultParser;
import rega.genotype.tools.blast.BlastJobOverviewForm.ClusterData;
import rega.genotype.tools.blast.BlastJobOverviewForm.SequenceData;
import rega.genotype.utils.ExcelUtils;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Utils;

/**
 * Summarize all the results in job dir to 1 excel file.
 * 
 * @author michael
 */
public class NgsVerification {
	// File Name	Expected	VIS Result	Reads	Cov	Time	
	private static final int FILE_NAME_COLUMN   = 0;
	private static final int EXPECTED_COLUMN    = 1;
	private static final int RESULTS_COLUMN     = 2;
	private static final int REF_COLUMN         = 3;
	private static final int CONTIGS_COLUMN     = 4;
	private static final int LENGTH_COLUMN      = 5;
	private static final int READ_COLUMN        = 6;
	private static final int COV_COLUMN         = 7;
	private static final int TIME_COLUMN        = 8;
	private static final int TOTAL_READS_COLUMN = 9;
	private static final int READS_LEN_COLUMN = 10;
	private static final int FILE_SIZE_COLUMN   = 11;

	private static final String[] headers = {
		"File Name", "Expected", "VIS Result", "Ref", "Contigs Count", "Length %", 
		"Reads count", "Deep cov", "Time", "#Total reads", "Read length", "Input file (zipped) size"}; 

	/**
	 * Summarize all the results in job dir to 1 excel file.
	 * @param args jobDir.
	 */
	public static void main(String args[]) {
		File toolJobDir = new File(args[0]);
		verify(toolJobDir);
	}

	public static void verify(File toolJobDir) {
		File out = new File(toolJobDir, "ngs-verification.xlsx");

		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet worksheet = workbook.createSheet("sheet");

		for (int i = 0; i < headers.length; ++i) {
			ExcelUtils.add(worksheet, 0, i, headers[i]);
		}
		ExcelUtils.writeDocument(workbook, out);

		int row = 1;
		for (File currentJobDir: toolJobDir.listFiles()) {
			if (!currentJobDir.isDirectory())
				continue;

			File resultsFile = new File(currentJobDir, "result.xml");
			if (!resultsFile.exists()) {
				ExcelUtils.add(worksheet, row, FILE_NAME_COLUMN, currentJobDir.getName());
				row++;
				continue;
			}

			ResultParser parser = new ResultParser();
			parser.parseFile(resultsFile);

			row += writeResults(currentJobDir, worksheet, row, parser.clusterDataMap);
		}

		ExcelUtils.writeDocument(workbook, out);
	}

	/**
	 * 
	 * @param currentJobDir
	 * @param hssfRow
	 * @param clusterDataMap
	 * @return how much rows where added.
	 */
	private static int writeResults(final File currentJobDir, final HSSFSheet worksheet,
			final int row, final Map<String, ClusterData> clusterDataMap) {
		Integer readLen = null;
		Integer totalNumberOfReads = null;

		try {
			QcData qcData = new QcData(QC.usedQcFile(currentJobDir));
			readLen = qcData.getReadLength();
			totalNumberOfReads = qcData.getTotalNumberOfReads();
		} catch (ApplicationException e) {
			e.printStackTrace();
		}

		int r = row;
		for (ClusterData cluster : clusterDataMap.values()) {	
			HSSFRow hssfRow = ExcelUtils.add(worksheet, r, FILE_NAME_COLUMN, currentJobDir.getName());
			r++;
			double contigsLen = 0;
			double readCount = 0;
			Integer refLen = null;
			String refName = null;
			String lenErrors = null;
			String covErrors = null;
			String bucket = null;

			for (SequenceData seq: cluster.sequencesData) {
				//11051__contig_1_len_10306_cov_950.489 vip
				// currently cov is encoded in description
				// # reads as Sum of (contig length * coverage / read length)
				refName = null;
				if (seq.length == null)
					lenErrors = "seq len not found";
				else
					contigsLen += seq.length;
				String[] seqParts = seq.description.split("__");
				String[] seqNameParts = seqParts[0].split("_");
				
				double readCov = 0.0;
				for (int j = 1; j < seqNameParts.length - 1; ++j) {
					if (seqNameParts[j].equals("cov")) {
						try {
							readCov = Double.parseDouble(seqNameParts[j + 1]);
						} catch (NumberFormatException e2) {
							covErrors = "cov not found";
						}
					}
				}
				for (int j = 1; j < seqParts.length - 1; ++j) {
					if (seqParts[j].equals("reflen")) {
						try {
							refLen = Integer.parseInt(seqParts[j + 1]);
						} catch (NumberFormatException e2) {
							lenErrors = "ref len not found";
						}
					}  else if (seqParts[j].equals("refName")) {
						if (refName == null)
							refName = seqParts[j + 1];
						else if (!refName.equals(seqParts[j + 1]))
							System.err.println("ERROR: not same ref!");
					} else if (seqParts[j].equals("bucket")) {
						bucket = seqParts[j + 1];
					}
				}

				readCount += readCov * seq.length / readLen;
			}
			double deepCov = readCount * readLen / contigsLen;

			ExcelUtils.add(hssfRow, RESULTS_COLUMN, cluster.concludedName);
			ExcelUtils.add(hssfRow, REF_COLUMN, refName);
			ExcelUtils.add(hssfRow, CONTIGS_COLUMN, cluster.sequencesData.size()+"");
			ExcelUtils.add(hssfRow, LENGTH_COLUMN,  
					contigsLen / refLen  * 100
					+ "% (" + contigsLen + " of " + refLen + ")");
			ExcelUtils.add(hssfRow, READ_COLUMN, readCount+"");
			ExcelUtils.add(hssfRow, COV_COLUMN, deepCov+"");
			
			final NgsResultsTracer ngsResults = NgsResultsTracer.read(currentJobDir);
			if (ngsResults == null){
				ExcelUtils.add(hssfRow, TIME_COLUMN, "Error no ngs progress file");
			} else {
				Long startTime = ngsResults.getModel().getStateStartTime(State.Init);
				Long endTime = System.currentTimeMillis();
				if(ngsResults.getModel().getState() == State.FinishedAll)
					endTime = ngsResults.getModel().getStateStartTime(State.FinishedAll);
				ExcelUtils.add(hssfRow, TIME_COLUMN, Utils.formatTime(endTime - startTime));				
			}
			ExcelUtils.add(hssfRow, TOTAL_READS_COLUMN, totalNumberOfReads+"");
			ExcelUtils.add(hssfRow, READS_LEN_COLUMN, readLen+"");

			// TODO: this will work only on our server.
			File in = FileUtil.find(currentJobDir.getParentFile(), currentJobDir.getName(), ".fastq.gz");
			if (in == null)
				ExcelUtils.add(hssfRow, FILE_SIZE_COLUMN, "Input file was deleted?");
			else
				ExcelUtils.add(hssfRow, FILE_SIZE_COLUMN, in.length() + " bytes");
		}

		return clusterDataMap.size();
	}
	
	public static class ResultParser extends BlastResultParser {
		public ResultParser() {
			super(null);
		}
		
		public void updateUi() {
		}
	}
}
