package rega.genotype.scripts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rega.genotype.singletons.Settings;
import rega.genotype.taxonomy.TaxonomyModel;
import rega.genotype.utils.ExcelUtils;

/**
 * Test taxonomer results.
 * 
 *  For each SRR file (only mock viromes), put in a file the expected taxonomy IDs according to the manuscript (as specific as possible).
	Run a script that for each # reads assigned:

	 - TP (identified taxonomy ID is or is descendant of golden species ID)
	 - FP (identified taxonomy ID is not ancestor or descendant of any golden species ID)
	 - Unspecific (identified taxonomy ID is not species but is an ancestor of a golden species ID)

	Then for each TP and FP: keep track of the identified species.

	Report:
	 - TP: amount of correclty identified species + number of reads
	 - FP: amount of incorreclty identified species + number of reads
	 - Unspecific: number of reads
 * 
 * @author michael
 */
public class CompareResultsSheetToExpected {
	public static final int INPUT_FILE_COLUMN = 0;
	public static final int TP_COLUMN = 1;
	public static final int TP_TOTAL_COLUMN = 2;
	public static final int TP_READS_COLUMN = 3;
	public static final int FP_COLUMN = 4;
	public static final int FP_READS_COLUMN = 5;
	public static final int UNSPECIFIC_COLUMN = 6;
	public static final int UNSPECIFIC_READS_COLUMN = 7;
	public static final int TP_NAMES_COLUMN = 8;
	public static final int FP_NAMES_COLUMN = 9;
	public static final int UNSPECIFIC_NAMES_COLUMN = 10;

	public static void main(String args[]) {
		File resultsFile = new File(args[0]);
		File expectedFile = new File(args[1]);
		File configFile = new File(args[2]); // path to config.json (in base-work-dir)
		File out = new File(resultsFile.getParentFile(), "summary.xlsx");

		Settings.initSettings(new Settings(configFile));

		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet worksheet = workbook.createSheet("sheet");

		// header
		ExcelUtils.add(worksheet, 0, INPUT_FILE_COLUMN, "Input File");
		ExcelUtils.add(worksheet, 0, TP_COLUMN, "TP");
		ExcelUtils.add(worksheet, 0, TP_TOTAL_COLUMN, "TP Total");
		ExcelUtils.add(worksheet, 0, TP_READS_COLUMN, "TP reads count");
		ExcelUtils.add(worksheet, 0, FP_COLUMN, "FP");
		ExcelUtils.add(worksheet, 0, FP_READS_COLUMN, "FP reads count");
		ExcelUtils.add(worksheet, 0, UNSPECIFIC_COLUMN, "Unspecific");
		ExcelUtils.add(worksheet, 0, UNSPECIFIC_READS_COLUMN, "Unspecific reads count");
		ExcelUtils.add(worksheet, 0, TP_NAMES_COLUMN, "TP names");
		ExcelUtils.add(worksheet, 0, FP_NAMES_COLUMN, "FP names");
		ExcelUtils.add(worksheet, 0, UNSPECIFIC_NAMES_COLUMN, "Unspecific names");

		Map<String, List<ResultRowData>> resultsMap = createResultsMap(resultsFile, 
				ParseTaxonomerResults.TAXONOMY_ID_COLUMN,
				ParseTaxonomerResults.READ_COUNT_COLUMN);
		Map<String, List<String>> expectedMap = createExpectedResultsMap(expectedFile);

		int row = 1;
		for(Map.Entry<String, List<ResultRowData>> resultEntry: resultsMap.entrySet()) {
			String fileName = resultEntry.getKey();
			List<String> expected = expectedMap.get(fileName);
			Map<String, String> descendantOriginMap = createDescendantsList(expected);
			Set<String> expectedAncestors = createAncestorsList(expected);
			List<ResultRowData> resultTaxonomyIds = resultEntry.getValue();
			int fp = 0;
			int tp = 0;
			int tpTotal = 0;
			int unspecific = 0;
			int fpReads = 0;
			int tpReads = 0;
			int unspecificReads = 0;
			StringBuilder tpNames = new StringBuilder();
			StringBuilder fpNames = new StringBuilder();
			StringBuilder unspecificNames = new StringBuilder();

			Set<String> found = new HashSet<String>();
			for (ResultRowData resultData: resultTaxonomyIds) {
				ResultType resultType = checkResult(resultData.taxonomyId,
						descendantOriginMap.keySet(), expectedAncestors);
				String scientificName = TaxonomyModel.getInstance().getScientificName(resultData.taxonomyId);
				switch (resultType) {
				case FP:
					fp++;
					fpReads += resultData.readCount;
					fpNames.append(scientificName);
					fpNames.append(";");
					break;
				case TP:
					if (!found.contains(descendantOriginMap.get(resultData.taxonomyId))) {
						found.add(descendantOriginMap.get(resultData.taxonomyId));
						tp++;
					}
					tpTotal++;
					tpReads += resultData.readCount;
					tpNames.append(scientificName);
					tpNames.append(";");
					break;
				case Unspecific:
					unspecific++;
					unspecificReads += resultData.readCount;
					unspecificNames.append(scientificName);
					unspecificNames.append(";");
					break;
				}
			}

			HSSFRow hssfRow = ExcelUtils.add(worksheet, row, 0, fileName);
			ExcelUtils.add(hssfRow, TP_COLUMN, tp+"");
			ExcelUtils.add(hssfRow, TP_TOTAL_COLUMN, tpTotal+"");
			ExcelUtils.add(hssfRow, FP_COLUMN, fp+"");
			ExcelUtils.add(hssfRow, UNSPECIFIC_COLUMN, unspecific+"");
			ExcelUtils.add(hssfRow, TP_READS_COLUMN, tpReads+"");
			ExcelUtils.add(hssfRow, FP_READS_COLUMN, fpReads+"");
			ExcelUtils.add(hssfRow, UNSPECIFIC_READS_COLUMN, unspecificReads+"");
			ExcelUtils.add(hssfRow, TP_NAMES_COLUMN, tpNames.toString());
			ExcelUtils.add(hssfRow, FP_NAMES_COLUMN, fpNames.toString());
			ExcelUtils.add(hssfRow, UNSPECIFIC_NAMES_COLUMN, unspecificNames.toString());

			row++;
		}

		ExcelUtils.writeDocument(workbook, out);
		System.err.println("done: results were written to " + out);
	}

