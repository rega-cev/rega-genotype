/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.data;

import java.io.IOException;

import rega.genotype.ui.util.DataTable;

/**
 * An abstract class extending SaxParser, which provides utility functions to 
 * write a csv file.
 */
public abstract class AbstractDataTableGenerator extends GenotypeResultParser {
	public enum ValueFormat {
		Label,
		Number
	}
	
	private DataTable table;

	public AbstractDataTableGenerator(DataTable table) {
		this.table = table;
	}

	@Override
	public void endSequence() {
		try {
			table.newRow();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    protected void addValue(String value) {
    	try {
			table.addLabel(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    protected void addValue(double value) {
    	try {
			table.addNumber(value);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    protected void addNamedValue(String name, ValueFormat format) {
    	String value = getValue(name);
    	if (value == null)
    		value = "";

    	if (format == ValueFormat.Number)
    		try {
    			addValue(Double.valueOf(value));
    		} catch (NumberFormatException e) {
    			addValue(value);
    		}
    	else
    		addValue(value);
    }

	protected void addPhyloResults(String analysisId, boolean useIdNotName) {
		addNamedValue("genotype_result.sequence.result['" + analysisId + "'].best."
					+ (useIdNotName ? "id" : "name"), ValueFormat.Label);
		addNamedValue("genotype_result.sequence.result['" + analysisId + "'].best.support", ValueFormat.Number);
		addNamedValue("genotype_result.sequence.result['" + analysisId + "'].best.inner", ValueFormat.Number);
		addNamedValue("genotype_result.sequence.result['" + analysisId + "'].best.outer", ValueFormat.Number);
	}

	protected void addPhyloScanResults(String analysisId) {
		addNamedValue("genotype_result.sequence.result['" + analysisId + "'].support['assigned']", ValueFormat.Label);
		addNamedValue("genotype_result.sequence.result['" + analysisId + "'].support['best']", ValueFormat.Label);
		addNamedValue("genotype_result.sequence.result['" + analysisId + "'].nosupport['best']", ValueFormat.Label);
		addNamedValue("genotype_result.sequence.result['" + analysisId + "'].profile['assigned']", ValueFormat.Label);
		addNamedValue("genotype_result.sequence.result['" + analysisId + "'].profile['best']", ValueFormat.Label);
	}

	@Override
	public void endFile() {
		try {
			table.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
