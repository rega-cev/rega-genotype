package rega.genotype.scripts;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import rega.genotype.singletons.Settings;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.utils.ExcelUtils;
import rega.genotype.utils.FileUtil;

/**
 * Convert taxonomer csv result to excel file that can be compared to our results.
 * 
 * @author michael
 */
public class ParseTaxonomerResults {
	public static final int INPUT_FILE_COLUMN = 0;
	public static final int TAXONOMY_ID_COLUMN = 1;
	public static final int SCIENTIFIC_NAME_COLUMN = 2;
	public static final int READ_COUNT_COLUMN = 3;

	/*  Taxonomer result line documentation from:
	 *  http://taxonomer.iobio.io/faq.html
	 *  
	    Taxonomer results can be downloaded in hierarchical JSON format or as tab-deliminted text file with read-level results that has the following columns: 
		1) Bin category 
		2) Read classified (C) or unclassified (U) following bin assigment 
		3) Read name 
		4) Taxid of read assignment. For virus this corresponds to NCBI taxid. For bacteria, this corresponds to an artificial ID to the greengenes database, likewise for fungi except to the UNITE database. For Human this is an artificial ID to a gene. 
		5) Internal sequence identifier of sequence that contributed either completely or equally (in case of a tie) to the read’s assignment. 
		6) Number of taxonomic levels at which the read assignment was made (more levels is a deeper assignment). Note this value cannot be compared between bin categories because difference taxonomies are used — for example, one cannot use this to compare the depth of a bacterial assignment to that of a viral assignment. 
		7) Number of sequences tied for the assignment 
		8) Max sum of k-mer weights for read assignment 
		9) Standard deviation of sum of k-mer weights for read assignment 
		10) Read length 
		11) Number of k-mers used to make the read assignment 
		12) Bin placeholder (no external significance) 
	 */

	public static void main(String args[]) {
		File taxonomerResultsDir = new File(args[0]);
		File configFile = new File(args[1]); // path to config.json (in base-work-dir)
		File out = new File(taxonomerResultsDir, "taxonomer-results.xlsx");

		Settings.initSettings(new Settings(configFile));
		//Settings.initSettings(new Settings(new File("/home/michael/projects/rega-genotype/rega-genotype/base-work-dir/config.json")));

		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet worksheet = workbook.createSheet("sheet");

		// header
		ExcelUtils.add(worksheet, 0, INPUT_FILE_COLUMN, "Input File");
		ExcelUtils.add(worksheet, 0, TAXONOMY_ID_COLUMN, "Taxonomy Id");
		ExcelUtils.add(worksheet, 0, SCIENTIFIC_NAME_COLUMN, "Scientific Name");
		ExcelUtils.add(worksheet, 0, READ_COUNT_COLUMN, "Read Count");

		int row = 1;

		for (File result: taxonomerResultsDir.listFiles()) {
			if (result.getName().startsWith("ERR")
					|| result.getName().startsWith("SRR")){
				// sort
				// <taxonomyId, counter>
				Map<String, Integer> taxonomyCounters = new HashMap<String, Integer>(); 
				List<String[]> readCSV = FileUtil.readCSV(result, "\t");
				for (String[] line: readCSV){
					String binCategory = line[0];
					String ReadClassified = line[1];
					if (binCategory.equals("viral") && ReadClassified.equals("C")) {
						String taxonomyId = line[3];
						Integer counter = taxonomyCounters.get(taxonomyId);
						if (counter == null)
							counter = 0;
						counter++;
						taxonomyCounters.put(taxonomyId, counter);
					}
				}

				// add results to sheet.
				for (Map.Entry<String, Integer> e: taxonomyCounters.entrySet()) {
					String taxonomyId = e.getKey();
					String scientificName = TaxonomyModel.getInstance().getScientificName(taxonomyId);
					HSSFRow hssfRow = ExcelUtils.add(worksheet, row, INPUT_FILE_COLUMN, result.getName());
					ExcelUtils.add(hssfRow, TAXONOMY_ID_COLUMN, taxonomyId);
					ExcelUtils.add(hssfRow, SCIENTIFIC_NAME_COLUMN, scientificName);
					ExcelUtils.add(hssfRow, READ_COUNT_COLUMN, e.getValue() + "");
					row++;
				}
			}
		}

		ExcelUtils.writeDocument(workbook, out);
		System.err.println("done: results were written to " + out);
	}
}