	private enum ResultType {TP, FP, Unspecific}
	private static ResultType checkResult(String resultTaxonomyId, 
			Collection<String> expectedList, Collection<String> expectedAncestors) {
		if (expectedList.contains(resultTaxonomyId))
			return ResultType.TP;
		else if (expectedAncestors.contains(resultTaxonomyId))
			return ResultType.Unspecific;
		else
			return ResultType.FP;
	}

	private static Set<String> createAncestorsList(List<String> taxonomyIds) {
		Set<String> ans = new HashSet<String>();
		for (String txid: taxonomyIds)
			ans.addAll(TaxonomyModel.getInstance().getHirarchyTaxonomyIds(txid));

		return ans;
	}

	private static Map<String, String> createDescendantsList(List<String> taxonomyIds) {
		Map<String, String> descendantOriginMap = new HashMap<String, String>(); 
		for (String txid: taxonomyIds){
			//ans.addAll(TaxonomyModel.getInstance().getDescendantsTaxonomyIds(txid));
			List<String> descendantsTaxonomyIds = TaxonomyModel.getInstance().getDescendantsTaxonomyIds(txid);
			for (String d: descendantsTaxonomyIds) 
				descendantOriginMap.put(d, txid);
		}
		return descendantOriginMap;
	}

	private static Map<String, List<ResultRowData>> createResultsMap(File resultsFile,
			int resultsFileTaxonomyIdColumn, int resultsFileReadCountColumn) {
		final Map<String, List<ResultRowData>> resultsMap = new HashMap<String, List<ResultRowData>>();

		XSSFWorkbook workbook;
		try {
			workbook = new XSSFWorkbook( new FileInputStream(resultsFile));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 

		XSSFSheet sheet = workbook.getSheetAt(0);

		int r = 0;
		Iterator<Row> rowIterator = sheet.iterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			if (r == 0) {
				r++;
				continue; // ignore header
			}
			Cell nameCell = row.getCell(0);
			if (nameCell == null)
				continue;
			List<ResultRowData> idsList = resultsMap.get(ExcelUtils.readCell(nameCell));
			if (idsList == null)
				idsList = new ArrayList<ResultRowData>();
			Cell taxonomyIdCell = row.getCell(resultsFileTaxonomyIdColumn);
			Cell readCountCell = row.getCell(resultsFileReadCountColumn);

			String taxonomyId = ExcelUtils.readCell(taxonomyIdCell);
			int readCount = Integer.parseInt(ExcelUtils.readCell(readCountCell));
			idsList.add(new ResultRowData(taxonomyId, readCount));

			resultsMap.put(ExcelUtils.readCell(nameCell), idsList);
			r++;
		}

		try {
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return resultsMap;
	}

	private static Map<String, List<String>> createExpectedResultsMap(File expectedFile) {
		final Map<String, List<String>> expectedResultsMap = new HashMap<String, List<String>>();

		XSSFWorkbook workbook;
		try {
			workbook = new XSSFWorkbook( new FileInputStream(expectedFile));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 

		XSSFSheet sheet = workbook.getSheetAt(0);

		Iterator<Row> rowIterator = sheet.iterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			ArrayList<String> expectedList = new ArrayList<String>();
			Iterator<Cell> cellIterator = row.cellIterator();
			String fileName = null;
			if (cellIterator.hasNext())
				fileName = ExcelUtils.readCell(cellIterator.next());

			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				String value = ExcelUtils.readCell(cell);
				expectedList.add(value);
			}
			if (expectedList.size() > 0)
				expectedResultsMap.put(fileName, expectedList);
		}

		try {
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return expectedResultsMap;
	}

	// classes

	private static class ResultRowData {
		public String taxonomyId;
		public int readCount;

		ResultRowData(String taxonomyId, int readCount){
			this.taxonomyId = taxonomyId;
			this.readCount = readCount;
		}
	}
}
