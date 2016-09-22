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
		Major, Minor;

		private int analysisNumber = 0;

		public String analysisName() {
			switch (this) {
			case Major: return "phylo-major";
			case Minor: 
				return "phylo-minor-" + analysisNumber;
			}
			return "";
		}

		public int getAnalysisNumber() {
			return analysisNumber;
		}

		public void setAnalysisNumber(int analysisNumber) {
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

	public static void createTool(String taxonomyId, String fastaAlingmentFile, String toolDir) throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException, ParserConfigurationException, TransformerException {
		// may need to update pattern to extract genotype string from FASTA seq id
		Pattern pattern = Pattern.compile("(\\d[^_]*)\\??_.*");;

		List<Sequence> sequences = new ArrayList<Sequence>();

		SequenceAlignment seqAlign = new SequenceAlignment(new FileInputStream(new File(fastaAlingmentFile)), 
				SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);

		for (AbstractSequence seq : seqAlign.getSequences()) {
			Matcher matcher = pattern.matcher(seq.getName());
			if(matcher.find()) {
				Sequence sequence = new Sequence();
				sequence.id = seq.getName();
				String genotypeSubtype = matcher.group(1);
				sequence.genotype = genotypeSubtype.substring(0, 1);
				sequence.subtype = genotypeSubtype.substring(1);
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

		Map<String, List<Sequence>> genotypeToSequences = new HashMap<String, List<Sequence>>();		
		for (Sequence seq : sequences) {
			if (genotypeToSequences.get(seq.genotype) == null)
				genotypeToSequences.put(seq.genotype, new ArrayList<Sequence>());
			genotypeToSequences.get(seq.genotype).add(seq);
		}

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

		Document clustalDoc = clustersDoc(fastaAlingmentFile, genotypeToSubtypeToSequences);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(toolDir, "phylo-" + taxonomyId + ".xml"));
			prettyPrint(clustalDoc, fos, 4);
		} finally {
			if (fos != null)
				fos.close();
		}

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

	private static Document blastDocument(
			String filename, String taxonomyId, 
			Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences)
					throws ParserConfigurationException {
		Document doc = newDocument();
		Element genotypeAnalysesElem = regaGenotypeAnalysesDoc(doc, filename);
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


	private static Element regaGenotypeAnalysesDoc(Document doc, String filename) {
		Element genotypeAnalysesElem = (Element) doc.appendChild(doc.createElement("genotype-analyses"));
		Element alignmentElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("alignment"));
		alignmentElem.setAttribute("file", new File(filename).getName());
		alignmentElem.setAttribute("trim", "true");
		return genotypeAnalysesElem;

	}

	private static Document clustersDoc(
			String filename, Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences)
					throws ParserConfigurationException {
		Document doc = newDocument();
		List<String> clusterIds = new ArrayList<String>();
		Map<String, List<Sequence>> majorList = new HashMap<String, List<Sequence>>();

		// Add sub type clusters, needed for phylo minor. (<identify> 1a,1b,1c,... )

		String outgroupName = null;
		Element genotypeAnalysesElem = regaGenotypeAnalysesDoc(doc, filename);
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
				minor.setAnalysisNumber(n);
				n++;
				createAnalysisElement(doc, clusterIds, genotypeAnalysesElem, minor, outgroupName);
			}
		}

		return doc;
	}

	private static Element createAnalysisElement(Document doc,
			List<String> clusterIds, Element genotypeAnalysesElem,
			AnalysisType analysisType, String outgroupName) {

		Element analysisElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("analysis"));
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

	private static Document newDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		return doc;
	}


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
