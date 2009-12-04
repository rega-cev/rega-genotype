/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A sliding window version of a wrapped analysis.
 * 
 * By means of the Scannable interface, this analysis may wrap any other analysis
 * that provides results that implement this interface.
 * 
 * @author koen
 */
public class ScanAnalysis extends AbstractAnalysis {
	private static class FragmentResult {
		public int start, end;
		public AbstractAnalysis.Result result;
	};
	
    public class Result extends AbstractAnalysis.Result implements Scannable {
        private List<Scannable> windowResults;
		private List<FragmentResult> recombinationResults;
		private Map<String, Integer> supportedTypes = null; 

        public Result(AbstractSequence sequence, List<Scannable> windowResults, List<FragmentResult> recombinationResults) {
            super(sequence);
            this.windowResults = windowResults;
            this.recombinationResults = recombinationResults;
        }

		public double getBootscanSupport() {
        	return getBootscanSupport(windowResults.get(0).scanDiscreteLabels().get(0));
        }
        
        public double getBootscanSupport(String id) {
            List<String> labels = windowResults.get(0).scanDiscreteLabels();

            if (labels.contains(id)) {
            	int labelI = labels.indexOf(id);

            	Map<String, Integer> histogram = new TreeMap<String, Integer>();
            	int total = 0;

            	for (int i = 0; i < windowResults.size(); ++i) {
                	List<String> values = windowResults.get(i).scanDiscreteValues();
                	
                	String value = values.get(labelI);
                	if (value == null) {
                		++total;
                		continue;
                	}

                	if (!histogram.containsKey(value)) {
                		histogram.put(value, 0);
                	}
                	
                	histogram.put(value, histogram.get(value) + 1);
                	++total;
                }
            	
            	String best = null;
            	int bestCount = 0;
            	for (Iterator<String> i = histogram.keySet().iterator(); i.hasNext();) {
            		String value = i.next();
            		
            		if ((best == null) || (histogram.get(value) > bestCount)) {
            			best = value;
            			bestCount = histogram.get(value);
            		}
            	}
            	
            	return (double)bestCount / total;
            }

            throw new RuntimeException("No such id for bootscansupport: \"" + id + "\"");
        }

        
		private double getBootscanNoSupport(String id) {
            List<String> labels = windowResults.get(0).scanDiscreteLabels();

            if (labels.contains(id)) {
            	int labelI = labels.indexOf(id);

            	int nosupport = 0;

            	for (int i = 0; i < windowResults.size(); ++i) {
                	List<String> values = windowResults.get(i).scanDiscreteValues();
                	
                	String value = values.get(labelI);
                	if (value == null)
                		++nosupport;
                }
            	
            	return (double)nosupport / windowResults.size();
            }

            throw new RuntimeException("No such id for bootscansupport: \"" + id + "\"");
		}

        public boolean haveSupport() {
            if (cutoff == null)
                return false;
            else
                return getBootscanSupport() > cutoff;
        }
        
        /**
         * @return Returns map of types with their window support rate, where the support rate > 0.1
         */
        public Map<String, Integer> getSupportedTypes(){
        	if(supportedTypes == null){
        		supportedTypes = new TreeMap<String, Integer>();
        	
		        Map<String, Integer> windowCount = new TreeMap<String, Integer>();
		        int j = windowResults.get(0).scanDiscreteLabels().indexOf("assigned");
		        
	        	for (int i = 0; i < windowResults.size(); ++i) {
	            	List<String> values = windowResults.get(i).scanDiscreteValues();
	            	
	            	String value = values.get(j);
	            	if (value != null){
		            	Integer count = supportedTypes.get(value);
		            	if(count == null)
		            		supportedTypes.put(value, 1);
		            	else
		            		supportedTypes.put(value, ++count);
	            	}
	            }
		        
		        for(Map.Entry<String, Integer> me : windowCount.entrySet()){
		        	float ratio = (float)me.getValue()/windowResults.size();
		        	if(ratio <= 0.1)
		        		supportedTypes.remove(me.getKey());
		        }
        	}
        	return supportedTypes;
        }
        
