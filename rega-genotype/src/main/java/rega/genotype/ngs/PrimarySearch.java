package rega.genotype.ngs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.Config.GeneralConfig;
import rega.genotype.config.NgsModule;
import rega.genotype.framework.async.LongJobsScheduler;
import rega.genotype.framework.async.LongJobsScheduler.Lock;
import rega.genotype.ngs.NgsProgress.State;
import rega.genotype.singletons.Settings;
import rega.genotype.utils.LogUtils;
import rega.genotype.utils.StreamReaderRuntime;

/**
 * NGS primary search: Order all the sequence to folders based on what 
 * virus the belong to.
 * 
 * @author michael and vagner
 */
public class PrimarySearch{
	/**
	 * 
	 * @param workDir
	 * @param preprocessed1
	 * @param preprocessed2
	 * @return primary search results that will be used by NgsProgress
	 * @throws ApplicationException
	 */
	public static void diamondSearch(File workDir, NgsModule ngsModule) throws ApplicationException {
		File preprocessedPE1 = NgsFileSystem.preprocessedPE1(workDir);
		File preprocessedPE2 = NgsFileSystem.preprocessedPE2(workDir);

		File diamondDir = new File(workDir, NgsFileSystem.DIAMOND_BLAST_DIR);
		if (!(diamondDir.exists())){
			diamondDir.mkdirs();
		}
		File fastqMerge = null;
		try {
			fastqMerge = megerFiles(diamondDir, preprocessedPE1, preprocessedPE2);
		} catch (IOException e) {
			throw new ApplicationException("diamond files could not be merged. " + e.getMessage(), e);
		} catch (FileFormatException e) {
			throw new ApplicationException("diamond files could not be merged. " + e.getMessage(), e);
		} 

		NgsProgress ngsProgress = NgsProgress.read(workDir);
		ngsProgress.setState(State.Diamond);
		ngsProgress.save(workDir);

		File matches = null;
		File view = null;

		Lock jobLock = LongJobsScheduler.getInstance().getJobLock(workDir);
		matches = diamondBlastX(diamondDir, fastqMerge, ngsModule);
		view = diamondView(diamondDir, matches);
		jobLock.release();

		File resultDiamondDir = new File(workDir, NgsFileSystem.DIAMOND_RESULT_DIR);
		if (!(resultDiamondDir.exists())){
			resultDiamondDir.mkdirs();
		}
		try {
			File[] preprocessedFiles = {preprocessedPE1, preprocessedPE2};
			creatDiamondResults(resultDiamondDir, view,  preprocessedFiles, ngsProgress);
		} catch (FileFormatException e) {
			throw new ApplicationException("diamond files could not be merged. " + e.getMessage(), e);
		} catch (IOException e) {
			throw new ApplicationException("diamond analysis failed: " + e.getMessage(), e);
		}
	}

	private static File diamondBlastX(File workDir, File query, NgsModule ngsModule) throws ApplicationException {
		Process blastx = null;
		File matches = new File(workDir.getAbsolutePath() + File.separator + "matches.daa");
		try {
			GeneralConfig gc = Settings.getInstance().getConfig().getGeneralConfig();
			File diamondDb = Settings.getInstance().getConfig().getDiamondBlastDb();
			if (diamondDb == null)
				throw new ApplicationException("Internal error: diamond blast db was not found. Ask your server admin to check that NGS Module is properlly configured.");

			String cmd = gc.getDiamondPath() + " blastx -d "
					+ diamondDb.getAbsolutePath() + " -q " + query.getAbsolutePath()
					+ " -a " + matches + " -k 1 --quiet "
					+ ngsModule.getDiamondOptions();

			LogUtils.getLogger(workDir).info(cmd);
			blastx = StreamReaderRuntime.exec(cmd, null, workDir);
			int exitResult = blastx.waitFor();

			if (exitResult != 0) {
				throw new ApplicationException("blastx exited with error: "
						+ exitResult);
			}
			return matches;
		} catch (FileNotFoundException e) {
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		} catch (IOException e) {
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		} catch (InterruptedException e) {
			if (blastx != null)
				blastx.destroy();
			throw new ApplicationException("blastx failed error: "
					+ e.getMessage(), e);
		}
	}

	private static File diamondView(File workDir, File query) throws ApplicationException {
		File matches = new File(workDir.getAbsolutePath() + File.separator + "matches.fasta");
		Process diamond = null;
		try {
			String cmd = Settings.getInstance().getConfig().getGeneralConfig().getDiamondPath() + " view -a " + query.getAbsolutePath()
					+ " -o " + matches +" --quiet";
			LogUtils.getLogger(workDir).info(cmd);
			diamond = StreamReaderRuntime.exec(cmd, null, workDir);
			int exitResult = diamond.waitFor();

			if (exitResult != 0) {
				throw new ApplicationException("blastx exited with error: "
						+ exitResult);
			}
			return matches;
		} catch (IOException e) {
			throw new ApplicationException(
					": " + e.getMessage());
		} catch (InterruptedException e) {
			if (diamond != null)
				diamond.destroy();
			throw new ApplicationException(": "
					+ e.getMessage(), e);
		}
	}

