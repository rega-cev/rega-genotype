package rega.genotype.ui.admin.file_editor.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rega.genotype.AbstractSequence;
import rega.genotype.AlignmentAnalyses;
import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.AlignmentAnalyses.Taxus;
import rega.genotype.ApplicationException;
import rega.genotype.BlastAnalysis;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;
import rega.genotype.singletons.Settings;
import rega.genotype.taxonomy.RegaSystemFiles;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.ExcelUtils;
import rega.genotype.utils.FileUtil;
import eu.webtoolkit.jwt.Signal1;

/**
 * Auto generate blast.xml for pan-viral tool from "ICTV Master Species List"
 * (download from https://talk.ictvonline.org/files/master-species-lists/m/msl/5945)
 * and a fasta file that was created by downloading fasta sequences for all
 * accession numbers in the list.
 * 
 * Note you can use  http://www.ncbi.nlm.nih.gov/sites/batchentrez to create the fasta file.
 * 
 * The pan-viral tool can be auto created/updated from ICTV master list using the next steps:
 * - Query all the sequences by accession numbers from NCBI.
 * - Create a cluster per organism. (Organism taxonomy id is also queried from NCBI)
 * - Annotate hierarchy (family__genus__species) as specified in ICTV master list.
 * 
 * @author michael
 */
public class PanViralToolGenerator {

	private static final int OrderCol = 0;
	private static final int FamilyCol = 1;
	private static final int SubfamilyCol = 2;
	private static final int GenusCol = 3;
	private static final int SpeciesCol = 4;
	private static final int TypeSpeciesCol = 5;
	private static final int ExemplarAccessionNumberCol = 6;
	private static final int ExemplarIsolateCol = 7;	
	private static final int GenomeCompositionCol = 8;	
	private static final int LastChangeCol = 9;
	private static final int MSLofLastChangeCol = 10;	
	private static final int ProposalCol = 11;	
	private static final int TaxonHistoryURCol = 12;
	
	public static final String AUTO_CREATE_BLAST_XML_DIR = "auto-create-blast-xml";
	public static final String FASTA_DESCRIPTION_SEPARATOR = "__";
	
	class Data {
		String description;
		String taxonomyId;
		private String organizedName;
		Data(String description, String taxonomyId, String organizedName){
			this.description = description;
			this.taxonomyId = taxonomyId;
			this.organizedName = organizedName;
		}
	}

	// <AccessionNumber, data (description)> 
	private Map<String, Data> accessionNumMapICTV = new HashMap<String, Data>(); 
	private Signal1<AlignmentAnalyses> finished = new Signal1<AlignmentAnalyses>();
	
