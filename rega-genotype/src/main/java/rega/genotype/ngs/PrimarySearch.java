package rega.genotype.ngs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import rega.genotype.ApplicationException;
import rega.genotype.FileFormatException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;
import rega.genotype.config.Config.GeneralConfig;
import rega.genotype.config.NgsModule;
import rega.genotype.framework.async.LongJobsScheduler;
import rega.genotype.framework.async.LongJobsScheduler.Lock;
import rega.genotype.ngs.NgsProgress.BasketData;
import rega.genotype.ngs.NgsProgress.State;
import rega.genotype.singletons.Settings;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.utils.LogUtils;

/**
 * NGS primary search: Order all the sequence to folders based on what 
 * virus the belong to.
 * 
 * @author michael and vagner
 */
public class PrimarySearch{
	private static class SequenceData {
		SequenceData(){}
		String taxon = null;
		Double score = null;
	}

	private static class TaxonomyNode {
		String taxonId = null;
		List<String> readNames = new ArrayList<String>();

		TaxonomyNode parentTaxon = null;
		List<TaxonomyNode> children = new ArrayList<TaxonomyNode>();

		TaxonomyNode(String taxonId) {
			this.taxonId = taxonId;
		}

		double level() {
			double l = 1;
			TaxonomyNode n = parentTaxon;
			while (n != null) {
				l++;
				n = n.parentTaxon;
			}
			return l;
		}
	}

	/**
	 * Order all the reads by taxonomy level. 
	 */
	public static class TaxonomyTree {
		TaxonomyNode root;
		TaxonomyNode find(String taxonId) {
			if (root == null)
				return null;
			return find(root, taxonId);
		}

		//Debug
		void printTree(TaxonomyNode node) {
			//if (node.children.size() > 0) TODO: fix to http://evolution.genetics.washington.edu/phylip/newicktree.html
				System.out.print("(");
			System.out.print(node.taxonId);
			int i = 0;
			for (TaxonomyNode c: node.children){
				if (i != 0)
					System.out.print(",");
				printTree(c);
				i++;
			}
			//if (node.children.size() > 0)
				System.out.print(")");
		}

		TaxonomyNode find(TaxonomyNode node, String taxonId) {
			if (node.taxonId.equals(taxonId))
				return node;

			for (TaxonomyNode c: node.children) {
				TaxonomyNode ans = find(c, taxonId);
				if (ans != null)
					return ans; 
			}

			return null;
		}

		TaxonomyNode add(TaxonomyNode parentNode, String taxonId) {
			if (parentNode.taxonId.equals(taxonId))
				System.err.println("ERROR parent child same !! " + taxonId);
			TaxonomyNode taxonomyNode = new TaxonomyNode(taxonId);
			taxonomyNode.parentTaxon = parentNode;
			parentNode.children.add(taxonomyNode);
			return taxonomyNode;
		}

		void add(String taxonId, String readName) {
			TaxonomyNode leafNode = find(taxonId);
			if (leafNode != null) {
				leafNode.readNames.add(readName);
				return;
			}

			List<String> hirarchyTaxonomyIds = TaxonomyModel.getInstance().getHirarchyTaxonomyIds(taxonId);
			if (hirarchyTaxonomyIds.size() == 0)
				return;

			if (root == null) {
				root = new TaxonomyNode(hirarchyTaxonomyIds.get(0));
			}

			if (taxonId.equals("10239")){ // viruses -> add to root.
				root.readNames.add(readName);
				return;
			}

			TaxonomyNode parentNode = null;
			// find lowest ancestor that is in the tree.
			for (int i = hirarchyTaxonomyIds.size() -1; i > -1; --i) {
				parentNode = find(hirarchyTaxonomyIds.get(i));
				if (parentNode != null) {
					if (parentNode.taxonId.equals(taxonId))
						System.err.println("ERROR parent child same !! " + taxonId);
					// go back and connect all nodes to parent.
					for (i++;i < hirarchyTaxonomyIds.size() - 1; ++i)
						parentNode = add(parentNode, hirarchyTaxonomyIds.get(i));
					break;
				}
			}

			TaxonomyNode taxonomyNode = add(parentNode, taxonId);
			taxonomyNode.readNames.add(readName);
		}

