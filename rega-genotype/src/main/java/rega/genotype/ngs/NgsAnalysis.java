package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import rega.genotype.ApplicationException;
import rega.genotype.framework.async.LongJobsScheduler;
import rega.genotype.framework.async.LongJobsScheduler.Lock;
import rega.genotype.ngs.NgsProgress.State;
import rega.genotype.ngs.QC.QcResults;
import rega.genotype.ngs.QC.QcResults.Result;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.LogUtils;

/**
 * Contract long virus contigs from ngs output. 
 * Steps:QC (FastQC), pre-processing (Trimmomatic),
 * primary search (Diamond blast), assembly (Spades) 
 * 
 * @author michael
 */
public class NgsAnalysis {
	private File workDir;

	private Logger ngsLogger = null;

	public NgsAnalysis(File workDir){
		this.workDir = workDir;
		ngsLogger = LogUtils.createLogger(new File(workDir, "ngs-log"), "ngsLogger");
	}

	/**
	 * Pre-process fastq sequences.
	 * By default use cutadapt
	 * Can be re-implemented.
	 * 
	 * @throws ApplicationException
	 */
	protected void preprocess() throws ApplicationException {
		Preprocessing.cutadaptPreprocess(workDir);
	}

	/**
	 * Primary search: sort the short NGS sequences 
	 * By default use diamond blast
	 * Can be re-implemented.
	 * 
	 * @throws ApplicationException
	 */
	protected void primarySearch() throws ApplicationException {
		PrimarySearch.diamondSearch(workDir);
	}

	/**
	 * assemble ngs data to long contigs
	 * 
	 * By default use Spades
	 * Can be re-implemented.
	 * 
	 * @throws ApplicationException
	 */
	protected File assemble(File sequenceFile1, File sequenceFile2, String virusName) throws ApplicationException {
		return Assemble.spadesAssemble(sequenceFile1, sequenceFile2, workDir, virusName);
	}

	/**
	 * Contract long virus contigs from ngs output. 
	 * Steps:QC (FastQC), pre-processing (Trimmomatic),
	 * primary search (Diamond blast), assembly (Spades)
	 * @param workDir
	 */
	public boolean analyze() {
		NgsProgress ngsProgress = NgsProgress.read(workDir);

		// QC

		ngsProgress.setState(State.QC);
		ngsProgress.save(workDir);

		boolean needPreprocessing = false; // for now we base it only on adapter content.
		File fastqDir = NgsFileSystem.fastqDir(workDir);
		try {
			QC.qcReport(fastqDir.listFiles(),
					new File(workDir, NgsFileSystem.QC_REPORT_DIR));

			List<QcResults> qcresults = QC.getResults(new File(workDir, NgsFileSystem.QC_REPORT_DIR));
			for (QcResults qcr: qcresults) {
				if (qcr.adapterContent == Result.Fail)
					needPreprocessing = true;
			}
		} catch (ApplicationException e1) {
			e1.printStackTrace();
			ngsProgress.setErrors("QC failed: " + e1.getMessage());
			ngsProgress.save(workDir);
			cleanBigData();
			return false;
		}

		ngsProgress.setSkipPreprocessing(!needPreprocessing);
		ngsProgress.setState(State.Preprocessing);
		ngsProgress.save(workDir);

		// pre-process
		if (needPreprocessing) {
			try {
				preprocess();
			} catch (ApplicationException e) {
				e.printStackTrace();
				ngsProgress.setErrors("Preprocessing failed: " + e.getMessage());
				ngsProgress.save(workDir);
				cleanBigData();
				return false;
			}

			File preprocessed1 = NgsFileSystem.preprocessedPE1(workDir);
			File preprocessed2 = NgsFileSystem.preprocessedPE2(workDir);

			ngsProgress = NgsProgress.read(workDir);
			ngsProgress.setState(State.QC2);
			ngsProgress.save(workDir);

			// QC 2

			try {
				QC.qcReport(new File[] {preprocessed1, preprocessed2}, 
						new File(workDir, NgsFileSystem.QC_REPORT_AFTER_PREPROCESS_DIR));
			} catch (ApplicationException e1) {
				e1.printStackTrace();
				ngsProgress.setErrors("QC failed: " + e1.getMessage());
				ngsProgress.save(workDir);
				cleanBigData();
				return false;
			}
		}

		// diamond blast

		try {
			primarySearch();
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("primary search failed: " + e.getMessage());
			ngsProgress.save(workDir);
			cleanBigData();
			return false;
		}

		boolean ans = assembleAll();

		cleanBigData();
		return ans;
	}