	public AlignmentAnalyses createAlignmentAnalyses(File ictvMasterSpeciesListFile) throws ApplicationException, IOException, InterruptedException, ParameterProblemException, FileFormatException {

		File ncbiVirusesDb = RegaSystemFiles.ncbiVirusesFile();
		if (!ncbiVirusesDb.exists())
			throw new ApplicationException("NCBI viruses file was not found. Ask admin to update global config.");

		File workDir = FileUtil.createTempDirectory("tool-dir", 
				new File(Settings.getInstance().getBaseDir(), AUTO_CREATE_BLAST_XML_DIR));
		workDir.mkdirs();

		File query = createICTVQueryFile(workDir, ictvMasterSpeciesListFile);

		// Query from NCBI
		File fastaOut = new File(workDir, "fasta-out");
		File taxonomyOut = new File(workDir, "taxonomy-out");

		queryFasta(query, workDir, fastaOut);
		querytaxonomyIds(query, workDir, taxonomyOut);

		// Add taxonomy id data		
		fillData(accessionNumMapICTV, taxonomyOut, true);

		// preprocess fasta: remove the description.
		//File fastaOutPreprocessed = preprocessFasta(workDir, fastaOut); removed because the format of the files from NCBI was changed.

		// make sure that taxonomy is ready
		if (TaxonomyModel.getTaxons().isEmpty())
			TaxonomyModel.read(RegaSystemFiles.taxonomyFile());
		
		// create blast.xml

		List<AbstractSequence> badSequences = new ArrayList<AbstractSequence>();
		AlignmentAnalyses alignmentAnalyses = createAlignmentAnalyses(workDir, fastaOut);
		for (AbstractSequence s: alignmentAnalyses.getAlignment().getSequences()) {
			String accessionNumber = findAccessionNumber(getAccessionNumber(s.getName()), accessionNumMapICTV);
			if (accessionNumber == null)
				continue;

			Data data = accessionNumMapICTV.get(accessionNumber);

			// sequence 
			s.setName(accessionNumber);

			// cluster
			Cluster cluster = alignmentAnalyses.findCluster(data.taxonomyId);
			if(cluster == null) {
				cluster = new Cluster();
				if (data.taxonomyId == null) {
					System.err.println("taxonomyId == null : " + accessionNumber);
					badSequences.add(s);
					continue; // should not get here
				}
				String mnemenic = TaxonomyModel.getMnemenic(data.taxonomyId);
				String id = data.taxonomyId;
				if (mnemenic != null && !mnemenic.isEmpty())
					id += "_" + mnemenic;
				cluster.setId(id);
				cluster.setName(data.organizedName);
				cluster.setTaxonomyId(data.taxonomyId);
				cluster.setDescription(TaxonomyModel.getHirarchy(data.taxonomyId));// TODO: this should come from ICTV?
				alignmentAnalyses.getAllClusters().add(cluster);
			}

			cluster.addTaxus(new Taxus(accessionNumber, Taxus.SOURCE_ICTV));	
		}

		for (AbstractSequence s:badSequences) {
			alignmentAnalyses.getAlignment().getSequences().remove(s);
		}
		
		// Add NCBI refseq

		File ncbiAccQuery = createNcbiAccQuery(workDir, ncbiVirusesDb);
		
		File ncbiTaxonomyIdsFile = new File(workDir, "ncbi-taxonomy.fasta");
		
		querytaxonomyIds(ncbiAccQuery, workDir, ncbiTaxonomyIdsFile);

		File fastaNcbiPreprocessed = new File(workDir, "ncbi-preprocessed.fasta");
		
		preprocessFasta(ncbiTaxonomyIdsFile, fastaNcbiPreprocessed);

		Map<String, Data> ncbiAccMap = new HashMap<String, PanViralToolGenerator.Data>();
		fillData(ncbiAccMap, fastaNcbiPreprocessed, false);

		AlignmentAnalyses ncbiAlignmentAnalyses = createAlignmentAnalyses(workDir, ncbiVirusesDb);
		for (AbstractSequence s: ncbiAlignmentAnalyses.getAlignment().getSequences()) {
			String accessionNumber = getAccessionNumber(s.getName());
			if (accessionNumber == null)
				continue;

			Data data = ncbiAccMap.get(accessionNumber);

			if (data == null) {
				System.err.println("data for " + accessionNumber + " not found.");// the acc num is not in taxonomy file.
				continue;
			}

			// sequence 
			s.setName(accessionNumber);

			// cluster
			Cluster cluster = alignmentAnalyses.findCluster(data.taxonomyId);
			if(cluster == null) {
				cluster = new Cluster();
				if (data.taxonomyId == null) {
					System.err.println("taxonomyId == null : " + accessionNumber);
					continue; // should not get here
				}
				String mnemenic = TaxonomyModel.getMnemenic(data.taxonomyId);
				String id = data.taxonomyId;
				if (mnemenic != null && !mnemenic.isEmpty())
					id += "_" + mnemenic;
				cluster.setId(id);
				cluster.setName(data.organizedName);
				cluster.setTaxonomyId(data.taxonomyId);
				cluster.setDescription(TaxonomyModel.getHirarchy(data.taxonomyId));
				alignmentAnalyses.getAllClusters().add(cluster);
			}

			cluster.addTaxus(new Taxus(accessionNumber, Taxus.SOURCE_NCBI));
			alignmentAnalyses.getAlignment().addSequence(s);
		}
		
		return alignmentAnalyses;
	}

