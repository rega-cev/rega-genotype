package rega.genotype.ui.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class CsvDataTable implements DataTable {
	private char delimiterChar;
	private char quoteChar;
	private Writer writer;
	int column;

	class CsvCell implements DataTable.Cell {
		private String value;

		public CsvCell(String v) {
			this.setValue(v);
		}

		public CsvCell(double v) {
			this.setValue(String.valueOf(v));
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public CsvDataTable(OutputStream stream, char delimiterChar, char quoteChar) {
		this.writer = new OutputStreamWriter(stream);
		this.delimiterChar = delimiterChar;
		this.quoteChar = quoteChar;
		column = 0;
	}

	public void addCell(Cell cell) throws IOException {
		if (column != 0)
			writer.append(delimiterChar);

		CsvCell csvCell = (CsvCell) cell;

		writer.append(quoteChar).append(escapeQuoteChar(csvCell.getValue())).append(quoteChar);
		
		++column;
	}

	private CharSequence escapeQuoteChar(String value) {
		return value.replaceAll(String.valueOf(quoteChar), "\\" + quoteChar);
	}

	public Cell createLabelCell(String v) {
		return new CsvCell(v);
	}

	public Cell createNumberCell(double v) {
		return new CsvCell(v);
	}

	public void newRow() throws IOException {
		writer.write('\n');
		column = 0;
	}

	public void flush() throws IOException {
		writer.flush();
	}

	public void addLabel(String v) throws IOException {
		addCell(createLabelCell(v));
	}

	public void addNumber(double v) throws IOException {
		addCell(createNumberCell(v));
	}

}