		void remove(TaxonomyNode node){
			node.parentTaxon.children.remove(node);
		}

		Map<String, String> createReadNameTaxonIdMap() {
			merge(root);
			Map<String, String> readNameTaxonIdMap = new HashMap<String, String>();
			fillReadNameTaxonIdMap(root, readNameTaxonIdMap);
			return readNameTaxonIdMap;
		}

		void fillDiamondResults(Map<String, BasketData> diamonResults, StringBuilder newickTree) {
			diamonResults.clear();
			fillDiamondResults(root, diamonResults, newickTree );
		}

		void fillDiamondResults(TaxonomyNode node, Map<String, BasketData> diamonResults, StringBuilder newickTree) {
			String scientificName = TaxonomyModel.getInstance().getScientificName(node.taxonId);
			String ancestors = TaxonomyModel.getInstance().getHirarchy(node.taxonId, 100);

			diamonResults.put(node.taxonId, new BasketData(
					scientificName, ancestors, node.readNames.size()));

			for (TaxonomyNode c: node.children)
				fillDiamondResults(c, diamonResults, newickTree);
		}

		void fillReadNameTaxonIdMap(TaxonomyNode parentTaxon, Map<String, String> readNameTaxonIdMap){
			for (String read: parentTaxon.readNames)
				readNameTaxonIdMap.put(read, parentTaxon.taxonId);

			for (TaxonomyNode c: parentTaxon.children)
				fillReadNameTaxonIdMap(c, readNameTaxonIdMap);
		}

		private int sumOfDescendants(TaxonomyNode node) {
			int ans = node.readNames.size();
			for(TaxonomyNode c: node.children)
				ans += sumOfDescendants(c);
			return ans;
		}

		private void collapse(TaxonomyNode node, List<String> reads) {
			while(!node.children.isEmpty())
				collapse(node.children.get(0), reads);

			reads.addAll(node.readNames);
			remove(node);
		}

		/**
		 * @param items Weighted Items
		 * @return random item with preference to the items with bigger weght
		 */
		private int chooseRandomallyFromWeightedItems(int[] itemWeights) {
			double totalWeight = 0.0d;
			for (int i : itemWeights)
			    totalWeight += i;

			int randomIndex = -1;
			double random = Math.random() * totalWeight;
			for (int i = 0; i < itemWeights.length; ++i){
			    random -= itemWeights[i];
			    if (random <= 0.0d){
			        randomIndex = i;
			        break;
			    }
			}
			return randomIndex;
		}

		private void combineAllToParent(TaxonomyNode parentTaxon) {
			List<String> descendantsReads = new ArrayList<String>();
			while(!parentTaxon.children.isEmpty()){
				collapse(parentTaxon.children.get(0), descendantsReads); // remove children and fill descendantsReads with there reads.
			}
			parentTaxon.readNames.addAll(descendantsReads);
		}

