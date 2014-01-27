/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.data;

import java.io.IOException;

import rega.genotype.data.GenotypeResultParser;
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

	//TODO no ui components in this class!
	private SequenceFilter filter;

	public AbstractDataTableGenerator(SequenceFilter filter, DataTable table) {
		super(-1);

		this.filter = filter;
		this.table = table;
	}

	@Override
	public void endSequence() {
		super.endSequence();

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

	protected void addPhyloResults(String analysisId, boolean useIdNotName, boolean innerOuterSupport) {
		addNamedValue("/genotype_result/sequence/result[@id='" + analysisId + "']/best/"
					+ (useIdNotName ? "id" : "name"), ValueFormat.Label);
		addNamedValue("/genotype_result/sequence/result[@id='" + analysisId + "']/best/support", ValueFormat.Number);
		if (innerOuterSupport) {
			addNamedValue("/genotype_result/sequence/result[@id='" + analysisId + "']/best/inner", ValueFormat.Number);
			addNamedValue("/genotype_result/sequence/result[@id='" + analysisId + "']/best/outer", ValueFormat.Number);
		}
	}

	protected void addPhyloScanResults(String analysisId) {
		addNamedValue("/genotype_result/sequence/result[@id='" + analysisId + "']/support[@id='best']", ValueFormat.Label);
		addNamedValue("/genotype_result/sequence/result[@id='" + analysisId + "']/support[@id='assigned']", ValueFormat.Label);
		addNamedValue("/genotype_result/sequence/result[@id='" + analysisId + "']/nosupport[@id='assigned']", ValueFormat.Label);
		addNamedValue("/genotype_result/sequence/result[@id='" + analysisId + "']/profile[@id='best']", ValueFormat.Label);
		addNamedValue("/genotype_result/sequence/result[@id='" + analysisId + "']/profile[@id='assigned']", ValueFormat.Label);
	}

	@Override
	public void endFile() {
		try {
			table.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
    public boolean skipSequence() {
    	return filter.excludeSequence(this);
    }
}
