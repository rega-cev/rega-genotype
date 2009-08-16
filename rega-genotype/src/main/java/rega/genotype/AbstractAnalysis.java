/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rega.genotype.AlignmentAnalyses.Cluster;

/**
 * An abstract base class for genotyping analyses.
 * 
 * @author koen
 */
public abstract class AbstractAnalysis {

	/**
	 * An interface for a conclusion.
	 * 
	 * A conclusion can be serialized as a <conclusion> in the results file.
	 */
	interface Concludable {
        public void writeConclusion(ResultTracer tracer);
        public Cluster getConcludedCluster();
        public float getConcludedSupport();
	}

	/**
	 * An interface for a result that may be plotted by a scan analysis.
	 * 
	 * A scan analysis implements a sliding window analysis, and at each time point
	 * results are retrieved from a Scannable result. The result may define a number
	 * of numerical and discrete variables.
	 */
	interface Scannable {
		/**
		 * @return the labels of numerical variables for the scan plot
		 */
        public List<String> scanLabels();

        /**
         * scanValues().size() == scanLabels.size()
         * 
		 * @return the numerical values at a single window point for a scan plot
		 */
        public List<Double> scanValues();

		/**
		 * @return the labels of discrete variables for the scan plot
		 */
        public List<String> scanDiscreteLabels();

        /**
         * scanDiscreteValues().size() == scanDiscreteLabels.size()
         * 
		 * @return the discrete values at a single window point for a scan plot
		 */
        public List<String> scanDiscreteValues();
	}

	/**
	 * A conclusion that combines a major and minor assignment conclusion for the sequence.
	 */
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

    /**
     * Abstract base class for results of an analysis.
     * 
     * Each analysis will typically extend from this result to provide information specific
     * to that analysis.
     */
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

    protected AlignmentAnalyses owner;
    private String            id;
	private String            options;

    public AbstractAnalysis(AlignmentAnalyses owner, String id) {
        this.owner = owner;
        this.id = id;
        this.options = null;
    }

    /**
     * This is the internal call for running an an analysis for a
     * given alignment + sequence.
     * 
     * The sequence may be null, if supported by the analysis (e.g. then it does an
     * internal quality control of the alignment).
     * 
     * The sequence may have been added already to the alignment (as for example
     * done in a scan analysis).
     */
    abstract Result run(SequenceAlignment alignment, AbstractSequence sequence)
        throws AnalysisException;

    /**
     * Run an analysis for a given sequence, and store the result.
     * 
     * The sequence may be null, if supported by the analysis.
     */
    public Result run(AbstractSequence sequence) throws AnalysisException {
        
        Result r = run(owner.getAlignment(), sequence);
        getTracer().addResult(r);
        return r;
    }

    private ResultTracer getTracer() {
        return owner.getGenotypeTool().getTracer();
    }
    
    String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    protected SequenceAlignment profileAlign(SequenceAlignment alignment, AbstractSequence sequence, File workingDir) throws AlignmentException {
        SequenceAlignment aligned = alignment;
    
        if (sequence != null && (alignment.findSequence(sequence.getName()) == null))
            aligned = SequenceAlign.profileAlign(sequence, alignment, owner.isTrimAlignment(), workingDir);
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

    /**
     * Quick and dirty way of obtaining a flag from the <options> block in the analyses.
     */
	protected boolean haveOption(String option) {
	    return options != null && options.contains(option);
	}

	/**
	 * Information from the <options> block configured for this analysis.
	 */
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
