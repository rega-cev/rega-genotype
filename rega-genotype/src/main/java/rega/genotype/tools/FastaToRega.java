package rega.genotype.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import rega.genotype.SequenceAlignment;

public class FastaToRega {

	private static class Sequence {
		String genotype = null;
		String subtype = null;
		String id;
		String data;
		public String toString() { return genotype+"-"+subtype+":"+id; }
	}

	public static void main(String[] args) throws Exception {

		String directory = ".";

		String filename = "fastaToRega/HCV_alignment.fasta";

		// may need to update pattern to extract genotype string from FASTA seq id
		Pattern pattern = Pattern.compile("(\\d[a-z]*)\\??_.*");;

		List<Sequence> sequences = new ArrayList<Sequence>();

		SequenceAlignment seqAlign = new SequenceAlignment(new FileInputStream(new File(directory, filename)), 
				SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);

		seqAlign.getSequences().stream().forEach(seq -> {
			Matcher matcher = pattern.matcher(seq.getName());
			if(matcher.find()) {
				Sequence sequence = new Sequence();
				sequence.id = seq.getName();
				String genotypeSubtype = matcher.group(1);
				sequence.genotype = genotypeSubtype.substring(0, 1);
				sequence.subtype = genotypeSubtype.substring(1);
				sequence.data = seq.getSequence();
				sequences.add(sequence);
			}

		});


		try(PrintWriter writer = new PrintWriter(new File(directory, filename))) {
			sequences.forEach(seq -> {
				writer.println(">"+seq.id);
				writer.println(seq.data);
				writer.flush();
			});
		}

		Map<String, List<Sequence>> genotypeToSequences = sequences.stream().collect(Collectors.groupingBy(seq -> seq.genotype));
		Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences = new LinkedHashMap<String, Map<String, List<Sequence>>>();
		genotypeToSequences.forEach( (genotype, genotypeSeqs) -> {
			genotypeToSubtypeToSequences.put(genotype, genotypeSeqs.stream().collect(Collectors.groupingBy(seq -> seq.subtype)));
		});

		Document clustalDoc = clustersDoc(filename, genotypeToSubtypeToSequences);
		try(FileOutputStream fos = new FileOutputStream(new File(directory, "hcv.xml"))) {
			prettyPrint(clustalDoc, fos, 4);
		}

		Document blastDoc = blastDocument(filename, genotypeToSubtypeToSequences);
		try(FileOutputStream fos = new FileOutputStream(new File(directory, "hcvblast.xml"))) {
			prettyPrint(blastDoc, fos, 4);
		}



	}


	private static Document blastDocument(
			String filename, Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences)
					throws ParserConfigurationException {
		Document doc = newDocument();
		Element genotypeAnalysesElem = regaGenotypeAnalysesDoc(doc, filename);
		Node clustersElem = genotypeAnalysesElem.appendChild(doc.createElement("clusters"));
		Element hcvClusterElem = (Element) clustersElem.appendChild(doc.createElement("cluster"));
		hcvClusterElem.setAttribute("id", "1");
		hcvClusterElem.setAttribute("name", "HCV");
		Element descriptionElem = (Element) hcvClusterElem.appendChild(doc.createElement("description"));
		descriptionElem.appendChild(doc.createTextNode("HCV"));

		genotypeToSubtypeToSequences.forEach( (genotype, subtypeToSequences) -> {
			Sequence firstSeq = subtypeToSequences.values().iterator().next().get(0);
			Element taxusElem = (Element) hcvClusterElem.appendChild(doc.createElement("taxus"));
			taxusElem.setAttribute("name", firstSeq.id);
		});

		Element analysisElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("analysis"));
		analysisElem.setAttribute("id", "blast");
		analysisElem.setAttribute("type", "blast");

		Element identifyElem = (Element) analysisElem.appendChild(doc.createElement("identify"));
		identifyElem.appendChild(doc.createTextNode("\n            1\n        "));