	private AlignmentAnalyses createAlignmentAnalyses(File workDir, File fastaOutPreprocessed) throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException {
		final File jobDir = GenotypeLib.createJobDir(workDir + File.separator + "tmp");
		jobDir.mkdirs();

		AlignmentAnalyses alignmentAnalyses = new AlignmentAnalyses();
		SequenceAlignment sequenceAlignment = new SequenceAlignment(new FileInputStream(fastaOutPreprocessed),
				SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);
		alignmentAnalyses.setAlignment(sequenceAlignment);
		BlastAnalysis blastAnalysis = new BlastAnalysis(alignmentAnalyses,
				"", new ArrayList<AlignmentAnalyses.Cluster>(),
				50.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, "-q -1 -r 1", "", jobDir);
		alignmentAnalyses.putAnalysis("blast", blastAnalysis);

		return alignmentAnalyses;
	}
	
	private void preprocessFasta(File fastaOut, File fastaOutPreprocessed) throws IOException {
		BufferedReader fastaBr = new BufferedReader(new FileReader(fastaOut));
		PrintWriter fastaWriter = new PrintWriter(new BufferedWriter(
				new FileWriter(fastaOutPreprocessed.getAbsolutePath(), true)));
		String fastaLine = null;
		while ((fastaLine = fastaBr.readLine()) != null) {
			if (fastaLine.length() > 0 && fastaLine.charAt(0) == '>'){
				//fastaWriter.println(">" + fastaLine.split(" ")[0]);
				fastaWriter.println(fastaLine.split(" ")[0]);
			} else {
				fastaWriter.println(fastaLine);
			}
		}
		fastaBr.close();
		fastaWriter.close();
	}

	private File createICTVQueryFile(File workDir, File ictvMasterSpeciesListFile) throws IOException {
		StringBuilder accessionNumbers = new StringBuilder();

		// parse ICTV Master Species List

		FileInputStream file = new FileInputStream(ictvMasterSpeciesListFile);
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		XSSFSheet sheet = workbook.getSheetAt(2);

		Iterator<Row> rowIterator = sheet.iterator();
		while (rowIterator.hasNext()) {
			/* separate
			 * known excretions:
			 * 1. KJ437671
			 * 2. JN606091, JN606090
			 * 3. KC979054 - KC979059
			 * 4. RNA1: AB512282, RNA2: AB512283
			 * 5. L segment: (HM745930), M segment: (HM745931), S segment: (HM745932)
			 * 6. KF812525 (RNA1), KF812526 (RNA2)
			 * 7. RNA1 (JX304792) full, RNA2 (JQ670669) full
			 * 8. Pepper yellow leaf curl virus - [China:YN65- 1:2010]  NOT SUPPORTED
			 */

			Row row = rowIterator.next();

			String accessionNumberField = ExcelUtils.readCell(row.getCell(ExemplarAccessionNumberCol));
			if (accessionNumberField.isEmpty())
				continue;

			if (accessionNumberField.contains("["))
				continue; // 8. is NOT SUPPORTED

			String acPattern = "\\s?\\b[A-Z]{1,2}_?\\d+\\.?\\d?\\b\\s?";
			for (String s: accessionNumberField.split(",")){
				{	// 1. KJ437671
					String pattern = "\\A" + acPattern + "\\z";
					Pattern r = Pattern.compile(pattern);
					Matcher m = r.matcher(s);
					if (m.find()) {
						System.err.println("1." + s);
						constructQuery(s, accessionNumbers);
						addField(s, row);
						continue;
					}
				}
				{	// 3. KC979054 - KC979059
					if (s.contains("-") || s.contains(" to ")) {
						s.replace(" to ", "-");

						String groupedAcPattern = "\\s?\\b([A-Z]{1,2}_?)(\\d+\\.?\\d?)\\b\\s?";
						String pattern = groupedAcPattern + "\\s?-\\s?" + groupedAcPattern;
						Pattern r = Pattern.compile(pattern);
						Matcher m = r.matcher(s);
						if (m.find()) {
							System.err.println("3." + s + " prefix = " + m.group(1) + " from = " + m.group(2) + " prefix = " + m.group(3) + " to = " + m.group(4));
							assert(m.group(1).equals(m.group(3)));
							for (int i = Integer.parseInt(m.group(2)); i <= Integer.parseInt(m.group(4)); ++i) {
								String ac = m.group(1) + i;
								constructQuery(ac, accessionNumbers);
								addField(ac, row);
							}
							continue;
						} 
					} 
				}
				{	// 4. RNA1: AB512282 
					// 5. L segment: (HM745930)
					// 7. RNA1 (JX304792) full,
					String pattern = "\\A(.*):?\\s?\\(?(" + acPattern + ")\\)?\\s?(\\(partial\\))?(\\(full\\))?(full)?\\z";
					Pattern r = Pattern.compile(pattern);
					Matcher m = r.matcher(s);
					if (m.find()) {
						String area = m.group(1);
						String ac = m.group(2);
						constructQuery(ac, accessionNumbers);
						addField(ac, row);
						System.err.println("4|5|7." + s + " ac = " + ac + " area = " + area);
						continue;
					}
				}
				{	// 6.KF812525 (RNA1)
					String pattern = "\\A(" + acPattern + ")\\s?\\((.*)\\)\\s?(full)?\\z";
					Pattern r = Pattern.compile(pattern);
					Matcher m = r.matcher(s);
					if (m.find()) {
						String ac = m.group(1);
						String area = m.group(2);
						constructQuery(ac, accessionNumbers);
						addField(ac, row);
						System.err.println("6." + s + " ac = " + ac + " area = " + area);
						continue;
					}
				}
				System.err.println("not identified = " + s);
			}
		}


		file.close();
		workbook.close();

		// create query file

		File query = new File(workDir, "query");
		FileUtil.writeStringToFile(query, accessionNumbers.toString());

		return query;
	}

