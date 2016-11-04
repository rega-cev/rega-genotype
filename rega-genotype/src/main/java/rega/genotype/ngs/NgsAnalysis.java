package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;

import rega.genotype.ApplicationException;
import rega.genotype.framework.async.LongJobsScheduler;
import rega.genotype.framework.async.LongJobsScheduler.Lock;
import rega.genotype.ngs.NgsProgress.State;
import rega.genotype.utils.FileUtil;

/**
 * Contract long virus contigs from ngs output. 
 * Steps:QC (FastQC), pre-processing (Trimmomatic),
 * primary search (Diamond blast), assembly (Spades) 
 * 
 * @author michael
 */
public class NgsAnalysis {
	private File workDir;

	public NgsAnalysis(File workDir){
		this.workDir = workDir;
	}

	/**
	 * Pre-process fastq sequences.
	 * By default use cutadapt
	 * Can be re-implemented.
	 * 
	 * @throws ApplicationException
	 */
	protected void preprocess() throws ApplicationException {
		//Preprocessing.cutadaptPreprocess(workDir);
		Preprocessing.generalPreprocessing("java -Xmx1000m -jar trimomatic ..", workDir);
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

		ngsProgress.setState(State.QC);
		ngsProgress.save(workDir);

		// QC

		File fastqDir = NgsFileSystem.fastqDir(workDir);
		try {
			QC.qcReport(fastqDir.listFiles(),
					new File(workDir, NgsFileSystem.QC_REPORT_DIR));
		} catch (ApplicationException e1) {
			e1.printStackTrace();
			ngsProgress.setErrors("QC failed: " + e1.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.Preprocessing);
		ngsProgress.save(workDir);

		// pre-process

		try {
			preprocess();
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("Preprocessing failed: " + e.getMessage());
			ngsProgress.save(workDir);
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
			return false;
		}

		// diamond blast

		try {
			primarySearch();
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.setErrors("primary search failed: " + e.getMessage());
			ngsProgress.save(workDir);
			return false;
		}

		ngsProgress.setState(State.Spades);
		ngsProgress.save(workDir);

		// spades
		Lock jobLock = LongJobsScheduler.getInstance().getJobLock(workDir);
		
		File fastqPE1 = NgsFileSystem.fastqPE1(workDir);
		File fastqPE2 = NgsFileSystem.fastqPE2(workDir);

		File dimondResultDir = new File(workDir, NgsFileSystem.DIAMOND_RESULT_DIR);
		for (File d: dimondResultDir.listFiles()){
			if (!d.isDirectory())
				continue;

			File sequenceFile1 = new File(d, fastqPE1.getName());
			File sequenceFile2 = new File(d, fastqPE2.getName());

			if (sequenceFile1.length() < 1000*1000)
				continue; // no need to assemble if there is not enough reads.

			try {
				File assembledFile = assemble(
						sequenceFile1, sequenceFile2, d.getName());
				if (assembledFile == null)
					continue;

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
