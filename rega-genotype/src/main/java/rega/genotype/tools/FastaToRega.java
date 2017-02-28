package rega.genotype.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import rega.genotype.AbstractSequence;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.SequenceAlignment;

/**
 * Auto create phylo- and blast xmls from fasta alignment file.
 * Syntax: {genotype}{sub-type}_{sequence name}
 * example:1a_AF123456
 * genotype = 1
 * sub-type = a
 * 
 * phylo-major: identify genotype -> 1 cluster per genotype
 * phylo-minor: identify sub-type -> 1 cluster per sub-type
 * example: fasta names
 * >1a_AF111111
 * >1a_AF123456
 * >1b_
 * >2a_
 * 
 * That will auto generate:
 * 2 clusters for phylo major: 1,2
 * 3 clusters for phylo minor: 1a,1b,2a
 */
public class FastaToRega {
	private static final String OUT_GROUP_NAME = "X";

	public enum AnalysisType {
		Major, Minor, MajorSelfScan, Empty;

		private String analysisNumber = "";

		public String analysisName() {
			switch (this) {
			case Major: return "phylo-major";
			case Minor: return "phylo-minor-" + analysisNumber;
			case MajorSelfScan: return "phylo-major-" + "self-scan";
			case Empty: return "Error= this analysis should not have an id";
			}
			return "";
		}

		public String getAnalysisNumber() {
			return analysisNumber;
		}

		public void setAnalysisNumber(String analysisNumber) {
			this.analysisNumber = analysisNumber;
		}

	}

	private static class Sequence {
		String genotype = null;
		String subtype = null;
		String id;
		String data;
		public String toString() { return genotype+"-"+subtype+":"+id; }
	}

	/**
	 * It is possible to run this as a java application.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args.length < 2) {
			System.err.println("Usage: fastaToRega input.fasta output-dir");
			return;
		}

		String fastaAlingmentFile = args[0];
		String toolDir = args[1];
		String taxonomyId = args[2];

		createTool(taxonomyId, fastaAlingmentFile, toolDir);
	}

	/**
	 * Create phylo-{taxonomyId}.xml xml File
	 * @param taxonomyId - will be used in the name of the file and also in the cluster name of blast.xml
	 * @param fastaAlingmentFile - all the fasta sequences for the analysis (must be formated as documeted on top)
	 * @param toolDir - xml dir of the tool the new file will be stored there (same as in createTool)
	 */
	public static void addPhyloFile(String taxonomyId, String fastaAlingmentFile, String toolDir) {
		// TODO: Hey Yum, this function will be very similar to createTool.
		// There is 2 cases
		// 1. blast.xml does not exist -> exactly the same as createTool
		// 2. blast.xml exist -> you do not need to create it but only to add the correct clusters for this phylo analysis. 
		//    use: addClustersToBlastDoc
	}

	/**
	 * Add the clusters of a phylo- analysis to blast.xml
	 * @param alingmentFile
	 * @param taxonomyId - used for the cluster name.
	 * @param genotypeToSubtypeToSequences - Map<genotype, Map<subType, all the sequences that belong to the subType> >
	 * @return the new blast.xml Document
	 */
	private static void addClustersToBlastDoc(
			String alingmentFile, String taxonomyId, 
			Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences) {
		// TODO: Hey Yum, this function will be very similar to blastDocument.
		// you need to read blast Document, add the clusters and write it back to the file.
	}

	/**
	 * Create blast.xml xml File
	 * @param taxonomyId - will be used in the name of the file and also in the cluster name of blast.xml
	 * @param fastaAlingmentFile - all the fasta sequences for the analysis (must be formated as documeted on top)
	 * @param toolDir - xml dir of the tool the new file will be stored there (same as in createTool)
	 */
	public static void addBlastFile(String taxonomyId, String fastaAlingmentFile, String toolDir) {
		// TODO: Hey Yum, this function will be very similar to createTool but does not need to create the phylo.xml file
	}

