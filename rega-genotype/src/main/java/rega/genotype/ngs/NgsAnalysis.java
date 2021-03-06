package rega.genotype.ngs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

	private enum AssemblyState {AssembledAndIdentified, Assembled, Failed, }
	
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

		if (cancelAnalysis())
			return false;

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

				if (cancelAnalysis())
					return false;

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


			if (cancelAnalysis())
				return false;

			File[] qc2In;
			if (ngsResults.getModel().isPairEnd()) {
				File preprocessed1 = NgsFileSystem.preprocessedPE1(workDir);
				File preprocessed2 = NgsFileSystem.preprocessedPE2(workDir);
				qc2In = new File[] {preprocessed1, preprocessed2};
			} else {
				File preprocessed = NgsFileSystem.preprocessedSE(workDir);
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

			if (cancelAnalysis())
				return false;

			primarySearch();
			ngsResults.printfiltering();
		} catch (ApplicationException e) {
			e.printStackTrace();
			ngsResults.printFatalError("primary search failed: " + e.getMessage());
			cleanBigData();
			return false;
		}

		if (cancelAnalysis())
			return false;

		boolean ans = assembleAll();

		cleanBigData();
		return ans;
	}

	/**
	 * Delete large ngs files from work dir.
	 */
	public void cleanBigData() {
		/*
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
		}*/
	}

	protected boolean cancelAnalysis() {
		return new File(workDir, ".CANCEL").exists();
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
			if (dimondResultDir.listFiles() != null) { // Could be that diamond filtered all.
				int n = 0;
				for (File d: dimondResultDir.listFiles()) {
					assembleVirus(d, tool);
					System.err.println("assemsbled = " + n + " from " + dimondResultDir.listFiles().length);
					n++;

					if (cancelAnalysis())
						return false;
				}
			}
		} finally {
			jobLock.release();
			ngsLogger.info("Assembly lock was released.");
		}

		File sequences = new File(workDir, NgsFileSystem.CONTIGS_FILE);
		if (!sequences.exists())
			ngsResults.printFatalError("No assembly results.");
		else
			ngsResults.setStateStart(State.FinishedAll);

		ngsResults.printStop();

		return true;
	}

	public AssemblyState assembleVirus(File virusDiamondDir, BlastTool tool) {
		if (!virusDiamondDir.isDirectory())
			return AssemblyState.Failed;

		DiamondBucket basketData = ngsResults.getModel().getDiamondBlastResults().get(virusDiamondDir.getName());
		if (ngsModule.getMinReadsToStartAssembly() > basketData.getReadCountTotal())
			return AssemblyState.Failed; // no need to assemble if there is not enough reads.

		boolean identified = false; // only identified consensus contigs are passed on to the sub typing tool.

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
				return AssemblyState.Failed;

			long endAssembly = System.currentTimeMillis();
			ngsLogger.info("assembled " + virusDiamondDir.getName() + " = " + (endAssembly - startAssembly) + " ms");

			File allContigsFile = new File(workDir, NgsFileSystem.CONTIGS_FILE);
			if (!allContigsFile.exists())
				try {
					allContigsFile.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
					ngsResults.printFatalError("assemble failed, could not create sequences.xml");
					return AssemblyState.Failed;
				}
			// fill sequences.fasta'
			File sequencesFile = new File(workDir, Constants.SEQUENCES_FILE_NAME);
			if (!sequencesFile.exists())
				try {
					sequencesFile.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
					ngsResults.printFatalError("assemble failed, could not create sequences.xml");
					return AssemblyState.Failed;
				}
			File allConsensusesFile = new File(workDir, NgsFileSystem.CONSENSUSES_FILE);
			if (!allConsensusesFile.exists())
				try {
					allConsensusesFile.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
					ngsResults.printFatalError("assemble failed, could not create sequences.xml");
					return AssemblyState.Failed;
				}

			SequenceAlignment spadesContigsAlignment = new SequenceAlignment(new FileInputStream(assembledFile), SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);

			File ncbiVirusesFasta = RegaSystemFiles.ncbiVirusesFileAnnotated();
			if (!ncbiVirusesFasta.exists())
				throw new ApplicationException("Ncbi Viruses Db Path needs to be set in global settings");

			workDir.mkdirs();
			String virusName = virusDiamondDir.getName();
			File virusConsensusDir = NgsFileSystem.consensusDir(workDir, virusName);
			SequenceAlignment refs = detectRefs(virusConsensusDir, spadesContigsAlignment, ncbiVirusesFasta);

			File consensusInputContigs = assembledFile;

			// <Contig, best alignment score for the contig>
			Map<String, Double> bestScoresMap = new HashMap<String, Double>();

			// Consensus alignments all.
			for (AbstractSequence ref : refs.getSequences()) {
				File refWorkDir = refWorkDir(virusConsensusDir, ref);
				File alingmentFile = SequenceToolMakeConsensus.consensusAlign(
						consensusInputContigs, ref, refWorkDir, ngsModule, ngsLogger);

				// Add score to best scores.
				SequenceAlignment alignment = new SequenceAlignment(alingmentFile);
				for (AbstractSequence s: alignment.getSequences()) {
					if (getValue(s.getName(), "score") == null)
						continue; // reference sequence name is empty
					Double score = Double.parseDouble(getValue(s.getName(), "score"));
					Double bestScore = bestScoresMap.get(makeKey(s.getName()));
					if (bestScore == null || score > bestScore)
						bestScoresMap.put(makeKey(s.getName()), score);
				}
			}

			// leave only sequences that had best score in this alignment. (So the same sequence is not included in 2 alignments)
			for (AbstractSequence ref : refs.getSequences()) {
				File refWorkDir = refWorkDir(virusConsensusDir, ref);
				// read alignment
				File alingmentFile = NgsFileSystem.consensusAlingmentFile(refWorkDir);

				SequenceAlignment alignment = new SequenceAlignment();

				SequenceAlignment fullAlignment = new SequenceAlignment(alingmentFile);
				for (AbstractSequence s: fullAlignment.getSequences()) {
					if (getValue(s.getName(), "score") == null) {
						if (s.getName().isEmpty())
							alignment.addSequence(s);
						continue; // reference sequence name is empty
					}
					Double score = Double.parseDouble(getValue(s.getName(), "score"));
					Double bestScore = bestScoresMap.get(makeKey(s.getName()));
					if (score.equals(bestScore))
						alignment.addSequence(s);
				}
				alingmentFile.delete();
				alingmentFile.createNewFile();
				alignment.writeOutput(alingmentFile);
			}

			// make consensus for all
			for (AbstractSequence ref : refs.getSequences()) {
				File refWorkDir = refWorkDir(virusConsensusDir, ref);
				File consensusFile = NgsFileSystem.consensusFile(refWorkDir);
				File contigsFile = NgsFileSystem.consensusContigsFile(refWorkDir);

				File alingmentFile = NgsFileSystem.consensusAlingmentFile(refWorkDir);

				SequenceToolMakeConsensus.makeConsensus(alingmentFile, refWorkDir, ngsModule, ngsLogger);

				//consensusInputContigs = NgsFileSystem.consensusUnusedContigsFile(refWorkDir); // next time use only what was not used buy the first ref.

				// add virus taxonomy id to every consensus contig name, save sequence metadata.

				SequenceAlignment consensusAlignment = new SequenceAlignment(consensusFile);

				for (AbstractSequence s: consensusAlignment.getSequences()) {
					String bucket = virusName;
					ConsensusBucket bucketData = new ConsensusBucket(
							bucket, ref.getName(), ref.getDescription(), ref.getLength());

					List<Contig> contigs = SequenceToolMakeConsensus.readCotigsData(refWorkDir);

					ngsResults.printAssemblybucketOpen(bucketData, contigs);
					if (!contigs.isEmpty()) {
						identified = tool.analyzeBlast(s, contigsFile); // add consensus alignment to results file.
						ngsResults.finishCurrentSequence();
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
			ngsResults.printAssemblyError("Bucket " + virusDiamondDir.getName() 
					+ ". assemble failed." + e.getMessage());
			return AssemblyState.Failed;
		}

		return identified ? AssemblyState.AssembledAndIdentified : AssemblyState.Assembled;
	}

	private String makeKey(String sequenceName) {
		return sequenceName.substring(0, 20); // trim the score.
	}

	private String getValue(String sequenceName, String var) {
		String[] split = sequenceName.split("_");
		for (int i = 0; i < split.length; ++i) {
			if (split[i].equals(var))
				return split[i+1];
		}

		return null;
	}

	private File refWorkDir(File virusConsensusDir, AbstractSequence ref) {
		String refseqName = ref.getName().replaceAll("\\|", "_");
		File refWorkDir = NgsFileSystem.consensusRefSeqDir(virusConsensusDir, refseqName);
		return refWorkDir;
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
			File blastResultRefsFile = NgsFileSystem.consensusRefFile(virusDir);
			virusDir.mkdirs();

			Double matchScores = BlastUtil.computeBestRefSeq(contig, virusDir,
					blastResultRefsFile, ncbiVirusesFasta, ngsModule.getRefMaxBlastEValue(),
					ngsModule.getRefMinBlastBitScore(), ngsLogger);

			if (matchScores != null) {
				SequenceAlignment blastResultRefs = new SequenceAlignment(new FileInputStream(blastResultRefsFile),
						SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);
				if (blastResultRefs.getSequences().size() > 0) {
					AbstractSequence ref = blastResultRefs.getSequences().get(0);

					if (contig.getLength() > ref.getLength())
						continue; // contig can not be longer then ref!

					String bucketTxId = virusDir.getName().split("_")[0];
					String refTxId = RegaSystemFiles.taxonomyIdFromAnnotatedNcbiSeq(ref.getDescription());

					if (refTxId != null && !bucketTxId.equals(TaxonomyModel.VIRUSES_TAXONOMY_ID)) {
						List<String> refAncestorTaxa = TaxonomyModel.getInstance().getHirarchyTaxonomyIds(refTxId);
						if (!refAncestorTaxa.contains(bucketTxId)){
							ngsLogger.info("Seq: " + ref.getName() + " " + ref.getDescription() + " not processed because bucket "
									+ bucketTxId + " is not ancestor - not in : " + Arrays.toString(refAncestorTaxa.toArray()));
							continue;
						}
					}
					Double prevScore = refNameScoreMap.get(ref.getName());
					Double currentScore = matchScores;//matchScores.get(as.getName());
					if (prevScore != null)
						refNameScoreMap.put(ref.getName(), prevScore + currentScore);
					else {
						// check that contig will path also sequencetool test
						if (checkAlignment(ref, contig)) {
							refs.addSequence(ref);
							refNameScoreMap.put(ref.getName(), currentScore);
						}
					}
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

	/** check that contig will path also sequencetool test */
	private boolean checkAlignment(AbstractSequence ref, AbstractSequence contig) {
		SequenceAlignment assembledContigs = new SequenceAlignment();
		assembledContigs.addSequence(contig);
		File tmpDir = new File(workDir, "tmp");
		tmpDir.mkdirs();
		File assembledContigsFile = new File(tmpDir, "contig.fasta");
		try {
			assembledContigsFile.createNewFile();
			assembledContigs.writeOutput(new FileOutputStream(assembledContigsFile), 
					SequenceAlignment.FILETYPE_FASTA);
			File consensusFile = SequenceToolMakeConsensus.consensusAlign(
					assembledContigsFile, ref, tmpDir, ngsModule, ngsLogger);
			
			SequenceAlignment ans = new SequenceAlignment(new FileInputStream(consensusFile),
					SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);
			return ans.getLength() > 1;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (ParameterProblemException e) {
			e.printStackTrace();
			return false;
		} catch (ApplicationException e) {
			e.printStackTrace();
			return false;
		} catch (FileFormatException e) {
			e.printStackTrace();
			return false;
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}
}