        public int getWindowCount(){
        	return windowResults.size();
        }

        public void writeXMLTable(ResultTracer tracer) {
            tracer.printlnOpen("<data>");
            List<String> labels = windowResults.get(0).scanLabels();
            if (haveOption("data-all"))
            	labels.addAll(windowResults.get(0).scanDiscreteLabels());
            
            tracer.printNoindent("window");
            for (int i = 0; i < labels.size(); ++i)
                tracer.printNoindent('\t' + labels.get(i));
            tracer.printNoindent("\n");
            
            for (int j = 0; j < windowResults.size(); ++j) {
                tracer.printNoindent(String.valueOf(step*j + (window/2)));

                List<Double> values = windowResults.get(j).scanValues();
                for (int i = 0; i < values.size(); ++i)
                    tracer.printNoindent('\t' + String.valueOf(values.get(i)));

                if (haveOption("data-all")) {
                    List<String> dValues = windowResults.get(j).scanDiscreteValues();
                    for (int i = 0; i < dValues.size(); ++i)
                    	if (dValues.get(i) != null)
                    		tracer.printNoindent('\t' + dValues.get(i));
                    	else
                    		tracer.printNoindent("\t-");
                }

                tracer.printNoindent("\n");
            }
            tracer.printlnClose("</data>");
            
            if (haveOption("recombination") && recombinationResults != null) {
            	tracer.printlnOpen("<recombination>");

            	for (FragmentResult r : recombinationResults) {
            		tracer.printlnOpen("<region>");
                    tracer.add("start", String.valueOf(r.start));
                    tracer.add("end", String.valueOf(r.end));
            		r.result.writeXML(tracer);
            		tracer.printlnClose("</region>");
            	}
            	
            	tracer.printlnClose("</recombination>");
            }
        }

		private void writeXMLSupports(ResultTracer tracer) {
			List<String> supportLabels = windowResults.get(0).scanDiscreteLabels();
            for (int i = 0; i < supportLabels.size(); ++i) {
            	tracer.printlnOpen("<support id=" + tracer.quote(supportLabels.get(i)) + ">");
            	tracer.println(String.valueOf(trimDouble(getBootscanSupport(supportLabels.get(i)))));
            	tracer.printlnClose("</support>");
            	tracer.printlnOpen("<nosupport id=" + tracer.quote(supportLabels.get(i)) + ">");
            	tracer.println(String.valueOf(trimDouble(getBootscanNoSupport(supportLabels.get(i)))));
            	tracer.printlnClose("</nosupport>");
            }
		}

		private void writeXMLProfiles(ResultTracer tracer) {
			List<String> supportLabels = windowResults.get(0).scanDiscreteLabels();
            for (int j = 0; j < supportLabels.size(); ++j) {
            	tracer.printlnOpen("<profile id=" + tracer.quote(supportLabels.get(j)) + ">");
            	for (int i = 0; i < windowResults.size(); ++i) {
                	List<String> values = windowResults.get(i).scanDiscreteValues();
                	
                	String value = values.get(j);
                	if (value == null)
                		value = "-";

               		tracer.printNoindent(value + " ");
                }

            	tracer.println("");
            	tracer.printlnClose("</profile>");
            }
		}

		public void writeXML(ResultTracer tracer) {
            writeXMLBegin(tracer);

            writeXMLInfo(tracer);
            writeXMLTable(tracer);            
            writeXMLSupports(tracer);
            writeXMLProfiles(tracer);

            writeXMLEnd(tracer);
        }

        private void writeXMLInfo(ResultTracer tracer) {
            tracer.add("window", window);
            tracer.add("step", step);
        }

        public List<String> scanLabels() {
            List<String> result = new ArrayList<String>();
            
            result.add("bootscan");
            return result;
        }

        public List<Double> scanValues() {
            List<Double> result = new ArrayList<Double>();
            
            result.add(getBootscanSupport());
            
            return result;
        }

		public List<String> scanDiscreteLabels() {
            return new ArrayList<String>();
		}

