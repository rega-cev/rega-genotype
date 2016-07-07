/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

/**
 * Class that directly implements the AbstractSequence information given
 * sequence information (e.g. from a FASTA file).
 * 
 * @author koen
 */
public class Sequence extends AbstractSequence
{
    private String name;
    private boolean nameCapped;
	private String description;
    private String sequence;
    private String quality;
	private AbstractSequence sourceSequence = this;

    public Sequence(String name, boolean nameCapped, String description, String sequence, String quality) {
        this.name = name;
        this.nameCapped = nameCapped;
        this.description = description;
        this.sequence = sequence;
        this.quality = quality;
    }

    public String getName() {
        return name;
    }
    
    public boolean isNameCapped() {
		return nameCapped;
	}

    public String getDescription() {
        return description;
    }

    public String getSequence() {
        return sequence;
    }

    public int getLength() {
        return sequence.length();
    }

	public void removeChar(int i) {
		sequence = new StringBuffer(sequence).deleteCharAt(i).toString();
	}
	
	public int firstNonGapPosition() {
		for (int i = 0; i < sequence.length(); ++i) {
			if (sequence.charAt(i) != '-')
				return i;
		}
		return sequence.length();
	}

	public int lastNonGapPosition() {
		for (int i = sequence.length() - 1; i >= 0; --i) {
			if (sequence.charAt(i) != '-')
				return i;
		}

		return -1;
	}
    
    public void removeGaps() {
        StringBuffer sb = new StringBuffer(sequence);
        for (int i = 0; i < sb.length(); ++i) {
            if (sb.charAt(i) == '-') {
                sb.deleteCharAt(i);
                --i;
            }
        }

        sequence = sb.toString();
     }

	public int getStart() {
		return 0;
	}

	@Override
	public AbstractSequence sourceSequence() {
		return sourceSequence;
	}

	public void setSourceSequence(AbstractSequence sourceSequence) {
		this.sourceSequence = sourceSequence;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public void setQuality(String quality) {
		this.quality = quality;
	}

	public String getQuality() {
		return quality;
	}
}