	private String getAccessionNumber(String fastaName) {
		String accessionNumber = null;
		if (fastaName.contains("|")){
			String[] split = fastaName.split("\\|");
			for(int i = 0; i < split.length - 1; ++i){
				String s = split[i];
				if (s.equals("gb") || s.equals("emb") || s.equals("dbj")
						|| s.equals("tpe") || s.equals("ref"))
					accessionNumber = split[i + 1];
			}
			if (accessionNumber == null) {
				System.err.println("bad accession numebr regex " + fastaName);
				return null;
			}
		} else 
			accessionNumber = fastaName;

		return accessionNumber;
	}
	
	private String findAccessionNumber(String accessionNumber, Map<String, Data> accessionNumMap) {
		if (accessionNumber == null)
			return null;

		Data data = accessionNumMap.get(accessionNumber);
		if (data == null) {
			if (accessionNumber.contains(".")) // the result for accession number x can be x.1
				accessionNumber = accessionNumber.split("\\.")[0];
			data = accessionNumMap.get(accessionNumber);
			if (data == null) {
				System.err.println("bad accession numebr " + accessionNumber);
				return null; // it is possible that the accession number in ictv master list contains error; 
			}
		}

		return accessionNumber;
	}

	private void constructQuery(String accessionNum, StringBuilder accessionNumbers) {
		accessionNum.replace(" ", "");
		accessionNumbers.append(accessionNum + "\n");
	}

	private void addField(String accessionNum, Row row){
		accessionNum.replace(" ", "");

		String ds = FASTA_DESCRIPTION_SEPARATOR;

		String description = accessionNum + ds
				+ ExcelUtils.readCell(row.getCell(OrderCol)) + ds 
				+ ExcelUtils.readCell(row.getCell(FamilyCol)) + ds 
				+ ExcelUtils.readCell(row.getCell(SubfamilyCol)) + ds 
				+ ExcelUtils.readCell(row.getCell(GenusCol)) + ds 
				+ ExcelUtils.readCell(row.getCell(SpeciesCol)) + ds;
		
		description.replace(" ", "_");

		accessionNumMapICTV.put(accessionNum, new Data(description, null, null));
	}