		Element cutoffElem = (Element) analysisElem.appendChild(doc.createElement("cutoff"));
		cutoffElem.appendChild(doc.createTextNode("\n          50\n        "));

		Element optionsElem = (Element) analysisElem.appendChild(doc.createElement("options"));
		optionsElem.appendChild(doc.createTextNode("\n          -q -1 -r 1\n        "));


		return doc;
	}


	private static Element regaGenotypeAnalysesDoc(Document doc, String filename) {
		Element genotypeAnalysesElem = (Element) doc.appendChild(doc.createElement("genotype-analyses"));
		Element alignmentElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("alignment"));
		alignmentElem.setAttribute("file", filename);
		alignmentElem.setAttribute("trim", "true");
		return genotypeAnalysesElem;

	}

	private static Document clustersDoc(
			String filename, Map<String, Map<String, List<Sequence>>> genotypeToSubtypeToSequences)
					throws ParserConfigurationException {
		Document doc = newDocument();
		List<String> clusterIds = new ArrayList<String>();
		Element genotypeAnalysesElem = regaGenotypeAnalysesDoc(doc, filename);
		Element clustersElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("clusters"));
		genotypeToSubtypeToSequences.forEach( (genotype, subtypeToSequences) -> {
			Element parentElem = clustersElem;
			if(subtypeToSequences.size() > 1) {
				Element genotypeClusterElem = (Element) clustersElem.appendChild(doc.createElement("cluster"));
				String genotypeClusterId = "Geno_"+genotype;
				clusterIds.add(genotypeClusterId);
				genotypeClusterElem.setAttribute("id", genotypeClusterId);
				genotypeClusterElem.setAttribute("name", "Genotype "+genotype);
				parentElem = genotypeClusterElem;
			}
			final Element subtypeParentElem = parentElem;
			subtypeToSequences.forEach( (subtype, subtypeSeqs) -> {
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
				clusterIds.add(subtypeClusterId);
				subtypeClusterElem.setAttribute("id", subtypeClusterId);
				subtypeClusterElem.setAttribute("name", subtypeDesc);
				subtypeSeqs.forEach(seq -> {
					Element taxusElem = (Element) subtypeClusterElem.appendChild(doc.createElement("taxus"));
					taxusElem.setAttribute("name", seq.id);
				});
			});
		});

		Element analysisElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("analysis"));
		analysisElem.setAttribute("id", "pure");
		analysisElem.setAttribute("type", "paup");

		Element identifyElem = (Element) analysisElem.appendChild(doc.createElement("identify"));
		identifyElem.appendChild(doc.createTextNode("\n            "+String.join(",", clusterIds.toArray(new String[]{}))+"\n        "));

		Element cutoffElem = (Element) analysisElem.appendChild(doc.createElement("cutoff"));
		cutoffElem.appendChild(doc.createTextNode("\n          70\n        "));

		Element blockElem = (Element) analysisElem.appendChild(doc.createElement("block"));
		blockElem.appendChild(doc.createTextNode("\n"+
				"              begin paup;\n"+
				"              log file=paup.log replace=yes;\n"+
				"              exclude gapped;\n"+
				"              export format=nexus file=paup.nex replace=yes;\n"+
				//				"              outgroup 4a.ED43;\n"+ // need a different outgroup?
				"              set criterion=distance outroot=monophyl;\n"+
				"              dset distance=HKY NegBrLen=Prohibit;\n"+
				"              NJ;\n"+
				"              savetree format=nexus brlens=yes file=paup.tre replace=yes;\n"+
				"              bootstrap nreps=100 search=nj;\n"+
				"              end;\n"+
				"              quit;\n"+
				"        "));

		Element optionsElem = (Element) analysisElem.appendChild(doc.createElement("options"));
		optionsElem.appendChild(doc.createTextNode("\n          log,alignment,tree\n        "));

		return doc;
	}


	private static Document newDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		return doc;
	}


	private static void prettyPrint(Document document, OutputStream outputStream, int indent) throws Exception {
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