		/**
		 * Algorithm:
		 *    Run DFS on taxonomy tree:
		 *    if (node reads/ sum(descendants reads) < MERGE_CONDITION)
		 *    	re-sample parent randomly to all children based on children size.
		 *    else
		 *      combine all descendants reads to parent
		 */
		// input: diamond results: for every read find lowest common ancestor, add it to taxonomy tree.
		public void merge(TaxonomyNode parentTaxon) {
			double level = parentTaxon.level();
			double MERGE_CONDITION = 0.17 / level; 
			if (level >= 5) // dont split genus
				combineAllToParent(parentTaxon);
			else {
				int sumOfDescendants = sumOfDescendants(parentTaxon); // number of read for all the descendants together.
				double ratio = (double)parentTaxon.readNames.size() / (double)sumOfDescendants;
				if (ratio < MERGE_CONDITION) {
					// re-sample parent randomly to all children based on children size.
					int[] childrenWeights = new int[parentTaxon.children.size()];
					for (int i = 0; i < parentTaxon.children.size(); ++i)
						childrenWeights[i] = sumOfDescendants(parentTaxon.children.get(i));
					for (String read: parentTaxon.readNames) {
						int c = chooseRandomallyFromWeightedItems(childrenWeights);
						parentTaxon.children.get(c).readNames.add(read);
					}
					parentTaxon.readNames.clear();

					for (TaxonomyNode c: parentTaxon.children)
						merge(c);
				} else {
					combineAllToParent(parentTaxon);
				}
			}
		}
	}
	/**
	 * 
	 * @param workDir
	 * @param preprocessed1
	 * @param preprocessed2
	 * @return primary search results that will be used by NgsProgress
	 * @throws ApplicationException
	 */
	public static void diamondSearch(File workDir, NgsModule ngsModule, Logger logger) throws ApplicationException {
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
		matches = diamondBlastX(diamondDir, fastqMerge, ngsModule, logger);
		view = diamondView(diamondDir, matches, logger);
		jobLock.release();

		File resultDiamondDir = new File(workDir, NgsFileSystem.DIAMOND_RESULT_DIR);
		if (!(resultDiamondDir.exists())){
			resultDiamondDir.mkdirs();
		}
		try {
			File[] preprocessedFiles = {preprocessedPE1, preprocessedPE2};
			creatDiamondResults(resultDiamondDir, view,  preprocessedFiles, ngsProgress, logger);
		} catch (FileFormatException e) {
			throw new ApplicationException("diamond files could not be merged. " + e.getMessage(), e);
		} catch (IOException e) {
			throw new ApplicationException("diamond analysis failed: " + e.getMessage(), e);
		}
	}

	private static File diamondBlastX(File workDir, File query, NgsModule ngsModule, Logger logger) throws ApplicationException {
		File matches = new File(workDir.getAbsolutePath() + File.separator + "matches.daa");
		GeneralConfig gc = Settings.getInstance().getConfig().getGeneralConfig();
		File diamondDb = Settings.getInstance().getConfig().getDiamondBlastDb();
		if (diamondDb == null)
			throw new ApplicationException("Internal error: diamond blast db was not found. Ask your server admin to check that NGS Module is properlly configured.");

		String cmd = gc.getDiamondPath() + " blastx -d "
				+ diamondDb.getAbsolutePath() + " -q " + query.getAbsolutePath()
				+ " -a " + matches + " -k 1 --quiet "
				+ ngsModule.getDiamondOptions();

		NgsFileSystem.executeCmd(cmd, workDir, logger);

		return matches;
	}

