/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

/**
 * Implements the abstract sequence interface based on a subsequence of another
 * AbstractSequence.
 * 
 * @author koen
 */
public class SubSequence extends AbstractSequence {
    private String name;
    private String description;
    private AbstractSequence sequence;
    private int beginIndex;
    private int endIndex;

    public SubSequence(String name, String description,
                       AbstractSequence sequence, int beginIndex,
                       int endIndex)
    {
        this.name = name;
        this.description = description;
        this.sequence = sequence;
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSequence() {
        return sequence.getSequence().substring(beginIndex, endIndex);
    }

    public int getLength() {
        return (endIndex - beginIndex);
    }

	public void removeChar(int i) {
		sequence.removeChar(beginIndex + i);
	}

	@Override
	public AbstractSequence sourceSequence() {
		return sequence.sourceSequence();
	}
}
