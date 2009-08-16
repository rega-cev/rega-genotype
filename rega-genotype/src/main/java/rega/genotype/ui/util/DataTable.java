package rega.genotype.ui.util;

import java.io.IOException;

public interface DataTable {
	interface Cell {
	};

	Cell createNumberCell(double v);
	Cell createLabelCell(String v);

	void newRow() throws IOException;
	void addCell(Cell cell) throws IOException;
	void addLabel(String v) throws IOException;
	void addNumber(double v) throws IOException;
	
	void flush() throws IOException;
}