	/**
	 * Delete large ngs files from work dir.
	 */
	private void cleanBigData() {
		File preprocessedDir = NgsFileSystem.preprocessedDir(workDir);
		File fastqDir = NgsFileSystem.fastqDir(workDir);
		File diamondDBDir = new File(workDir, NgsFileSystem.DIAMOND_BLAST_DIR);
		try {
			if (fastqDir.exists())
				FileUtils.deleteDirectory(fastqDir);
			if (preprocessedDir.exists())
				FileUtils.deleteDirectory(preprocessedDir);
			if (diamondDBDir.exists())
				FileUtils.deleteDirectory(diamondDBDir);
		} catch (IOException e) {
			e.printStackTrace();
			// leave it
		}
	}

	public boolean assembleAll() {
		NgsProgress ngsProgress = NgsProgress.read(workDir); // primarySearch may update NgsProgress with diamond results.
		ngsProgress.setState(State.Spades);
		ngsProgress.save(workDir);

		// spades
		Lock jobLock = LongJobsScheduler.getInstance().getJobLock(workDir);

//		File fastqPE1 = NgsFileSystem.fastqPE1(workDir);
//		File fastqPE2 = NgsFileSystem.fastqPE2(workDir);

		File fastqPE1 = NgsFileSystem.preprocessedPE1(workDir);
		File fastqPE2 = NgsFileSystem.preprocessedPE2(workDir);

		File dimondResultDir = new File(workDir, NgsFileSystem.DIAMOND_RESULT_DIR);
		for (File d: dimondResultDir.listFiles()){
			if (!d.isDirectory())
				continue;

			File sequenceFile1 = new File(d, fastqPE1.getName());
			File sequenceFile2 = new File(d, fastqPE2.getName());

			if (sequenceFile1.length() < 1000*1000)
				continue; // no need to assemble if there is not enough reads.

			try {
				long startAssembly = System.currentTimeMillis();

				File assembledFile = assemble(
						sequenceFile1, sequenceFile2, d.getName());
				if (assembledFile == null)
					continue;

				long endAssembly =  System.currentTimeMillis();
				ngsLogger.info("assembled " + d.getName() + " = " + (startAssembly - endAssembly) + " ms");
				
				// fill sequences.xml'
				File sequences = new File(workDir, NgsFileSystem.SEQUENCES_FILE);
				if (!sequences.exists())
					try {
						sequences.createNewFile();
					} catch (IOException e1) {
						e1.printStackTrace();
						ngsProgress.setErrors("assemble failed, could not create sequences.xml");
						ngsProgress.save(workDir);
						return false;
					}

				File alingment = SequenceToolMakeConsensus.consensusAlign(assembledFile, workDir, d.getName());
				File consensus = SequenceToolMakeConsensus.makeConsensus(alingment, workDir, d.getName());

				ngsLogger.info("consensus " + d.getName() + " = " + (endAssembly - System.currentTimeMillis()) + " ms");

				FileUtil.appendToFile(consensus, sequences);

			} catch (Exception e) {
				e.printStackTrace();
				ngsProgress.getSpadesErrors().add("assemble failed." + e.getMessage());
				ngsProgress.save(workDir);
			}
		}

		jobLock.release();

		File sequences = new File(workDir, NgsFileSystem.SEQUENCES_FILE);
		if (!sequences.exists())
			ngsProgress.setErrors("No assembly results.");
		else
			ngsProgress.setState(State.FinishedAll);

		ngsProgress.save(workDir);

		return true;
	}
}
