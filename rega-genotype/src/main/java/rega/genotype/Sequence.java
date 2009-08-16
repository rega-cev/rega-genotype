package rega.genotype;
/*
 * Created on Apr 7, 2003
 */

/**
 * Represents a single sequence
 */
public class Sequence extends AbstractSequence
{
    private String name;
    private String description;
    private String sequence;
	private AbstractSequence sourceSequence = this;

    public Sequence(String name, String description, String sequence) {
        this.name = name;
        this.description = description;
        this.sequence = sequence;
    }

    public String getName() {
        return name;
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
	
	int firstNonGapPosition() {
		for (int i = 0; i < sequence.length(); ++i) {
			if (sequence.charAt(i) != '-')
				return i;
		}
		return sequence.length();
	}

	int lastNonGapPosition() {
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
}
