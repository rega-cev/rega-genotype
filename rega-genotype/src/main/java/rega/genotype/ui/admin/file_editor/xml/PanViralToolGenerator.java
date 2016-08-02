package rega.genotype.ui.admin.file_editor.xml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.StreamReaderRuntime;

/**
 * Auto generate blast.xml for pan-viral tool from "ICTV Master Species List"
 * (download from https://talk.ictvonline.org/files/master-species-lists/m/msl/5945)
 * and a fasta file that was created by downloading fasta sequences for all
 * accession numbers in the list.
 * 
 * Note you can use  http://www.ncbi.nlm.nih.gov/sites/batchentrez to create the fasta file.
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
		Data(String description, String taxonomyId){
			this.description = description;
			this.taxonomyId = taxonomyId;
		}
	}

	// <AccessionNumber, data (description)> 
	private Map<String, Data> accessionNumMap = new HashMap<String, Data>(); 

	public AlignmentAnalyses readICTVMasterSpeciesList(File ictvMasterSpeciesListFile) throws ApplicationException, IOException, InterruptedException, ParameterProblemException, FileFormatException {
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ";";

		StringBuilder accessionNumbers = new StringBuilder();

		// parse ICTV Master Species List

		try {
			br = new BufferedReader(new FileReader(ictvMasterSpeciesListFile));
			boolean isHeader = true;
			while ((line = br.readLine()) != null) {
				if (isHeader) {
					isHeader = false;
					continue;
				}

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

				String[] row = line.split(cvsSplitBy);

				String accessionNumberField = row[ExemplarAccessionNumberCol];
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
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// create query file

		String edirectPath = Settings.getInstance().getConfig().getGeneralConfig().getEdirectPath();
		File workDir = FileUtil.createTempDirectory("tool-dir", 
				new File(Settings.getInstance().getBaseDir(), AUTO_CREATE_BLAST_XML_DIR));
		workDir.mkdirs();

		File query = new File(workDir, "query");
		FileUtil.writeStringToFile(query, accessionNumbers.toString());

		// Query from NCBI

		String script = "/home/michael/install/edirect/tests/q5"; //TODO !!
		
		File fastaOut = new File(workDir, "fasta-out");
		File taxonomyOut = new File(workDir, "taxonomy-out");

		String cmd = script + " " + edirectPath + " " +  query.getAbsolutePath() + " " +  taxonomyOut.getAbsolutePath() + " " + fastaOut.getAbsolutePath();
		
		System.err.println(cmd);		

		Process fetchFasta = null;
		fetchFasta = StreamReaderRuntime.exec(cmd, null, workDir);
		int exitResult = fetchFasta.waitFor();
		if (exitResult != 0){
			throw new ApplicationException("fetchFasta exited with error: " + exitResult);
		}

		// Add taxonomy id data

		BufferedReader taxonomyBr = new BufferedReader(new FileReader(taxonomyOut));

		String l = null;
		while ((l = taxonomyBr.readLine()) != null) {
			String[] row = l.split("\t");
			String accessionNumber = row[0];
			String taxonomyId = row[1];

			Data data = accessionNumMap.get(accessionNumber);
			accessionNumMap.put(accessionNumber, new Data(data.description, taxonomyId));
		}
		taxonomyBr.close();

		// create blast.xml

		final File jobDir = GenotypeLib.createJobDir(workDir + File.separator + "tmp");
		jobDir.mkdirs();
		AlignmentAnalyses alignmentAnalyses = new AlignmentAnalyses();
		SequenceAlignment sequenceAlignment = new SequenceAlignment(new FileInputStream(fastaOut),
				SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);
		alignmentAnalyses.setAlignment(sequenceAlignment);
		BlastAnalysis blastAnalysis = new BlastAnalysis(alignmentAnalyses,
				"", new ArrayList<AlignmentAnalyses.Cluster>(),
				50.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, "-q -1 -r 1", "", jobDir);
		alignmentAnalyses.putAnalysis("blast", blastAnalysis);

		String ds = FASTA_DESCRIPTION_SEPARATOR;

		for (AbstractSequence s: alignmentAnalyses.getAlignment().getSequences()) {
			String[] row = s.getName().split("\t");
			String accessionNumber = row[1];
			Data data = accessionNumMap.get(accessionNumber);

			// sequence 
			s.setName(accessionNumber + ds + data.taxonomyId + ds + data.description);

			// cluster
			Cluster cluster = alignmentAnalyses.findCluster(data.taxonomyId);
			if(cluster == null) {
				cluster = new Cluster();
				cluster.setId(data.taxonomyId);
				cluster.setName("TODO"); //TODO
				alignmentAnalyses.getAllClusters().add(cluster);
			}

			cluster.addTaxus(new Taxus(data.taxonomyId));	
		}

		return alignmentAnalyses;
	}
	
	private void constructQuery(String accessionNum, StringBuilder accessionNumbers) {
		accessionNumbers.append(accessionNum + "\n");
	}

	private void addField(String accessionNum, String[] row){
		String ds = FASTA_DESCRIPTION_SEPARATOR;

		String description = accessionNum + ds
				+ row[OrderCol] + ds 
				+ row[FamilyCol] + ds 
				+ row[SubfamilyCol] + ds 
				+ row[GenusCol] + ds 
				+ row[SpeciesCol] + ds; 

		accessionNumMap.put(accessionNum, new Data(description, null));
	}
}