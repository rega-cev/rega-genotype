package rega.genotype.ngs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;

import rega.genotype.AbstractSequence;
import rega.genotype.ApplicationException;
import rega.genotype.Constants;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.config.NgsModule;
import rega.genotype.framework.async.LongJobsScheduler;
import rega.genotype.framework.async.LongJobsScheduler.Lock;
import rega.genotype.ngs.QC.QcData;
import rega.genotype.ngs.QC.QcResults;
import rega.genotype.ngs.QC.QcResults.Result;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.ngs.model.Contig;
import rega.genotype.ngs.model.DiamondBucket;
import rega.genotype.ngs.model.NgsResultsModel.State;
import rega.genotype.taxonomy.RegaSystemFiles;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.tools.blast.BlastTool;
import rega.genotype.utils.BlastUtil;
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
	private ToolConfig toolConfig;
	private NgsResultsTracer ngsResults;

	public NgsAnalysis(NgsResultsTracer ngsProgress, NgsModule ngsModule, ToolConfig toolConfig){
		this.workDir = ngsProgress.getWorkDir();
		this.ngsModule = ngsModule;
		this.toolConfig = toolConfig;
		this.ngsResults = ngsProgress;
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
		Preprocessing.trimomatic(ngsResults, ngsLogger);
	}

	/**
	 * Primary search: sort the short NGS sequences 
	 * By default use diamond blast
	 * Can be re-implemented.
	 * 
	 * @throws ApplicationException
	 */
	protected void primarySearch() throws ApplicationException {
		PrimarySearch.diamondSearch(ngsResults, ngsModule, ngsLogger);
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
		return Assemble.spadesAssemble(sequenceFile1, sequenceFile2, workDir, virusName, ngsModule, ngsLogger);
	}

	protected File assemble(File sequenceFile, String virusName) throws ApplicationException {
		return Assemble.spadesAssemble(sequenceFile, workDir, virusName, ngsModule, ngsLogger);
	}

	/**
	 * Contract long virus contigs from ngs output. 
	 * Steps:QC (FastQC), pre-processing (Trimmomatic),
	 * primary search (Diamond blast), assembly (Spades)
	 * @param workDir
	 */
	public boolean analyze() {
		// QC

		ngsResults.setStateStart(State.QC);

		boolean needPreprocessing = !ngsResults.getModel().getSkipPreprocessing();
		File fastqDir = NgsFileSystem.fastqDir(ngsResults);
		try {
			QC.qcReport(fastqDir.listFiles(),
					new File(workDir, NgsFileSystem.QC_REPORT_DIR),
					workDir);

			List<QcResults> qcresults = QC.getResults(new File(workDir, NgsFileSystem.QC_REPORT_DIR));
			for (QcResults qcr: qcresults) {
				if (qcr.adapterContent == Result.Fail){
					//needPreprocessing = true; TODO
				}
			}

			QcData qcData = new QC.QcData(QC.qcReportFile(workDir));
			ngsResults.getModel().setReadCountInit(qcData.getTotalNumberOfReads());
			ngsResults.getModel().setReadLength(qcData.getReadLength());
		} catch (ApplicationException e1) {
			e1.printStackTrace();
			ngsResults.printFatalError("QC failed: " + e1.getMessage());
			cleanBigData();
			return false;
		}

		ngsResults.getModel().setSkipPreprocessing(!needPreprocessing);
		ngsResults.setStateStart(State.Preprocessing);
		ngsResults.printQC1();

		// pre-process
		if (needPreprocessing) {
			try {
				preprocess();
				ngsResults.printPreprocessing();
			} catch (ApplicationException e) {
				e.printStackTrace();
				ngsResults.printFatalError("Preprocessing failed: " + e.getMessage());
				cleanBigData();
				return false;
			}

			ngsResults.setStateStart(State.QC2);

			// QC 2

			File[] qc2In;
			if (ngsResults.getModel().isPairEnd()) {
				File preprocessed1 = NgsFileSystem.preprocessedPE1(ngsResults);
				File preprocessed2 = NgsFileSystem.preprocessedPE2(ngsResults);
				qc2In = new File[] {preprocessed1, preprocessed2};
			} else {
				File preprocessed = NgsFileSystem.preprocessedSE(ngsResults);
				qc2In = new File[] {preprocessed};
			}
			try {
				QC.qcReport(qc2In, 
						new File(workDir, NgsFileSystem.QC_REPORT_AFTER_PREPROCESS_DIR),
						workDir);

				QcData qcData = new QC.QcData(QC.qcPreprocessedReportFile(workDir));
				ngsResults.getModel().setReadCountAfterPrepocessing(qcData.getTotalNumberOfReads());
				ngsResults.getModel().setReadLength(qcData.getReadLength());
				ngsResults.printQC2();
			} catch (ApplicationException e1) {
				e1.printStackTrace();
				ngsResults.printFatalError("QC failed: " + e1.getMessage());
				cleanBigData();
				return false;
			}
		} else {
			ngsResults.getModel().setReadCountAfterPrepocessing(ngsResults.getModel().getReadCountInit());
		}
		
		// diamond blast

		try {
			primarySearch();
			ngsResults.printfiltering();
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsResults.printFatalError("primary search failed: " + e.getMessage());
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
		File fastqDir = NgsFileSystem.fastqDir(ngsResults);
		// TODO: diamond results are useful only for testing 
		//File diamondDBDir = new File(workDir, NgsFileSystem.DIAMOND_BLAST_DIR);
		// delete all html files
		for (File f: workDir.listFiles())
			if (f.isFile() && f.getName().endsWith(".html"))
				f.delete();

		try {
			if (fastqDir.exists())
				FileUtils.deleteDirectory(fastqDir);
			if (preprocessedDir.exists())
				FileUtils.deleteDirectory(preprocessedDir);
//			if (diamondDBDir.exists())
//				FileUtils.deleteDirectory(diamondDBDir);
		} catch (IOException e) {
			e.printStackTrace();
			// leave it
		}
	}

	public BlastTool startIdentification(NgsResultsTracer ngsProgress) {
		BlastTool tool;
		try {
			boolean isHiv = toolConfig.getPath().equals("hiv");
			String blastXmlFileName = isHiv ? "hiv.xml" : Constants.BLAST_XML_FILE_NAME;
			tool = new BlastTool(toolConfig, workDir, blastXmlFileName);
			tool.setTracer(ngsProgress);
			tool.formatDB();
		} catch (IOException e) {
			e.printStackTrace();
			ngsProgress.printFatalError("Identification - failed to init blast tool: " + e. getMessage());
			return null;
		} catch (ParameterProblemException e) {
			e.printStackTrace();
			ngsProgress.printFatalError("Identification - failed to init blast tool: " + e. getMessage());
			return null;
		} catch (FileFormatException e) {
			e.printStackTrace();
			ngsProgress.printFatalError("Identification - failed to init blast tool: " + e. getMessage());
			return null;
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsProgress.printFatalError("Identification - failed to init blast tool: " + e. getMessage());
			return null;
		}
		return tool;
	}
	
	public boolean assembleAll() {
		BlastTool tool = startIdentification(ngsResults);
		if (tool == null)
			return false;

		ngsResults.setStateStart(State.Spades);
		ngsResults.printAssemblyOpen();
		// spades
		Lock jobLock = LongJobsScheduler.getInstance().getJobLock(workDir);

		try {
			File dimondResultDir = new File(workDir, NgsFileSystem.DIAMOND_RESULT_DIR);
			for (File d: dimondResultDir.listFiles()){
				assembleVirus(d, tool);
			}
		} finally {
			jobLock.release();
		}

		File sequences = new File(workDir, NgsFileSystem.CONTIGS_FILE);
		if (!sequences.exists())
			ngsResults.printFatalError("No assembly results.");
		else
			ngsResults.setStateStart(State.FinishedAll);

		ngsResults.printStop();

		return true;
	}

	public boolean assembleVirus(File virusDiamondDir, BlastTool tool) {
		if (!virusDiamondDir.isDirectory())
			return false;

		DiamondBucket basketData = ngsResults.getModel().getDiamondBlastResults().get(virusDiamondDir.getName());
		if (ngsModule.getMinReadsToStartAssembly() > basketData.getReadCountTotal())
			return false; // no need to assemble if there is not enough reads.

		try {
			long startAssembly = System.currentTimeMillis();
			File assembledFile;

			String fastqFileName;

			if (ngsResults.getModel().isPairEnd()) {
				String fastqPE1FileName = NgsFileSystem.fastqPE1(ngsResults).getName();
				String fastqPE2FileName = NgsFileSystem.fastqPE2(ngsResults).getName();
				fastqFileName = fastqPE1FileName;

				File sequenceFile1 = new File(virusDiamondDir, fastqPE1FileName);
				File sequenceFile2 = new File(virusDiamondDir, fastqPE2FileName);
				assembledFile = assemble(
						sequenceFile1, sequenceFile2, virusDiamondDir.getName());
			} else {
				fastqFileName = NgsFileSystem.fastqSE(ngsResults).getName();

				File sequenceFile = new File(virusDiamondDir, fastqFileName);
				assembledFile = assemble(
						sequenceFile, virusDiamondDir.getName());
			}
			if (assembledFile == null)
				return false;

			long endAssembly = System.currentTimeMillis();
			ngsLogger.info("assembled " + virusDiamondDir.getName() + " = " + (endAssembly - startAssembly) + " ms");

			File allContigsFile = new File(workDir, NgsFileSystem.CONTIGS_FILE);
			if (!allContigsFile.exists())
				try {
					allContigsFile.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
					ngsResults.printFatalError("assemble failed, could not create sequences.xml");
					return false;
				}
			// fill sequences.fasta'
			File sequencesFile = new File(workDir, Constants.SEQUENCES_FILE_NAME);
			if (!sequencesFile.exists())
				try {
					sequencesFile.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
					ngsResults.printFatalError("assemble failed, could not create sequences.xml");
					return false;
				}
			File allConsensusesFile = new File(workDir, NgsFileSystem.CONSENSUSES_FILE);
			if (!allConsensusesFile.exists())
				try {
					allConsensusesFile.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
					ngsResults.printFatalError("assemble failed, could not create sequences.xml");
					return false;
				}

			SequenceAlignment spadesContigsAlignment = new SequenceAlignment(new FileInputStream(assembledFile), SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);

			File ncbiVirusesFasta = RegaSystemFiles.ncbiVirusesFileAnnotated();
			if (!ncbiVirusesFasta.exists())
				throw new ApplicationException("Ncbi Viruses Db Path needs to be set in global settings");

			workDir.mkdirs();
			String virusName = virusDiamondDir.getName();
			File virusConsensusDir = NgsFileSystem.consensusDir(workDir, virusName);
			SequenceAlignment refs = detectRefs(virusConsensusDir, spadesContigsAlignment, ncbiVirusesFasta);

			// FIXME:
			//  - probably should change the cutoff for the alignment, relative to length?

			File consensusInputContigs = assembledFile;

			boolean identified = false; // only identified consensus contigs are passed on to the sub typing tool.
			for (AbstractSequence ref : refs.getSequences()) {
				ngsLogger.info("Trying with " + ref.getName() + " " + ref.getDescription());
				String refseqName = ref.getName().replaceAll("\\|", "_");
				File refWorkDir = NgsFileSystem.consensusRefSeqDir(virusConsensusDir, refseqName);
				
				File alingment = SequenceToolMakeConsensus.consensusAlign(consensusInputContigs, ref, refWorkDir, ngsModule, ngsLogger);
				File consensusFile = SequenceToolMakeConsensus.makeConsensus(alingment, refWorkDir, ngsModule, ngsLogger);
				File contigsFile = NgsFileSystem.consensusContigsFile(refWorkDir);

				consensusInputContigs = NgsFileSystem.consensusUnusedContigsFile(refWorkDir); // next time use only what was not used buy the first ref.

				// add virus taxonomy id to every consensus contig name, save sequence metadata.

				SequenceAlignment consensusAlignment = new SequenceAlignment(
						new FileInputStream(consensusFile), 
						SequenceAlignment.FILETYPE_FASTA, 
						SequenceAlignment.SEQUENCE_DNA);

				int i = 0;
				for (AbstractSequence s: consensusAlignment.getSequences()) {
					String[] split = fastqFileName.split("_");
					String fastqFileId = (split.length > 0) ? split[0] : fastqFileName;
					String refAC = "AC";
					if (refseqName.contains("_ref_"))
						refAC = refseqName.split("_ref_")[0];
					String bucket = virusName;

					String name = refAC + "__" + i + " " + s.getName();
					String description = fastqFileId;

					ConsensusBucket bucketData = new ConsensusBucket(bucket, ref.getName(), ref.getDescription(), 
							ref.getLength());

					List<Contig> contigs = SequenceToolMakeConsensus.readCotigsData(refWorkDir);

					ngsResults.printAssemblybucketOpen(bucketData, contigs);
					if (!contigs.isEmpty()){
						s.setName(name);
						identified = tool.analyzeBlast(s); // add consensus alignment to results file.
						ngsResults.finishCurrentSequence();
						i++;
					}
					ngsResults.printAssemblybucketClose();
				}

				ngsLogger.info("consensus " + virusDiamondDir.getName() + " = " + (System.currentTimeMillis() - endAssembly) + " ms");

				if(contigsFile.exists() && contigsFile.length() != 0) {
					FileUtil.appendToFile(contigsFile, allContigsFile);
					FileUtil.appendToFile(consensusFile, allConsensusesFile);
					if (identified)
						FileUtil.appendToFile(contigsFile, sequencesFile);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			ngsResults.printAssemblyError("assemble failed." + e.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * Detect for given genus a list of reference sequences based on long contigs.
	 * The sequences are stored in the consensus dir.
	 */
	private SequenceAlignment detectRefs(File virusDir,
			SequenceAlignment contigs, File ncbiVirusesFasta)
			throws ApplicationException, IOException, InterruptedException,
			ParameterProblemException, FileFormatException,
			FileNotFoundException {

		SequenceAlignment refs = new SequenceAlignment();
		final Map<String, Double> refNameScoreMap = new HashMap<String, Double>(); // make sure not to add same seq 2 times.

		for (AbstractSequence contig : contigs.getSequences()) {
			if (contig.getLength() < ngsModule.getRefMinContigLength())
				continue;

			//File consensusContigDir = NgsFileSystem.consensusContigDir(virusDir, contig.getName());
			File reference = NgsFileSystem.consensusRefFile(virusDir);
			virusDir.mkdirs();

			Double matchScore = BlastUtil.computeBestRefSeq(contig, virusDir,
					reference, ncbiVirusesFasta, ngsModule.getRefMaxBlastEValue(),
					ngsModule.getRefMinBlastBitScore(), ngsLogger);

			if (matchScore != null) {
				SequenceAlignment ref = new SequenceAlignment(new FileInputStream(reference),
						SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);
				AbstractSequence as = ref.getSequences().get(0);

				String bucketTxId = virusDir.getName().split("_")[0];
				String refTxId = RegaSystemFiles.taxonomyIdFromAnnotatedNcbiSeq(as.getDescription());

				if (refTxId != null && !bucketTxId.equals(TaxonomyModel.VIRUSES_TAXONOMY_ID)) {
					List<String> refAncestorTaxa = TaxonomyModel.getInstance().getHirarchyTaxonomyIds(refTxId);
					if (!refAncestorTaxa.contains(bucketTxId)){
						ngsLogger.info("Seq: " + as.getName() + " " + as.getDescription() + " not processed because bucket "
								+ bucketTxId + " is not ancestor - not in : " + Arrays.toString(refAncestorTaxa.toArray()));
						continue;
					}
				}
				Double prevScore = refNameScoreMap.get(as.getName());
				if (prevScore != null)
					refNameScoreMap.put(as.getName(), Math.max(prevScore, matchScore));
				else {
					refs.addSequence(as);
					refNameScoreMap.put(as.getName(), matchScore);
				}
			}
		}

		// order by best score.
		Collections.sort(refs.getSequences(), new Comparator<AbstractSequence>() {
			public int compare(AbstractSequence as1, AbstractSequence as2) {
				Double score1 = refNameScoreMap.get(as1.getName());
				Double score2 = refNameScoreMap.get(as2.getName());
				return -score1.compareTo(score2);
			}
		});

		return refs;
	}
}
