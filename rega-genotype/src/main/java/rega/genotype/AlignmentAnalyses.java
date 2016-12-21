/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import rega.genotype.AlignmentAnalyses.Cluster.Source;
import rega.genotype.BlastAnalysis.ReferenceTaxus;
import rega.genotype.BlastAnalysis.Region;
import rega.genotype.ui.util.GenotypeLib;

/**
 * Class that represents information contained in a a single analysis (XML file).
 * 
 * A single analysis file describes analyses that may be done based on a single set of
 * reference sequences (usually an alignment).
 * 
 * It provides the following information:
 *  - the reference sequences, possibly as an alignment, with an indication of the
 *    sequence type (AA or DNA)
 *  - the clusters that are described for these reference sequences
 *  - the analyses that are described for these reference sequences
 *  
 * @author koen
 */
public class AlignmentAnalyses {    
    private List<Cluster>                 clusters;
    private Map<String, AbstractAnalysis> analyses;
    private Map<String, Cluster>          clusterMap;
    private SequenceAlignment             alignment;
    private boolean                       trimAlignment;
    private GenotypeTool                  genotypeTool;
	private Region                        region;

    /**
     * A taxus corresponds to a sequence in the alignment
     */
    public static class Taxus {
    	private String id;
    	
    	public Taxus(String id) {
    		this.id = id;
    	}

		public String getId() {
			return id;
		}
    }

    /**
     * A cluster groups a number of taxa.
     * 
     * Clusters can contain sub clusters. Together with the taxa defined directly for
     * the cluster, taxa in sub clusters define all taxa for a cluster.
     * 
     * A cluster may have an optional description, and a list of tags that may be
     * referenced from analyses.
     * 
     * The cluster id is used internally (referenced from analyses), while the description
     * is reported to the user.
     */
    public static class Cluster {
    	public enum Source {
    		ICTV, NCBI, Unkown, Admin;
    		public static Source fromString(String str) {
    			try {
    				return valueOf(str);
    			} catch (Exception e) {
    				return Unkown;
    			}
    		}
    	}
    	
        private String        id;
        private String        name;
        private String        description;
        private List<Taxus>   taxa;
        private List<Cluster> clusters;
        private Cluster       parent;
        private String        tags;
		private String        taxonomyId;
    	private Source        source;

        public Cluster(String id, String name, String description, String tags, String taxonomyId, Source src) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.tags = tags;
			this.taxonomyId = taxonomyId;
            this.taxa = new ArrayList<Taxus>();
            this.clusters = new ArrayList<Cluster>();
            this.parent = null;
            this.source = src;
        }

        public Cluster() {
            this.id = null;
            this.name = null;
            this.description = null;
            this.tags = null;
			this.taxonomyId = null;
            this.taxa = new ArrayList<Taxus>();
            this.clusters = new ArrayList<Cluster>();
            this.parent = null;
            this.source = null;
        }
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void addTaxus(String id) {
            taxa.add(new Taxus(id));
        }

        public void addTaxus(Taxus taxus) {
            taxa.add(taxus);
        }

        public boolean removeTaxus(String taxusName) {
        	for (Taxus t: taxa)
        		if(t.getId().equals(taxusName))
        			return taxa.remove(t);

        	return false;
        }

        public void addCluster(Cluster c) {
            clusters.add(c);
            c.setParent(this);
        }
        
        private void setParent(Cluster cluster) {
            parent = cluster;
        }

        /**
         * @return all taxa in this cluster, including taxa from sub clusters
         */
        public List<Taxus> getTaxa() {
            List<Taxus> result = new ArrayList<Taxus>(taxa);

            for (int i = 0; i < clusters.size(); ++i) {
                result.addAll(clusters.get(i).getTaxa());
            }
            
            return result;
        }

        /**
         * @return same as getTaxa(), but returns the ids rather than the taxa themselves
         */
        public List<String> getTaxaIds() {
            List<Taxus> taxa = getTaxa();
            List<String> result = new ArrayList<String>();

            for (Taxus t:taxa) {
                result.add(t.getId());
            }
            
            return result;
        }

        /**
         * @return direct sub clusters
         */
        public List<Cluster> getClusters() {
            return clusters;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
        	this.description = description;
        }

