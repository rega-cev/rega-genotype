package rega.genotype.ui.util;

import java.io.IOException;
import java.io.OutputStream;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCell;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class XlsDataTable implements DataTable {
	private WritableSheet sheet;
	private int row, column;
	private WritableWorkbook workbook;

	interface XlsCell extends Cell {		
		public WritableCell getCell();
	};
	
	class XlsNumberCell implements XlsCell {
		Number cell;

		public XlsNumberCell(int row, int column, double value) {
			cell = new Number(column, row, value);
		}

		public WritableCell getCell() {
			return cell;
		}
	}

	class XlsLabelCell implements XlsCell {
		Label cell;

		public XlsLabelCell(int row, int column, String value) {
			cell = new Label(column, row, value);
		}

		public WritableCell getCell() {
			return cell;
		}
	}

	public XlsDataTable(OutputStream stream) throws IOException {
		workbook = Workbook.createWorkbook(stream);
		sheet = workbook.createSheet("Sheet 1", 0);
	}
	
	public void addCell(Cell cell) throws IOException {
		XlsCell xlsCell = (XlsCell) cell;
		
		try {
			sheet.addCell(xlsCell.getCell());
		} catch (RowsExceededException e) {
			throw new IOException(e);
		} catch (WriteException e) {
			throw new IOException(e);
		}
	}

	public Cell createLabelCell(String v) {
		return new XlsLabelCell(row, column++, v);
	}

	public Cell createNumberCell(double v) {
		return new XlsNumberCell(row, column++, v);
	}

	public void flush() throws IOException {
		workbook.write();
		workbook.close(); 
	}

	public void newRow() throws IOException {
		++row;
		column = 0;
	}

	public void addLabel(String v) throws IOException {
		addCell(createLabelCell(v));
	}

	public void addNumber(double v) throws IOException {
		addCell(createNumberCell(v));
	}

}