		public List<String> scanDiscreteValues() {
            return new ArrayList<String>();
		}
    }

    private AbstractAnalysis analysis;
    private int window;
    private int step;
    private Double cutoff;
	public ScanAnalysis(AlignmentAnalyses owner, String id, AbstractAnalysis analysis,
                        int window, int step, Double cutoff, File workingDir) {
        super(owner, id);
        this.analysis = analysis;
        this.window = window;
        this.step = step;
        this.cutoff = cutoff;
        this.workingDir = workingDir;
    }

    @Override
    public Result run(SequenceAlignment alignment, AbstractSequence sequence)
            throws AnalysisException {

        try {
            SequenceAlignment aligned = profileAlign(alignment, sequence, workingDir);

            List<SequenceAlignment> windows
                = SlidingGene.generateSlidingWindow(aligned, window, step);
            List<Scannable> windowResults
                = new ArrayList<Scannable>();

            for (int i = 0; i < windows.size(); ++i) {
                windowResults.add((Scannable) analysis.run(windows.get(i), sequence));
            }

        	List<FragmentResult> recombinationResults = null;
            if (haveOption("recombination")) {
				recombinationResults = doRecombinationAnalysis(getOption("recombination"), windowResults, aligned, sequence);
            }

            return new Result(sequence, windowResults, recombinationResults);
        } catch (AlignmentException e) {
            throw new AnalysisException(getId(), sequence, e);
        }
    }

    private List<FragmentResult> doRecombinationAnalysis(String label, List<Scannable> windowResults, SequenceAlignment aligned,
    		AbstractSequence sequence) throws AnalysisException {
    	int labelIdx = windowResults.get(0).scanDiscreteLabels().indexOf(label);

    	boolean haveRecombination = false;

    	String current = null;
    	for (Scannable result : windowResults) {
    		String thisWindow = result.scanDiscreteValues().get(labelIdx);
    		if (thisWindow != null) {
    			if (current != null && !thisWindow.equals(current)) {
    				haveRecombination = true;
    				break;
    			}
    			current = thisWindow;
    		}
    	}
    	
    	if (haveRecombination) {
			List<FragmentResult> recombinationResults = new ArrayList<FragmentResult>();

    		int firstIndex = 0;
    		int lastIndex = 0;
    		current = null;

    		// make sure tree is saved as well
    		String originalOptions = analysis.getOptions();
    		analysis.setOptions(originalOptions + ",tree");

    		try {
        		for (Scannable result : windowResults) {
            		String thisWindow = result.scanDiscreteValues().get(labelIdx);

            		boolean handleFragment = (thisWindow != null && current != null && !thisWindow.equals(current));
            		
            		if (windowResults.indexOf(result) == windowResults.size() - 1) {
       					lastIndex = aligned.getLength();
       					handleFragment = true;
            		}

            		if (handleFragment && lastIndex > firstIndex) {
        				FragmentResult r = new FragmentResult();
        				r.start = firstIndex;
        				r.end = lastIndex;

        				SequenceAlignment fragment = aligned.getSubSequence(r.start, r.end);
        
        				r.result = analysis.run(fragment, sequence);
        				recombinationResults.add(r);

        				firstIndex = windowResults.indexOf(result) * step + window/2;
        			}

            		if (thisWindow != null) {
            			current = thisWindow;
        				lastIndex = windowResults.indexOf(result) * step + window/2;
            		}
            	}
    		} finally {
    			analysis.setOptions(originalOptions);
    		}
    		
    		return recombinationResults;
    	} else
    		return null;
	}

	private String getOption(String option) {
    	String[] options = getOptions().split(",");

    	for (String o : options) {
    		String[] ov = o.split(":");
    		
    		if (ov[0].equals(option))
    			return ov[1];
    	}
    	
    	return null;
	}

	/**
     * @return Returns the analysis.
     */
    public AbstractAnalysis getAnalysis() {
        return analysis;
    }

	@Override
	public Result run(AbstractSequence sequence) throws AnalysisException {
		return (rega.genotype.ScanAnalysis.Result) super.run(sequence);
	}
}