        public void setId(String id) {
        	this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getTags() {
        	return tags;
        }

        /**
         * @return the cluster depth: for a top level cluster it is 1, for its sub clusters,
         * this is 2, etc...
         */
        public int depth() {
            if (parent == null)
                return 1;
            else
                return 1 + parent.depth();
        }

        /**
         * Returns whether the cluster contains directly or indirectly a particular taxus.
         */
        public boolean containsTaxus(String taxusId) {
            for (Taxus t:taxa) {
            	if (t.getId().equals(taxusId))
            		return true;
            }
            
            for (int i = 0; i < clusters.size(); ++i) {
                if (clusters.get(i).containsTaxus(taxusId))
                    return true;
            }
            
            return false;
        }

        /**
         * Quick and dirty test to see if a particular tag is set for the cluster.
         */
		public boolean hasTag(String tag) {
			return tags != null && tags.contains(tag);
		}

		public Cluster getParent() {
			return parent;
		}

		public String getTaxonomyId() {
			return taxonomyId;
		}

		public void setTaxonomyId(String taxonomyId) {
			this.taxonomyId = taxonomyId;
		}

		public Source getSource() {
			return source;
		}

		public void setSource(Source source) {
			this.source = source;
		}
    }

    public AlignmentAnalyses() {
    	this.clusters = new ArrayList<Cluster>();
        this.analyses = new HashMap<String, AbstractAnalysis>();
        this.clusterMap = new HashMap<String, Cluster>();
    }

    public AlignmentAnalyses(File fileName, GenotypeTool tool, File workingDir)
            throws IOException, ParameterProblemException, FileFormatException {

    	this();
        this.genotypeTool = tool;

        retrieve(fileName, workingDir);
    }

    public AbstractAnalysis getAnalysis(String id) {
    	AbstractAnalysis result = analyses.get(id);
    	if (result == null)
    		throw new RuntimeException("Could not find analysis named \"" + id + "\"");
        return result;
    }

    public void putAnalysis(String id, AbstractAnalysis analysis) {
    	analyses.put(id, analysis);
    }

    public boolean haveAnalysis(String id) {
    	return analyses.containsKey(id);
    }
    
    public Collection<AbstractAnalysis> analyses() {
    	return analyses.values();
    }

    @SuppressWarnings("unchecked")
	private void retrieve(File fileName, File workingDir)
            throws IOException, ParameterProblemException, FileFormatException {

        SAXBuilder builder = new SAXBuilder();
        try {
            Document document = builder.build(fileName);
            Element root = document.getRootElement();

            Element alignmentE = root.getChild("alignment");
            String alignmentFile = alignmentE.getAttributeValue("file");

            int sequenceType = SequenceAlignment.SEQUENCE_ANY;
            
            String data = alignmentE.getAttributeValue("data");
            if (data != null) {
            	if (data.equalsIgnoreCase("dna"))
            		sequenceType = SequenceAlignment.SEQUENCE_DNA;
            	else
            		sequenceType = SequenceAlignment.SEQUENCE_AA;
            }

            alignment
                = new SequenceAlignment
                    (new BufferedInputStream
                            (new FileInputStream(
                                    fileName.getParent()
                                    + File.separator + alignmentFile)),
                     SequenceAlignment.FILETYPE_FASTA,
                     sequenceType);
            
            String trimAlignment = alignmentE.getAttributeValue("trim");
            if (trimAlignment != null && trimAlignment.equals("true"))
                this.trimAlignment = true;
            else
                this.trimAlignment = false;

            Element clustersE = root.getChild("clusters");
            
            Element sequenceIdsE = clustersE.getChild("sequence-ids");
            if (sequenceIdsE != null) {
            	for (int i = 0; i < alignment.getSequences().size(); ++i) {
            		AbstractSequence seq = alignment.getSequences().get(i);
            		Cluster c = new Cluster(seq.getName(), seq.getName(), seq.getDescription(), null, null, null);
            		c.addTaxus(seq.getName()); //TODO SRC?
            		clusters.add(c);
            	}
            } else {
            	List clusterEs = clustersE.getChildren("cluster");
            
            	for (Iterator i = clusterEs.iterator(); i.hasNext();) {
            		clusters.add(readCluster((Element) i.next()));
            	}
            }

            List analysisEs = root.getChildren("analysis");            

            for (Iterator i = analysisEs.iterator(); i.hasNext();) {
                AbstractAnalysis n = readAnalysis((Element) i.next(), workingDir);
                analyses.put(n.getId(), n);
            }            
        } catch (JDOMException e) {
            e.printStackTrace();
        }        
    }