	private static File diamondView(File workDir, File query, Logger logger) throws ApplicationException  {
		File matches = new File(workDir.getAbsolutePath() + File.separator + "matches.view");
		String cmd = Settings.getInstance().getConfig().getGeneralConfig().getDiamondPath() + " view -a " + query.getAbsolutePath()
				+ " -o " + matches +" --quiet";

		NgsFileSystem.executeCmd(cmd, workDir, logger);
		return matches;
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

	// TODO: we are still testing what will be the best way to bucket.
	private static Map<String, String> basketDiamondResultsBasedOnBestScore(File view, NgsProgress ngsProgress) throws NumberFormatException, IOException {
		String line = "";
		BufferedReader bf;
		bf = new BufferedReader(new FileReader(view.getAbsolutePath()));
		// sequences with best score per taxon counters.
		Map<String, BasketData> taxonCounters = new HashMap<String, BasketData>();
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
					Integer counter = taxonCounters.get(prevBestTaxon) == null ? null :
							taxonCounters.get(prevBestTaxon).getReadCountTotal();
					if (counter == null)
						counter = 0;
					counter++;
					taxonCounters.put(prevBestTaxon, new BasketData("", "", counter));
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

		ngsProgress.setDiamondBlastResults(taxonCounters);

		// Step2: find genus taxonomy for every sequence. 
		Map<String, String> taxoNameId = new HashMap<String, String>();
		for (Map.Entry<String, List<SequenceData>> s: sequenceNameData.entrySet()) {
			SequenceData bestScore = s.getValue().get(0);
 			for (SequenceData d: s.getValue()) {
				if (d.score > bestScore.score)
					bestScore = d;
 			}

			SequenceData best = s.getValue().get(0);
			Integer bestScoreTaxonCounter = taxonCounters.get(bestScore.taxon).getReadCountTotal();
			for (SequenceData d: s.getValue()) {
				if (taxonCounters.get(d.taxon) != null){
					Integer currentTaxonCounter = taxonCounters.get(d.taxon).getReadCountTotal();
					if (currentTaxonCounter != null 
							&& bestScore.score - d.score < 5 
							&& currentTaxonCounter > bestScoreTaxonCounter + 10000)
						best = d;// big basket gets priority.
				}
			}
			taxoNameId.put(s.getKey(), best.taxon);
		}
		return taxoNameId;
	}

	/**
	 * First step basket by the result of every read by lowest common ancestor.
	 */
	private static TaxonomyTree basketDiamondResultsLowCommonAncestorold(File view) throws NumberFormatException, IOException {
		String line = "";
		BufferedReader bf;
		bf = new BufferedReader(new FileReader(view.getAbsolutePath()));
		// All the data per sequence
		TaxonomyTree taxonomyTree = new TaxonomyTree();

		String prevName = null;
		String prevTaxon = null;
		String lowestCommonAncestor = null;
		while ((line = bf.readLine()) != null)  {
			String[] name = line.split("\\t");
			String[] taxon = name[1].split("_");

			String taxonId = taxon[0];

			if (prevName == null || !prevName.equals(name[0])) { // new read
				if (prevName != null) { // not first time.
					taxonomyTree.add(lowestCommonAncestor, prevName);
				}

				prevName = name[0];
				lowestCommonAncestor = taxonId;
			} else { // continue analyzing more output for same read.
				if (lowestCommonAncestor == null)
					lowestCommonAncestor = taxonId;
				lowestCommonAncestor = TaxonomyModel.getInstance().getLowesCommonAncestor(
						taxonId, lowestCommonAncestor);
			}
			prevTaxon = taxonId;
		}
		bf.close();

		return taxonomyTree;
	}

	public static TaxonomyTree basketDiamondResultsLowCommonAncestor(File view, File workDir) throws NumberFormatException, IOException {
		String line = "";
		BufferedReader bf;
		bf = new BufferedReader(new FileReader(view.getAbsolutePath()));
		// All the data per sequence
		TaxonomyTree taxonomyTree = new TaxonomyTree();

		Logger log = LogUtils.createDebugLogger(workDir);
		String prevName = null;
		Set<String> readTaxa = new HashSet<String>();
		while ((line = bf.readLine()) != null)  {
			String[] name = line.split("\\t");
			String[] taxon = name[1].split("_");

			String taxonId = taxon[0];
			String readName = name[0];

			if (prevName != null && !prevName.equals(readName)) {
				// restart for next read.
				String lowestAncestor = lowestAnsestorOnLinage(readTaxa);
				StringBuilder readTaxaDebug = new StringBuilder();
				for (String t:readTaxa){
					readTaxaDebug.append(t + " " + TaxonomyModel.getInstance().getHirarchy(t, 100) + "\n");
				}
				//log.info("Set: " + Arrays.toString(readTaxa.toArray()));
				log.info(readTaxaDebug.toString());
				log.info("lowestAncestor = " + lowestAncestor + "\n");
				taxonomyTree.add(lowestAncestor, prevName);
				readTaxa.clear();
			}

			readTaxa.add(taxonId);

			prevName = readName;
		}
		bf.close();

		return taxonomyTree;
	}

	private static String lowestAnsestor(Set<String> readTaxa) {
		String lowestCommonAncestor = null;
		for (String taxonId: readTaxa) {
			if (lowestCommonAncestor == null)
				lowestCommonAncestor = taxonId;
			else
				lowestCommonAncestor = TaxonomyModel.getInstance().getLowesCommonAncestor(
						taxonId, lowestCommonAncestor);
		}

		return lowestCommonAncestor;
	}

	// TODO: document or rename
	public static String lowestAnsestorOnLinage(Set<String> readTaxa) {
		TaxonomyTree taxonomyTree = new TaxonomyTree();
		for (String taxonId: readTaxa) 
			taxonomyTree.add(taxonId, "");

		//taxonomyTree.printTree(taxonomyTree.root);
		removeUnclassifiedNode(taxonomyTree.root); // remove only top lovel unclassified.

		//if (taxonomyTree.root.taxonId.equals(TaxonomyModel.VIRUSES_TAXONOMY_ID))
		//	removeUnclassifiedNode(taxonomyTree.root); // remove only top lovel unclassified.

		//return taxonomyTree.root.taxonId;
		return removeTopLevel1ChildParent(taxonomyTree.root).taxonId;
	}

	private static boolean checkUnclassified(TaxonomyNode node) {
		return TaxonomyModel.getInstance().getScientificName(node.taxonId).startsWith("unclassified")
				|| TaxonomyModel.getInstance().getScientificName(node.taxonId).startsWith("unassigned");
	}

	private static void removeUnclassifiedNode(TaxonomyNode node) {
		if (node.children.size() > 1){
			List<TaxonomyNode> unclassified = new ArrayList<TaxonomyNode>();
			for (TaxonomyNode c: node.children)
				if(checkUnclassified(c))
					unclassified.add(c);
			if (unclassified.size() != node.children.size()) // all unclassified -> leave it
				for (TaxonomyNode u: unclassified)
					node.children.remove(u);
		}

		for (TaxonomyNode c: node.children)
			removeUnclassifiedNode(c);
	}

	public static TaxonomyNode removeTopLevel1ChildParent(TaxonomyNode root) {
		TaxonomyNode newRoot = root;
		while(newRoot.children.size() == 1)
			newRoot = newRoot.children.get(0);

		return newRoot;
		//return root.children.size() != 1 ? root : removeTopLevel1ChildParent(root.children.get(0));
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
	private static void creatDiamondResults(File diamondResultsDir, File view, File[] fastqFiles,
			NgsProgress ngsProgress, Logger logger) throws FileFormatException, IOException {
		long start = System.currentTimeMillis();
		TaxonomyTree taxonomyTree = basketDiamondResultsLowCommonAncestor(view, diamondResultsDir.getParentFile());
		StringBuilder newickTreeBeforeMerge = new StringBuilder(); // TODO ..
		taxonomyTree.fillDiamondResults(ngsProgress.getDiamondBlastResultsBeforeMerge(), newickTreeBeforeMerge);
		
		Map<String, String> readIdTaxonomyId = taxonomyTree.createReadNameTaxonIdMap();
		StringBuilder newickTreeAfterMerge = new StringBuilder();
		taxonomyTree.fillDiamondResults(ngsProgress.getDiamondBlastResults(), newickTreeAfterMerge);

//		Map<String, String> readIdTaxonomyId = basketDiamondResultsBasedOnBestScore(view, ngsProgress);

		logger.info("Basket reads time = " + (System.currentTimeMillis() - start) + " ms");

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

				String taxosId = readIdTaxonomyId.get(name[0]);
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

		ngsProgress.save(diamondResultsDir.getParentFile());
	}
}
