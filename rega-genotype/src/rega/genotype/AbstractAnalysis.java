/*
 * Created on Feb 6, 2006
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package rega.genotype;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rega.genotype.AlignmentAnalyses.Cluster;

public abstract class AbstractAnalysis {

	interface Concludable {
        public void writeConclusion(ResultTracer tracer);
        public Cluster getConcludedCluster();
        public float getConcludedSupport();
	}

	interface Scannable {
        public List<String> scanLabels();
        public List<Double> scanValues();
        public List<String> scanDiscreteLabels();
        public List<String> scanDiscreteValues();
	}
	
    static public class ComposedConclusion implements Concludable {
    	Concludable major, minor;

		public ComposedConclusion(Concludable major, Concludable minor) {
			this.major = major;
			this.minor = minor;
		}

		public void writeXML(ResultTracer tracer) {
		}

		public void writeConclusion(ResultTracer tracer) {
            tracer.printlnOpen("<assigned>");
            Cluster majorC = major.getConcludedCluster();
            Cluster minorC = minor.getConcludedCluster();
            
            if (majorC != minorC) {
            	tracer.add("id", majorC.getId() + " (" + minorC.getId() + ")");
                
                StringBuffer majorName = new StringBuffer(majorC.getName());
                
                if (majorName.indexOf("(") != -1) {
                    majorName.delete(majorName.indexOf("(") - 1, majorName.indexOf(")") + 1);
                }

            	tracer.add("name", majorName.toString() + " (" + minorC.getId() + ")");
            	tracer.add("support", minor.getConcludedSupport());
            } else {
            	tracer.add("id", majorC.getId());
            	tracer.add("name", majorC.getName());
            	tracer.add("support",  major.getConcludedSupport());
            }
            
            tracer.printlnOpen("<major>");
            major.writeConclusion(tracer);
            tracer.printlnClose("</major>");

            tracer.printlnOpen("<minor>");
            minor.writeConclusion(tracer);
            tracer.printlnClose("</minor>");

            tracer.printlnClose("</assigned>");
		}

		public Cluster getConcludedCluster() {
			return null;
		}

		public float getConcludedSupport() {
			return 0;
		}
	}

	public abstract class Result {
        private AbstractSequence sequence;

        public Result(AbstractSequence sequence) {
            this.sequence = sequence;
        }

		public AbstractSequence getSequence() {
            return sequence;
        }
        
        public AbstractAnalysis getAnalysis() {
            return AbstractAnalysis.this;
        }

        abstract public void writeXML(ResultTracer tracer);

        protected void writeXMLEnd(ResultTracer tracer) {
            tracer.printlnClose("</result>");
        }

        protected void writeXMLBegin(ResultTracer tracer) {
            tracer.printlnOpen("<result id=" + tracer.quote(getId()) + ">");
        }
    };

    private AlignmentAnalyses owner;
    private String            id;
	private String            options;

    public AbstractAnalysis(AlignmentAnalyses owner, String id) {
        this.owner = owner;
        this.id = id;
        this.options = null;
    }

    /*
     * This is the internal call for running an an analysis for a
     * given alignment + sequence.
     * 
     * The sequence may be null, if supported by the analysis.
     * 
     * The sequence may have been added already to the alignment (as for example
     * done in a scan analysis).
     */
    abstract Result run(SequenceAlignment alignment, AbstractSequence sequence)
        throws AnalysisException;

    /*
     * Run an anlysis for a given sequence, and store the result.
     * 
     * The sequence may be null, if supported by the analysis.
     */
    public Result run(AbstractSequence sequence) throws AnalysisException {
        
        Result r = run(owner.getAlignment(), sequence);
        getTracer().addResult(r);
        return r;
    }

    /**
     * @return
     */
    private ResultTracer getTracer() {
        return owner.getGenotypeTool().getTracer();
    }
    
    String getId() { return id; }

    /**
     * @param id The id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param alignment
     * @param sequence
     * @return
     * @throws AlignmentException
     */
    protected SequenceAlignment profileAlign(SequenceAlignment alignment, AbstractSequence sequence) throws AlignmentException {
        SequenceAlignment aligned = alignment;
    
        if (sequence != null && (alignment.findSequence(sequence.getName()) == null))
            aligned = SequenceAlign.profileAlign(sequence, alignment, owner.isTrimAlignment());
        return aligned;
    }

    protected String makeResource(File file, String suffix) throws IOException {
        File resource = getTracer().getResourceFile(suffix);
        file.renameTo(resource);
        return resource.getName();
    }
    
    protected File getTempFile(String fileName) {
        return new File(getTracer().getOutputPath() + File.separator + fileName);
    }

	protected boolean haveOption(String option) {
	    return options != null && options.contains(option);
	}

	public void setOptions(String options) {
		this.options = options;
	}
    
    public static double trimDouble(double d) {
        return (Math.round(d * 1000.) / 1000.);
    }

    public static float trimFloat(float d) {
        return (float)(Math.round(d * 1000) / 1000.);
    }
}