	private AbstractAnalysis readAnalysis(Element element, File workingDir) {
        String type = element.getAttributeValue("type");
        String id = element.getAttributeValue("id");
        
        if (type.equals("paup")) {
            return readPaupAnalysis(element, id, workingDir);
        } else if (type.equals("scan")) {
            return readScanAnalysis(element, id, workingDir);
        } else if (type.equals("blast")) {
            return readBlastAnalaysis(element, id, workingDir);
        } else {
            throw new RuntimeException("Unsupported analysis type: " + type);
        }
    }

	private Double elementDoubleValue(Element e) {
		if (e == null)
			return null;
		return e.getValue() == null ? null : Double.valueOf(e.getTextTrim());
	}

	private Boolean elementBooleanValue(Element e) {
		if (e == null)
			return null;
		return e.getValue() == null ? null : Boolean.valueOf(e.getTextTrim());
	}
	
    @SuppressWarnings("unchecked")
	private AbstractAnalysis readBlastAnalaysis(Element element, String id, File workingDir) {
        Element identifyE = element.getChild("identify");
        String clusters = identifyE.getTextTrim();
        List<Cluster> cs = parseCommaSeparatedClusterIds(clusters);

        Double absCutoff = elementDoubleValue(element.getChild("absolute-cutoff"));
        Double relativeCutoff = elementDoubleValue(element.getChild("relative-cutoff"));
        Double absMaxEValue = elementDoubleValue(element.getChild("absolute-max-e-value"));
        Double relativeMaxEValue = elementDoubleValue(element.getChild("relative-max-e-value"));
        Double absSimilarity = elementDoubleValue(element.getChild("absolute-similarity"));
        Double relativeSimilarity = elementDoubleValue(element.getChild("relative-similarity"));
        Boolean exactMatching = elementBooleanValue(element.getChild("exact-matching"));
        Boolean showMultiple = elementBooleanValue(element.getChild("show-multiple"));
        if (exactMatching == null)
        	exactMatching = false;
        if (showMultiple == null)
        	showMultiple = false;

        Element cutoffE = element.getChild("cutoff");
        if (cutoffE != null) { //  old cutoff format (support old blast.xml files)
        	Double cutoff = null;
        	boolean isrelativeCutoff = false;
        	Double maxPValue = null;
        	cutoff = Double.valueOf(cutoffE.getTextTrim());

        	String maxP = cutoffE.getAttributeValue("max-p-value");
        	if (maxP != null)
        		maxPValue = Double.valueOf(maxP);

        	String type = cutoffE.getAttributeValue("type");
        	if (type != null)
        		if (type.equals("relative"))
        			isrelativeCutoff = true;
        		else if (type.equals("absolute"))
        			; // default
        		else
        			throw new RuntimeException("BlastAnalysis: unsupported cutoff type " + type + ", expecting relative|absolute");
        	if (isrelativeCutoff){
        		relativeCutoff = cutoff;
        		relativeMaxEValue = maxPValue;
        	} else {
        		absCutoff = cutoff;
        		absMaxEValue = maxPValue;
        	}
        }

        String blastOptions = null;
        Element optionsE = element.getChild("options");
        if (optionsE != null)
            blastOptions = optionsE.getTextTrim();
        
        String detailsOptions = null;
        Element detailsE = element.getChild("details");
        if (detailsE != null)
            detailsOptions = detailsE.getTextTrim();

        BlastAnalysis analysis = new BlastAnalysis(this, id, cs,
        		absCutoff, absMaxEValue, absSimilarity, 
        		relativeCutoff, relativeMaxEValue, relativeSimilarity, exactMatching,
        		showMultiple, blastOptions, detailsOptions, workingDir);

        List regionsEs = element.getChildren("regions");
        
        int priority = 0;
        for (Iterator i = regionsEs.iterator(); i.hasNext();) {
        	Element regionsE = (Element) i.next();
        	ReferenceTaxus t = new ReferenceTaxus(regionsE.getAttributeValue("taxus"), priority++);

        	if (regionsE.getAttributeValue("report-as") != null) {
        		t.setReportAsOther(regionsE.getAttributeValue("report-as"),
        				Integer.valueOf(regionsE.getAttributeValue("report-as-offset")));
        	}
        	
            List regionEs = regionsE.getChildren("region");
            for (Iterator j = regionEs.iterator(); j.hasNext();) {
                Element regionE = (Element) j.next(); 
                String regionName = regionE.getAttributeValue("name");
                int begin = Integer.parseInt(regionE.getAttributeValue("begin"));
                int end = Integer.parseInt(regionE.getAttributeValue("end"));
               	t.addRegion(new BlastAnalysis.Region(regionName, begin, end));
            }
            
            analysis.addReferenceTaxus(t);
        }

        return analysis;
    }

