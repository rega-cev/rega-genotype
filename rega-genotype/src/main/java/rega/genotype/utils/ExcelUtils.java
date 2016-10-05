package rega.genotype.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;

import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.WAbstractItemModel;

public class ExcelUtils {
	// trim that also discards unicode whitespace
	public static final String unicodeTrim(String str) {
		int len = str.length();
		int st = 0;
		char[] val = str.toCharArray();

		while ((st < len) && (val[st] <= ' ' || Character.isSpaceChar(val[st]))) {
			st++;
		}
		while ((st < len) && (val[len - 1] <= ' ' || Character.isSpaceChar(val[len - 1]))) {
			len--;
		}
		return ((st > 0) || (len < val.length)) ? str.substring(st, len) : str;
	}

	public static final String readCell(Cell cell) {
		if(cell == null) {
			return "";
		} else {
			switch(cell.getCellType()) {
			case Cell.CELL_TYPE_STRING 	:
				// string
				return unicodeTrim(cell.getRichStringCellValue().getString());
			case Cell.CELL_TYPE_NUMERIC :
			{
				double r = cell.getNumericCellValue();
				int i = (int) r;
				if ((double)i == r) {
					return String.valueOf(i);
				} else {
					return String.valueOf(r);
				}
			}
			case Cell.CELL_TYPE_BLANK 	:
				// empty
				return "";
			case Cell.CELL_TYPE_BOOLEAN:
				boolean booleanCellValue = cell.getBooleanCellValue();
				return String.valueOf(booleanCellValue);
			case Cell.CELL_TYPE_FORMULA:
			{
				FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
				CellValue cellValue = evaluator.evaluate(cell);
				switch (cellValue.getCellType()) {
				case Cell.CELL_TYPE_BOOLEAN:
					return String.valueOf(cellValue.getBooleanValue());
				case Cell.CELL_TYPE_NUMERIC:
					double r = cellValue.getNumberValue();
					int i = (int) r;
					if ((double)i == r) {
						return String.valueOf(i);
					} else {
						return String.valueOf(r);
					}
				case Cell.CELL_TYPE_STRING:
					return unicodeTrim(cellValue.getStringValue());
				case Cell.CELL_TYPE_BLANK:
					return "";
				case Cell.CELL_TYPE_ERROR:
					break;
					// CELL_TYPE_FORMULA will never happen
				case Cell.CELL_TYPE_FORMULA: 
					break;
				}
			}
			}
			// error
			return null;
		}
	}

	public static File write(WAbstractItemModel model, File file) {	
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		HSSFWorkbook workbook = new HSSFWorkbook();
		HSSFSheet worksheet = workbook.createSheet("sheet1");

		//headers
		HSSFRow headerRow = worksheet.createRow(0);
		for(int column = 0; column < model.getColumnCount(); column++) {
			HSSFCell hssfcell = headerRow.createCell(column);
			Object data = model.getHeaderData(column);
			hssfcell.setCellValue(new HSSFRichTextString(data == null ? "" : data.toString()));
		}

		//data
		for(int row = 0; row < model.getRowCount(); row++) {
			HSSFRow hssfrow = worksheet.createRow(row + 1);
			for(int column = 0; column < model.getColumnCount(); column++) {
				HSSFCell hssfcell = hssfrow.createCell(column);
				Object data = model.getData(row, column, ItemDataRole.DisplayRole);
				String text = data == null ? "" : data.toString();
				hssfcell.setCellValue(new HSSFRichTextString(text));
			}
		}
		try {
			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return file;
	}
}