	private static File megerFiles(File workDir, File pe1, File pe2) throws IOException, FileFormatException, ApplicationException {
		File result = new File(workDir.getAbsolutePath() + File.separator +"query.fna");
		FileWriter megerFile = new FileWriter(result);
		PrintWriter saveFile = new PrintWriter(megerFile);
		FileReader fr1 = new FileReader(pe1.getAbsolutePath());
		LineNumberReader lnr1 = new LineNumberReader(fr1);
		FileReader fr2 = new FileReader(pe2.getAbsolutePath());
		LineNumberReader lnr2 = new LineNumberReader(fr2);
		while (true){
			Sequence s1 = SequenceAlignment.readFastqFileSequence(lnr1, SequenceAlignment.SEQUENCE_DNA);
			Sequence s2 = SequenceAlignment.readFastqFileSequence(lnr2, SequenceAlignment.SEQUENCE_DNA);
			if (s1 == null || s2 == null){
				if (s1 != null || s2 != null) {
					megerFile.close();
					saveFile.close();
					throw new ApplicationException("Fastq files different");
				}
				break;
			}
			String[] name1 = s1.getName().split(" ");
			String[] name2 = s2.getName().split(" ");
			if (name1[0].equalsIgnoreCase(name2[0])){
				saveFile.println("@" + s1.getName());
				saveFile.println(s1.getSequence() + s2.getSequence());
				saveFile.println("+");
				saveFile.println(s1.getQuality() + s2.getQuality());
			}
		}
		saveFile.close();
		megerFile.close();
		return result;
	}

	/**
	 * Order all the sequence by taxonomy id. 
	 * The algorithm favors creating big baskets, so if 1 taxon appears many times 
	 * a new sequence would rather go to its basket then to a new taxon basket
	 * even if the new 1 has slightly better score.
	 * @param diamondResultsDir
	 * @param view
	 * @param fastqFiles
	 * @param ngsProgress 
	 * @throws FileFormatException
	 * @throws IOException
	 */
	private static void creatDiamondResults(File diamondResultsDir, File view, File[] fastqFiles, NgsProgress ngsProgress) throws FileFormatException, IOException {
		String line = "";

		class SequenceData {
			String taxon;
			double score;
		}
		BufferedReader bf;
		bf = new BufferedReader(new FileReader(view.getAbsolutePath()));
		// sequences with best score per taxon counters.
		Map<String, Integer> taxonCounters = new HashMap<String, Integer>();
		// All the data per sequence
		Map<String, List<SequenceData>> sequenceNameData = new HashMap<String, List<SequenceData>>();

		String prevName = null;
		String prevBestTaxon = null;
		Double prevBestScore = null;
		while ((line = bf.readLine()) != null)  {
			String[] name = line.split("\\t");
			String[] taxon = name[1].split("_");

			String score = name[2];

			SequenceData data = new SequenceData();
			data.score = Double.parseDouble(score);
			data.taxon = taxon[0] + "_" + taxon[1];

			List<SequenceData> sequenceDataList = sequenceNameData.get(name[0]);
			if (sequenceDataList == null) {
				sequenceDataList = new ArrayList<SequenceData>();
				sequenceNameData.put(name[0], sequenceDataList);
			}
			sequenceDataList.add(data);

			if (prevName == null || !prevName.equals(name[0])) {
				if (prevName != null) { // not first time.
					Integer counter = taxonCounters.get(prevBestTaxon);
					if (counter == null)
						counter = 0;
					counter++;
					taxonCounters.put(prevBestTaxon, counter);
				}

				prevName = name[0];
				prevBestTaxon = data.taxon;
				prevBestScore = data.score;
			} else {
				if (data.score > prevBestScore) {
					prevBestTaxon = data.taxon;
					prevBestScore = data.score;
				}
			}
		}
		bf.close();

		// find genus taxonomy for every sequence. 
		Map<String, String> taxoNameId = new HashMap<String, String>();
		for (Map.Entry<String, List<SequenceData>> s: sequenceNameData.entrySet()) {
			SequenceData bestScore = s.getValue().get(0);
			for (SequenceData d: s.getValue()) {
				if (d.score > bestScore.score)
					bestScore = d;
			}

			SequenceData best = s.getValue().get(0);
			Integer bestScoreTaxonCounter = taxonCounters.get(bestScore.taxon);
			for (SequenceData d: s.getValue()) {
				Integer currentTaxonCounter = taxonCounters.get(d.taxon);
				if (currentTaxonCounter != null 
						&& bestScore.score - d.score < 5 
						&& currentTaxonCounter > bestScoreTaxonCounter + 10000)
					best = d;// big basket gets priority.
			}
			taxoNameId.put(s.getKey(), best.taxon);
		}

		// Order fastq sequences in basket per taxon.
		for(File f : fastqFiles){
			FileReader fileReader = new FileReader(f.getAbsolutePath());
			LineNumberReader lnr = new LineNumberReader(fileReader);
			while(true){
				Sequence s = SequenceAlignment.readFastqFileSequence(lnr, SequenceAlignment.SEQUENCE_DNA);
				if (s == null){
					break;
				}

				String[] name = s.getName().split(" ");

				String taxosId = taxoNameId.get(name[0]);
				if (taxosId == null)
					continue; // TODO ??
				File taxonDir = new File(diamondResultsDir, taxosId);
				taxonDir.mkdirs();

				FileWriter fastq = new FileWriter(taxonDir.getAbsoluteFile() + File.separator + f.getName(), true);
				PrintWriter saveFastq = new PrintWriter(fastq);

				saveFastq.println("@" + s.getName());
				saveFastq.println(s.getSequence());
				saveFastq.println(s.getDescription());
				saveFastq.println(s.getQuality());

				fastq.close();
			}
			fileReader.close();
			lnr.close();
		}

		ngsProgress.setDiamondBlastResults(taxonCounters);
		ngsProgress.save(diamondResultsDir.getParentFile());
	}

}