    private AbstractAnalysis readScanAnalysis(Element element, String id, File workingDir) {
        Element windowE = element.getChild("window");
        int window = Integer.parseInt(windowE.getTextTrim());

        Element stepE = element.getChild("step");
        int step = Integer.parseInt(stepE.getTextTrim());

        Element cutoffE = element.getChild("cutoff");
        Double cutoff = null;
        if (cutoffE != null)
            cutoff = Double.valueOf(cutoffE.getTextTrim());

        Element analysisE = element.getChild("analysis");
        AbstractAnalysis analysis = null;
        if (analysisE.getAttribute("id") != null) {
            analysis = getAnalysis(analysisE.getAttributeValue("id"));
        } else {
            analysis = readAnalysis(analysisE, workingDir);
        }

        ScanAnalysis result = new ScanAnalysis(this, id, analysis, window, step, cutoff, workingDir);
        
        Element optionsE = element.getChild("options");
        if (optionsE != null)
            result.setOptions(optionsE.getTextTrim());
        
        return result;
    }

    private AbstractAnalysis readPaupAnalysis(Element element, String id, File workingDir) {
        Element identifyE = element.getChild("identify");
        String clusters = identifyE.getTextTrim();
        List<Cluster> cs = parseCommaSeparatedClusterIds(clusters);

        Element blockE = element.getChild("block");
        String block = blockE.getText();
        
        Element cutoffE = element.getChild("cutoff");
        Double cutoff = null;
        if (cutoffE != null)
            cutoff = Double.valueOf(cutoffE.getTextTrim());
        
        AbstractAnalysis analysis = new PhyloClusterAnalysis(this, id, cs, block, cutoff, workingDir);
        
        Element optionsE = element.getChild("options");
        if (optionsE != null)
            analysis.setOptions(optionsE.getTextTrim());
        
        return analysis;
    }

    private List<Cluster> parseCommaSeparatedClusterIds(String s) {
        List<Cluster> result = new ArrayList<Cluster>();
        
        if (s.equals("*")) {
        	result.addAll(this.clusters);
        } else if (!s.isEmpty()){
            String[] ids = s.split(",");
            for (int i = 0; i < ids.length; ++i) {
                String id = ids[i].trim();
                Cluster c = getCluster(id);
                if (c == null) {
                    throw new RuntimeException("Undefined cluster: " + id);
                }
                result.add(c);
            }
        }

        return result;
    }

     private Cluster getCluster(String id) {
        String name = id;
        String taxalist = null;
        if (id.contains("[")) {
            name = id.substring(0, id.indexOf('['));
            taxalist = id.substring(id.indexOf('[') + 1, id.length() - 1);
            
        }
        
        Cluster c = clusterMap.get(name);
        if (c != null && taxalist != null) {
            Cluster result = new Cluster(c.getId(), c.getName(), c.getDescription(), c.tags, c.getTaxonomyId(), c.getSource());
            
            List<Taxus> taxa = c.getTaxa();

            String[] ids = taxalist.split(";");
            try {
                for (int i = 0; i < ids.length; ++i) {
                    int idx = Integer.parseInt(ids[i]);
                    result.addTaxus(taxa.get(idx));
                }
                
                return result;
            } catch (NumberFormatException e) {
                return null;
            }
        } else
            return c;
    }

