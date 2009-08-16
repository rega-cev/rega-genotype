/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

/**
 * Exception thrown by a genotyping analysis
 */
public class AnalysisException extends Exception {
    private static final long serialVersionUID = 6415192377329324105L;
	private String analysId;
	private Exception nestedException;
	private AbstractSequence sequence;

    public AnalysisException(String analysisId, AbstractSequence sequence,
                             Exception nestedException) {    	
        super("Problem during analysis \"" + analysisId
              + "\" for sequence \"" + (sequence == null ? "(no sequence)" : sequence.getName())
              + "\": " + nestedException.getMessage());

        this.analysId = analysisId;
    	this.sequence = sequence;
    	this.nestedException = nestedException;
    }

	public String getAnalysId() {
		return analysId;
	}

	public Exception getNestedException() {
		return nestedException;
	}

	public AbstractSequence getSequence() {
		return sequence;
	}
}
