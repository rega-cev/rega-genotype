package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import rega.genotype.ApplicationException;
import rega.genotype.config.NgsModule;
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
	private Logger ngsLogger = null;
	private File workDir;
	private NgsModule ngsModule;

	public NgsAnalysis(File workDir, NgsModule ngsModule){
		this.workDir = workDir;
		this.ngsModule = ngsModule;
		ngsLogger = LogUtils.createLogger(workDir);
	}

	/**
	 * Pre-process fastq sequences.
	 * By default use cutadapt
	 * Can be re-implemented.
	 * 
	 * @throws ApplicationException
	 */
	protected void preprocess() throws ApplicationException {
		// Preprocessing.cutadaptPreprocess(workDir);
		Preprocessing.trimomatic(workDir);
	}

	/**
	 * Primary search: sort the short NGS sequences 
	 * By default use diamond blast
	 * Can be re-implemented.
	 * 
	 * @throws ApplicationException
	 */
	protected void primarySearch() throws ApplicationException {
		PrimarySearch.diamondSearch(workDir, ngsModule);
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
		return Assemble.spadesAssemble(sequenceFile1, sequenceFile2, workDir, virusName, ngsModule);
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
					new File(workDir, NgsFileSystem.QC_REPORT_DIR),
					workDir);

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
						new File(workDir, NgsFileSystem.QC_REPORT_AFTER_PREPROCESS_DIR),
						workDir);
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
	public void cleanBigData() {
		if (!workDir.exists() || workDir.listFiles() == null)
			return;
		File preprocessedDir = NgsFileSystem.preprocessedDir(workDir);
		File fastqDir = NgsFileSystem.fastqDir(workDir);
		File diamondDBDir = new File(workDir, NgsFileSystem.DIAMOND_BLAST_DIR);
		// delete all html files
		for (File f: workDir.listFiles())
			if (f.isFile() && f.getName().endsWith(".html"))
				f.delete();

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

		File dimondResultDir = new File(workDir, NgsFileSystem.DIAMOND_RESULT_DIR);
		for (File d: dimondResultDir.listFiles()){
			assembleVirus(d);
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

	public boolean assembleVirus(File virusDir) {
		if (!virusDir.isDirectory())
			return false;

		NgsProgress ngsProgress = NgsProgress.read(workDir);

		String fastqPE1FileName = NgsFileSystem.fastqPE1(workDir).getName();
		String fastqPE2FileName = NgsFileSystem.fastqPE2(workDir).getName();

		File sequenceFile1 = new File(virusDir, fastqPE1FileName);
		File sequenceFile2 = new File(virusDir, fastqPE2FileName);

		if (sequenceFile1.length() < 1000*100)
			return false; // no need to assemble if there is not enough reads.

		try {
			long startAssembly = System.currentTimeMillis();

			File assembledFile = assemble(
					sequenceFile1, sequenceFile2, virusDir.getName());
			if (assembledFile == null)
				return false;

			long endAssembly =  System.currentTimeMillis();
			ngsLogger.info("assembled " + virusDir.getName() + " = " + (endAssembly - startAssembly) + " ms");

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

			File alingment = SequenceToolMakeConsensus.consensusAlign(assembledFile, workDir, virusDir.getName(), ngsModule);
			File consensus = SequenceToolMakeConsensus.makeConsensus(alingment, workDir, virusDir.getName(), ngsModule);

			ngsLogger.info("consensus " + virusDir.getName() + " = " + (System.currentTimeMillis() - endAssembly) + " ms");

			FileUtil.appendToFile(consensus, sequences);

		} catch (Exception e) {
			e.printStackTrace();
			ngsProgress.getSpadesErrors().add("assemble failed." + e.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		return true;
	}
}