    @SuppressWarnings("unchecked")
	private Cluster readCluster(Element element) {
        String id = element.getAttributeValue("id");
        String name = element.getAttributeValue("name");
        String description = null;
        String tags = null;
        String taxonomyId = null;

        Element descriptionE = element.getChild("description");
        if (descriptionE != null)
            description = descriptionE.getText();

        Element tagsE = element.getChild("tags");
        if (tagsE != null)
            tags = tagsE.getText();
        
        Element taxonomyIdE = element.getChild("taxonomy-id");
        if (taxonomyIdE != null)
        	taxonomyId = taxonomyIdE.getText();

        Source source = Source.fromString(element.getAttributeValue("src"));

        Cluster result = new Cluster(id, name, description, tags, taxonomyId, source);
        clusterMap.put(id, result);
        
        List taxusEs = element.getChildren("taxus");        
        for (Iterator i = taxusEs.iterator(); i.hasNext();) {
            Element taxusE = (Element) i.next();
            Taxus taxus = new Taxus(taxusE.getAttributeValue("name"));
            result.addTaxus(taxus);
        }

        List clusterEs = element.getChildren("cluster");        
        for (Iterator i = clusterEs.iterator(); i.hasNext();) {
            result.addCluster(readCluster((Element) i.next()));
        }
        
        return result;
    }

    public List<Taxus> getAllTaxa() {
    	List<Taxus> taxa = new ArrayList<Taxus>();
    	for (Cluster cluster: getAllClusters()) 
			for (Taxus t: cluster.getTaxa()) 
				taxa.add(t);

    	return taxa;
    }
    
    public List<Cluster> getAllClusters() {
        return clusters;
    }

    public Cluster findCluster(String id){
    	for(Cluster c: clusters)
    		if(c.getId().equals(id))
    			return c;

    	return null;
    }

    public Cluster findClusterByTaxonomyId(String taxonomyId){
    	for(Cluster c: clusters)
    		if(c.getTaxonomyId().equals(taxonomyId))
    			return c;

    	return null;
    }

    public boolean removeCluster(Cluster c){
    	return clusters.remove(c);
    }

    public void addCluster(Cluster c) {
    	clusters.add(c);
    }

    /**
     * @return Returns the alignment.
     */
    public SequenceAlignment getAlignment() {
        return alignment;
    }

	public void setAlignment(SequenceAlignment alignment) {
		this.alignment = alignment;
	}

    /**
     * @return Returns the trimAlignment.
     */
    public boolean isTrimAlignment() {
        return trimAlignment;
    }

    /**
     * @return Returns the genotypeTool.
     */
    public GenotypeTool getGenotypeTool() {
        return genotypeTool;
    }

	public void setRegion(Region region) {
		this.region = region;
	}

	public Region getRegion() {
		return region;
	}

	public static File blastFile(File dir) {
		return new File(dir.getAbsolutePath(), "blast.xml");
	}

	public static AlignmentAnalyses readFastaToAlignmentAnalyses(File workDir, File fasta) throws ParameterProblemException, IOException, FileFormatException {
		final File jobDir = GenotypeLib.createJobDir(workDir + File.separator + "tmp");
		jobDir.mkdirs();

		AlignmentAnalyses alignmentAnalyses = new AlignmentAnalyses();
		SequenceAlignment sequenceAlignment = new SequenceAlignment(new FileInputStream(fasta),
				SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);
		alignmentAnalyses.setAlignment(sequenceAlignment);
		BlastAnalysis blastAnalysis = new BlastAnalysis(alignmentAnalyses,
				"", new ArrayList<AlignmentAnalyses.Cluster>(),
				50.0, 0.0, 0.0, 0.0, 0.0, 0.0, false, false, "-q -1 -r 1", "", jobDir);
		alignmentAnalyses.putAnalysis("blast", blastAnalysis);

		return alignmentAnalyses;
	}
}