	/**
	 * Main function: Create blast.xml and phylo xml files 
	 */
	public static void createTool(String taxonomyId, String fastaAlingmentFile, String toolDir) throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException, ParserConfigurationException, TransformerException {
		// may need to update pattern to extract genotype string from FASTA seq id
		//Pattern pattern = Pattern.compile("(\\d[^_]*)\\??_.*");
		
		//Pattern pattern = Pattern.compile("(\\d++)([^_]++)_.*"); // Tulio
		Pattern pattern = Pattern.compile("([^\\.]++).([^_]++)_.*"); // Sam

		List<Sequence> sequences = new ArrayList<Sequence>();

		SequenceAlignment seqAlign = new SequenceAlignment(new FileInputStream(new File(fastaAlingmentFile)), 
				SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);

		for (AbstractSequence seq : seqAlign.getSequences()) {
			Matcher matcher = pattern.matcher(seq.getName());
			if(matcher.find()) {
				Sequence sequence = new Sequence();
				sequence.id = seq.getName();
				sequence.genotype = matcher.group(1);
				sequence.subtype = matcher.group(2);
				sequence.data = seq.getSequence();
				sequences.add(sequence);
			} else if (seq.getName().startsWith(OUT_GROUP_NAME)) {
				Sequence sequence = new Sequence();
				sequence.id = seq.getName();
				sequence.genotype = OUT_GROUP_NAME;
				sequence.subtype = "";
				sequence.data = seq.getSequence();
				sequences.add(sequence);
			}

		}

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new File(fastaAlingmentFile));
			for (Sequence seq : sequences) {
				writer.println(">"+seq.id);
				writer.println(seq.data);
				writer.flush();
			}
		} finally {
			if (writer != null)
				writer.close();
		}

		// Map<genotype, all the sequences that belong to the genotype>
		Map<String, List<Sequence>> genotypeToSequences = new HashMap<String, List<Sequence>>();		
		for (Sequence seq : sequences) {
			if (genotypeToSequences.get(seq.genotype) == null)
				genotypeToSequences.put(seq.genotype, new ArrayList<Sequence>());
			genotypeToSequences.get(seq.genotype).add(seq);
		}

		// Map<genotype, Map<subType, all the sequences that belong to the subType> >
		Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences = new LinkedHashMap<String, Map<String, List<Sequence>>>();
		for (Entry<String, List<Sequence>> a : genotypeToSequences.entrySet()) {
			Map<String, List<Sequence>> subtypeMap = new HashMap<String, List<Sequence>>();

			for (Sequence seq : a.getValue()) {
				if (subtypeMap.get(seq.subtype) == null)
					subtypeMap.put(seq.subtype, new ArrayList<Sequence>());
				subtypeMap.get(seq.subtype).add(seq);
			}
			genotypeToSubtypeToSequences.put(a.getKey(), subtypeMap);
		}

		// create phylo-taxonomyId.xml file.
		Document clustalDoc = clustersDoc(fastaAlingmentFile, genotypeToSubtypeToSequences);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(toolDir, "phylo-" + taxonomyId + ".xml"));
			prettyPrint(clustalDoc, fos, 4);
		} finally {
			if (fos != null)
				fos.close();
		}

		// create blast.xml
		Document blastDoc = blastDocument(
				fastaAlingmentFile, taxonomyId, genotypeToSubtypeToSequences);
		fos = null;
		try {
			fos = new FileOutputStream(new File(toolDir, "blast.xml"));
			prettyPrint(blastDoc, fos, 4);
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	/**
	 * Create blast.xml file
	 * @param alingmentFile
	 * @param taxonomyId - used for the cluster name.
	 * @param genotypeToSubtypeToSequences - Map<genotype, Map<subType, all the sequences that belong to the subType> >
	 * @return the new blast.xml Document
	 * @throws ParserConfigurationException
	 */
	private static Document blastDocument(
			String alingmentFile, String taxonomyId, 
			Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences)
					throws ParserConfigurationException {
		Document doc = newDocument();
		Element genotypeAnalysesElem = regaGenotypeAnalysesDoc(doc, alingmentFile);
		Node clustersElem = genotypeAnalysesElem.appendChild(doc.createElement("clusters"));
		Element toolClusterElem = (Element) clustersElem.appendChild(doc.createElement("cluster"));
		toolClusterElem.setAttribute("id", taxonomyId);
		toolClusterElem.setAttribute("name", taxonomyId);
		Element descriptionElem = (Element) toolClusterElem.appendChild(doc.createElement("description"));
		descriptionElem.appendChild(doc.createTextNode(taxonomyId));

		for (Map<String, List<Sequence>> subtypeToSequences : genotypeToSubtypeToSequences.values()) {
			Sequence firstSeq = subtypeToSequences.values().iterator().next().get(0);
			Element taxusElem = (Element) toolClusterElem.appendChild(doc.createElement("taxus"));
			taxusElem.setAttribute("name", firstSeq.id);
		}

		Element analysisElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("analysis"));
		analysisElem.setAttribute("id", "blast");
		analysisElem.setAttribute("type", "blast");

		Element identifyElem = (Element) analysisElem.appendChild(doc.createElement("identify"));
		identifyElem.appendChild(doc.createTextNode("\n            *\n        "));

		Element cutoffElem = (Element) analysisElem.appendChild(doc.createElement("cutoff"));
		cutoffElem.appendChild(doc.createTextNode("\n          50\n        "));

		Element optionsElem = (Element) analysisElem.appendChild(doc.createElement("options"));
		optionsElem.appendChild(doc.createTextNode("\n          -q -1 -r 1\n        "));


		return doc;
	}

	/**
	 * Add the genotype-analyses Element to the top of the file.
	 * @param doc
	 * @param filename
	 * @return
	 */
	private static Element regaGenotypeAnalysesDoc(Document doc, String filename) {
		Element genotypeAnalysesElem = (Element) doc.appendChild(doc.createElement("genotype-analyses"));
		Element alignmentElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("alignment"));
		alignmentElem.setAttribute("file", new File(filename).getName());
		alignmentElem.setAttribute("trim", "true");
		return genotypeAnalysesElem;

	}

	/**
	 * Create phylo-{taxonomyId}.xml xml Document
	 * @param fastaAlingmentFile - all the fasta sequences for the analysis (must be formated as documeted on top)
	 * @param genotypeToSubtypeToSequences  - Map<genotype, Map<subType, all the sequences that belong to the subType> >
	 * @return the new phylo-{taxonomyId}.xml Document
	 * @throws ParserConfigurationException
	 */
	private static Document clustersDoc(
			String fastaAlingmentFile, Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences)
					throws ParserConfigurationException {
		Document doc = newDocument();
		List<String> clusterIds = new ArrayList<String>();
		Map<String, List<Sequence>> majorList = new HashMap<String, List<Sequence>>();

		// Add sub type clusters, needed for phylo minor. (<identify> 1a,1b,1c,... )

		String outgroupName = null;
		Element genotypeAnalysesElem = regaGenotypeAnalysesDoc(doc, fastaAlingmentFile);
		Element clustersElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("clusters"));
		for (Entry<String, Map<String, List<Sequence>>> i : genotypeToSubtypeToSequences.entrySet()) {
			String genotype = i.getKey();
			Map<String, List<Sequence>> subtypeToSequences = i.getValue();
			Element parentElem = clustersElem;
			clusterIds.add(genotype);
			final Element subtypeParentElem = parentElem;

			for (Entry<String, List<Sequence>> j : subtypeToSequences.entrySet()) {
				String subtype = j.getKey();
				List<Sequence> subtypeSeqs = j.getValue();
				if (genotype.equals(OUT_GROUP_NAME) && subtypeSeqs.size() > 0)
					outgroupName = subtypeSeqs.get(0).id; // save out group name

				if (!subtype.isEmpty()) {
					if (!majorList.containsKey(genotype)) 
						majorList.put(genotype, new ArrayList<Sequence>());
					majorList.get(genotype).add(subtypeSeqs.get(0));
				}

				Element subtypeClusterElem = (Element) subtypeParentElem.appendChild(doc.createElement("cluster"));
				String subtypeClusterId;
				String subtypeDesc;
				if(subtype.startsWith("unassigned:")) {
					subtypeDesc = "Genotype "+genotype+" unassigned subtype defined by sequence "+subtype.replace("unassigned:", "");
					subtypeClusterId = genotype+":"+subtype;
				} else {
					subtypeDesc = "Genotype "+genotype+" subtype "+subtype;
					subtypeClusterId = genotype+subtype;
				}
				subtypeClusterElem.setAttribute("id", subtypeClusterId);
				subtypeClusterElem.setAttribute("name", subtypeDesc);

				for (Sequence seq : subtypeSeqs) {
					Element taxusElem = (Element) subtypeClusterElem.appendChild(doc.createElement("taxus"));
					taxusElem.setAttribute("name", seq.id);
				};
			};
		};

		// Add genotype clusters, needed for phylo major. (<identify> 1,2,3,... )

		for (Map.Entry<String, List<Sequence>> e: majorList.entrySet()) {
			String genotype  = e.getKey();
			List<Sequence> sequences = e.getValue();
			Element majorClusterElem = (Element) clustersElem.appendChild(doc.createElement("cluster"));
			majorClusterElem.setAttribute("id", genotype);
			majorClusterElem.setAttribute("name", "Genotype " + genotype);
			for (Sequence seq : sequences) {
				Element taxusElem = (Element) majorClusterElem.appendChild(doc.createElement("taxus"));
				taxusElem.setAttribute("name", seq.id);
			};
		}
		

		createAnalysisElement(doc, clusterIds, genotypeAnalysesElem, AnalysisType.Major, outgroupName);
		clusterIds.remove(OUT_GROUP_NAME);
		createSelfScanElement(doc, clusterIds, genotypeAnalysesElem);

		int n = 1;
		for (Entry<String, Map<String, List<Sequence>>> i : genotypeToSubtypeToSequences.entrySet()) {
			clusterIds.clear();
			Map<String, List<Sequence>> subtypeToSequences = i.getValue();
			if (!i.getKey().equals(OUT_GROUP_NAME))
				for (Entry<String, List<Sequence>> j : subtypeToSequences.entrySet()) {
					if (!j.getKey().isEmpty()) { 
						clusterIds.add(i.getKey() + j.getKey());
					}
				}
			if (!clusterIds.isEmpty()) {
				AnalysisType minor = AnalysisType.Minor;
				minor.setAnalysisNumber(i.getKey());
				n++;
				createAnalysisElement(doc, clusterIds, genotypeAnalysesElem, minor, outgroupName);
			}
		}

		return doc;
	}

	/**
	 * Create self scan analysis xml Element that can be embedded in the phylo...xml document.
	 * @param doc - The xml documnet that we are creating.
	 * @param clusterIds the clusters that will be used in the scan analysis
	 * @param genotypeAnalysesElem the element will be added to this element in the documnet.
	 * @return
	 */
	private static Element createSelfScanElement(Document doc,
			List<String> clusterIds, Element genotypeAnalysesElem) {
		Element analysisElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("analysis"));
		analysisElem.setAttribute("id", AnalysisType.MajorSelfScan.analysisName());
		analysisElem.setAttribute("type", "scan");

		Element windowElem = (Element) analysisElem.appendChild(doc.createElement("window"));
		Element stepElem = (Element) analysisElem.appendChild(doc.createElement("step"));

		windowElem.setTextContent("500");
		stepElem.setTextContent("100");

		createAnalysisElement(doc, clusterIds, analysisElem, AnalysisType.Empty, null);

		return analysisElem;
	}

	/**
	 * 
	 * @param doc - The xml documnet that we are creating.
	 * @param clusterIds - the clusters that will be used in the scan analysis
	 * @param genotypeAnalysesElem the element will be added to this element in the documnet.
	 * @param analysisType - analysis type attribute for the analysis element example Major -> type="phylo-major"
	 * @param outgroupName - taxon name of outgroup or null if there is no out group.
	 * @return
	 */
	private static Element createAnalysisElement(Document doc,
			List<String> clusterIds, Element genotypeAnalysesElem,
			AnalysisType analysisType, String outgroupName) {

		Element analysisElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("analysis"));
		if (analysisType != AnalysisType.Empty)
			analysisElem.setAttribute("id", analysisType.analysisName());
		analysisElem.setAttribute("type", "paup");

		Element identifyElem = (Element) analysisElem.appendChild(doc.createElement("identify"));

		String identifyStr = "";
		for (String s : clusterIds) {
			if (!identifyStr.isEmpty())
				identifyStr += ",";
			identifyStr += s;
		}
		identifyElem.appendChild(doc.createTextNode("\n            "+identifyStr+"\n        "));

		Element cutoffElem = (Element) analysisElem.appendChild(doc.createElement("cutoff"));
		cutoffElem.appendChild(doc.createTextNode("\n          70\n        "));

		String outgroup = "";
		String method = "outroot=monophyl";

		//Note: paup crashes when adding outgroup to phylo-minor.
		if (outgroupName != null && analysisType == AnalysisType.Major){
			outgroup = "              outgroup " + outgroupName + ";\n";
			method = "rootmethod=midpoint";
		}

		Element blockElem = (Element) analysisElem.appendChild(doc.createElement("block"));
		blockElem.appendChild(doc.createTextNode("\n"+
				"              begin paup;\n"+
				"              log file=paup.log replace=yes;\n"+
				"              exclude gapped;\n"+
				"              export format=nexus file=paup.nex replace=yes;\n"+
				"              set criterion=distance " + method + ";\n" +
				outgroup +
				"              dset distance=HKY NegBrLen=Prohibit;\n"+
				"              NJ;\n"+
				"              savetree format=nexus brlens=yes file=paup.tre replace=yes;\n"+
				"              bootstrap nreps=100 search=nj;\n"+
				"              end;\n"+
				"              quit;\n"+
				"        "));

		Element optionsElem = (Element) analysisElem.appendChild(doc.createElement("options"));
		optionsElem.appendChild(doc.createTextNode("\n          log,alignment,tree,inner,outer\n        "));

		return analysisElem;
	}

	/**
	 * Create xml Document
	 * @return
	 * @throws ParserConfigurationException
	 */
	private static Document newDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		return doc;
	}

	/**
	 * Set some configuration so the xml file will human readable. 
	 */
	private static void prettyPrint(Document document, OutputStream outputStream, int indent) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		if (indent > 0) {
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(indent));
		}
		Result result = new StreamResult(outputStream);
		Source source = new DOMSource(document);
		transformer.transform(source, result);
	}

}