	private File createNcbiAccQuery(File workDir, File ncbiVirusesDb) throws IOException, ParameterProblemException, FileFormatException {
//		File preprocessNcbiDbFasta = new File(workDir, "ncbi-preprocesses-sequences");
//				preprocessFasta(ncbiVirusesDb);
		AlignmentAnalyses ncbiSequences = createAlignmentAnalyses(workDir, ncbiVirusesDb);

		StringBuilder accessionBuild = new StringBuilder();
		
		for (AbstractSequence s: ncbiSequences.getAlignment().getSequences()) {
			String accessionNumber = getAccessionNumber(s.getName());
			if (accessionNumber == null)
				continue;

			Data data = accessionNumMapICTV.get(accessionNumber);

			if (data != null)
				continue; // the accession number was added from ICTV is more accurate.

			accessionBuild.append(accessionNumber);
			accessionBuild.append(System.getProperty("line.separator"));
		}

		File ans = new File(workDir, "ncbi-query");
		FileUtil.writeStringToFile(ans, accessionBuild.toString());
		return ans;
	}

	private void fillData(Map<String, Data> accessionNumMap, File taxonomyOut, boolean ictvData) throws IOException {
		BufferedReader taxonomyBr = new BufferedReader(new FileReader(taxonomyOut));

		String l = null;
		while ((l = taxonomyBr.readLine()) != null) {
			String[] row = l.split("\t");

			String accessionNumber = ictvData ? 
					findAccessionNumber(getAccessionNumber(row[0]), accessionNumMap) : 
						getAccessionNumber(getAccessionNumber(row[0]));
			if (accessionNumber == null)
				continue;

			String taxonomyId = row[1];
			String organizedName = row[2];
			String description = ictvData ? accessionNumMap.get(accessionNumber).description : "TODO";
			accessionNumMap.put(accessionNumber, new Data(description, taxonomyId, organizedName));
		}
		taxonomyBr.close();
	}

	private void querytaxonomyIds(File accessionNumbersQuery, File workDir, File out) throws ApplicationException, IOException, InterruptedException {
		String edirectPath = Settings.getInstance().getConfig().getGeneralConfig().getEdirectPath();

		String epost = edirectPath + "epost";
		String efetch = edirectPath + "efetch";
		String xtract = edirectPath + "xtract";

		String cmd = 
				// query taxonomy ids from NCBI
				"cat " + accessionNumbersQuery.getAbsolutePath() + "|" 
				+ epost + " -db nuccore -format acc|"
				+ efetch + " -db nuccore -format docsum | " 
				+ xtract + " -pattern DocumentSummary -element Extra,TaxId,Organism > " + out.getAbsolutePath();
		execShellCmd(cmd);
	}

	private void queryFasta(File accessionNumbersQuery, File workDir, File out) throws ApplicationException, IOException, InterruptedException {
		String edirectPath = Settings.getInstance().getConfig().getGeneralConfig().getEdirectPath();

		String epost = edirectPath + "epost";
		String efetch = edirectPath + "efetch";

		String cmd = 
				// query fasta ids from NCBI
				" cat " + accessionNumbersQuery.getAbsolutePath() + "|" 
				+ epost + " -db nuccore -format acc|" 
				+ efetch + " -db nuccore -format fasta > " + out.getAbsolutePath();

		execShellCmd(cmd);
	}

	private void execShellCmd(String cmd) throws IOException, InterruptedException, ApplicationException {
		String[] shellCmd = {"/bin/sh", "-c", cmd};
		System.err.println(cmd);

		Process fetchFasta = null;
		fetchFasta = Runtime.getRuntime().exec(shellCmd);
		int exitResult = fetchFasta.waitFor();
		if (exitResult != 0){
			throw new ApplicationException("fetchFasta exited with error: " + exitResult);
		}
	}

	public Signal1<AlignmentAnalyses> finished() {
		return finished;
	}
}