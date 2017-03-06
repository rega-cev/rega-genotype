package rega.genotype.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
import rega.genotype.AlignmentException;
import rega.genotype.ApplicationException;
import rega.genotype.BlastAnalysis.Region;
import rega.genotype.FileFormatException;
import rega.genotype.ParameterProblemException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlign;
import rega.genotype.SequenceAlignment;
import rega.genotype.utils.EdirectUtil;
import rega.genotype.utils.FileUtil;

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

	/**
	 * phylo-'cluster'.fasta
	 */
	public static class PhyloAlignment {
		String alignmentName;
		Map<String, Map<String, List<RegaSequence>>> genotypeToSubtypeToSequences;

		public PhyloAlignment(File fastaAlingmentFile, String fastaHeaderRegix) 
				throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException, ApplicationException {
			this(alignmentNameFromFileName(fastaAlingmentFile), fastaAlingmentFile, fastaHeaderRegix);
		}

		public PhyloAlignment(String alignmentName,
				File fastaAlingmentFile, String fastaHeaderRegix) throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException, ApplicationException {
			this.alignmentName = alignmentName;
			genotypeToSubtypeToSequences = new HashMap<String, Map<String,List<RegaSequence>>>();

			List<RegaSequence> sequences = readSequences(fastaAlingmentFile, fastaHeaderRegix);

			// Map<genotype, all the sequences that belong to the genotype>
			Map<String, List<RegaSequence>> genotypeToSequences = new HashMap<String, List<RegaSequence>>();		
			for (RegaSequence seq : sequences) {
				if (genotypeToSequences.get(seq.genotype) == null)
					genotypeToSequences.put(seq.genotype, new ArrayList<RegaSequence>());
				genotypeToSequences.get(seq.genotype).add(seq);
			}

			// Map<genotype, Map<subType, all the sequences that belong to the subType> >
			for (Entry<String, List<RegaSequence>> a : genotypeToSequences.entrySet()) {
				Map<String, List<RegaSequence>> subtypeMap = new HashMap<String, List<RegaSequence>>();

				for (RegaSequence seq : a.getValue()) {
					if (subtypeMap.get(seq.subtype) == null)
						subtypeMap.put(seq.subtype, new ArrayList<RegaSequence>());
					subtypeMap.get(seq.subtype).add(seq);
				}
				genotypeToSubtypeToSequences.put(a.getKey(), subtypeMap);
			}
		}

		public RegaSequence find(String accessionNumber) {
			for (Map<String, List<RegaSequence>> subtypeToSequences :
				genotypeToSubtypeToSequences.values())
				for (List<RegaSequence> seqs: subtypeToSequences.values())
					for (RegaSequence seq: seqs)
						if (seq.accesssionNumber.equals(accessionNumber))
							return seq;

			return null;
		}

		public String aligmentFastaFileName() {
			return "phylo-" + alignmentName + ".fasta";
		}

		public String aligmentXmlFileName() {
			return "phylo-" + alignmentName + ".xml";
		}

		public static String alignmentNameFromFileName(File fastaFile) {
			return fastaFile.getName().replace("phylo-", "").replace(".fasta", "");
		}
	}

	public enum Mode {OneAlilgnment, ManyAlignemnts}

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

	private static class RegaSequence extends Sequence {
		public RegaSequence(AbstractSequence other) {
			super(other.getName(), false, other.getDescription(), other.getSequence(), null);
		}
		String genotype = null;
		String subtype = null;
		String accesssionNumber = null;
	}

	/**
	 * It is possible to run this as a java application.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (args.length < 3) {
			System.err.println("Usage: fastaToRega input.fasta output-dir");
			return;
		}

		String fastaAlingmentFile = args[0];
		String toolDir = args[1];
		String taxonomyId = args[2];
		String clustalwPath = (args.length > 3) ? args[3] : null;

		createTool(taxonomyId, new File(fastaAlingmentFile), 
				new File(toolDir), clustalwPath);
	}

	public static void createTool(String taxonomyId,
			File fastaAlingmentFile, File toolDir) throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException, ParserConfigurationException, TransformerException, ApplicationException, AlignmentException {
		createTool(taxonomyId, fastaAlingmentFile, toolDir, "(\\d++)([^_]++)_(.*)"); // Tulio regix
	}

	/**
	 * Main function: Create blast.xml and phylo xml files 
	 */
	public static void createTool(String taxonomyId, File fastaAlingmentFile,
			File toolDir, String fastaHeaderRegix)
					throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException, ParserConfigurationException, TransformerException, ApplicationException, AlignmentException {

		PhyloAlignment phyloAlignment = new PhyloAlignment(taxonomyId, fastaAlingmentFile, fastaHeaderRegix);

		// create phylo-taxonomyId.xml file.
		Document clustalDoc = clustersDoc(phyloAlignment);
		writeXml(new File(toolDir, phyloAlignment.aligmentXmlFileName()), clustalDoc);

		// create blast.xml
		List<PhyloAlignment> alignments = new ArrayList<FastaToRega.PhyloAlignment>();
		Document blastDoc = blastDocument(Mode.OneAlilgnment, fastaAlingmentFile, alignments);
		writeXml(new File(toolDir, "blast.xml"), blastDoc);
	}

	public static void createTool(List<PhyloAlignment> phyloAlignments,
			File toolDir)
					throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException, ParserConfigurationException, TransformerException, ApplicationException, InterruptedException, AlignmentException {
		File blastFasta = new File(toolDir, "blast.fasta");
		autoCreateBlastFasta(phyloAlignments, new File(toolDir, "blast.fasta"));
		createTool(blastFasta, phyloAlignments, toolDir);
	}

	/**
	 * Main function: Create blast.xml and phylo xml files 
	 */
	public static void createTool(File blastFasta, List<PhyloAlignment> phyloAlignments,
			File toolDir) throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException, ParserConfigurationException, TransformerException, AlignmentException {

		// create phylo-taxonomyId.xml files.
		for (PhyloAlignment phyloAlignment: phyloAlignments) {
			Document clustalDoc = clustersDoc(phyloAlignment);
			writeXml(new File(toolDir, phyloAlignment.aligmentXmlFileName()), clustalDoc);
		}

		// create blast.xml
		Document blastDoc = blastDocument(Mode.ManyAlignemnts, blastFasta, phyloAlignments);
		writeXml(new File(toolDir, "blast.xml"), blastDoc);
	}

	/**
	 * Download from gen bank all the full sequence for every first sequence in a sub type. 
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws ApplicationException 
	 * @throws FileFormatException 
	 * @throws ParameterProblemException 
	 */
	public static void autoCreateBlastFasta(List<PhyloAlignment> phyloAlignments, File blastFasta) throws ApplicationException, IOException, InterruptedException, ParameterProblemException, FileFormatException {
		List<String> accessionNumbers = new ArrayList<String>();
		for (PhyloAlignment phyloAlignment: phyloAlignments) {
			for (Entry<String, Map<String, List<RegaSequence>>> i : phyloAlignment.genotypeToSubtypeToSequences.entrySet()) {
				Map<String, List<RegaSequence>> subtypeToSequences = i.getValue();
				for (Entry<String, List<RegaSequence>> j : subtypeToSequences.entrySet()) {
					if (j.getValue().size() > 0)
						accessionNumbers.add(j.getValue().get(0).accesssionNumber);
				}
			}
		}

		File query = File.createTempFile("accession-numbers", "");
		File ncbiFasta = File.createTempFile("ncbi-seq", "");

		EdirectUtil.createNcbiAccQuery(accessionNumbers, query);
		EdirectUtil.queryFasta(query, ncbiFasta);

		SequenceAlignment alignment = new SequenceAlignment(new FileInputStream(ncbiFasta), 
				SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA, false);

		for (AbstractSequence seq: alignment.getSequences()) {
			String accessionNumber = EdirectUtil.getAccessionNumberFromNCBI(seq.getName());
			// NCBI addes version numbers to accession number.
			if (accessionNumber.contains(".")
					&& !accessionNumbers.contains(accessionNumber))
				accessionNumber = accessionNumber.substring(0, accessionNumber.indexOf("."));
			seq.setName(accessionNumber);
		}

		alignment.writeOutput(new FileOutputStream(blastFasta),
				SequenceAlignment.FILETYPE_FASTA);
	}

	private static List<RegaSequence> readSequences(File fastaAlingmentFile, String regix) throws FileNotFoundException, ParameterProblemException, IOException, FileFormatException, ApplicationException {
		Pattern pattern = Pattern.compile(regix);
		List<RegaSequence> sequences = new ArrayList<RegaSequence>();

		SequenceAlignment seqAlign = new SequenceAlignment(new FileInputStream(fastaAlingmentFile), 
				SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);

		for (AbstractSequence seq : seqAlign.getSequences()) {
			Matcher matcher = pattern.matcher(seq.getName());
			if(matcher.find()) {
				RegaSequence sequence = new RegaSequence(seq);
				sequence.genotype = matcher.group(1);
				sequence.subtype = matcher.group(2);
				sequence.accesssionNumber = matcher.group(3);
				sequences.add(sequence);
			} else if (seq.getName().startsWith(OUT_GROUP_NAME)) {
				RegaSequence sequence = new RegaSequence(seq);
				sequence.genotype = OUT_GROUP_NAME;
				sequence.subtype = "";
				sequences.add(sequence);
			} else {
				throw new ApplicationException("Sequence: '" + seq.getName() + "' name is not properlly formated.");
			}
		}

		return sequences;
	}

	/**
	 * If region is a region of refSeq return the region info, else return null.
	 */
	public static Region region(AbstractSequence refSeq,
			AbstractSequence region, File workDir) throws AlignmentException {

		SequenceAlignment example = SequenceAlign.pairAlign(refSeq, region, workDir);

		System.err.println("refSeq = " + refSeq.getLength()
				+ " example = " + example.getLength() + " region = " + region.getLength());
		
		int diff = example.getLength() - region.getLength();

		if (diff == 0)
			return null;

		rega.genotype.Sequence query = (rega.genotype.Sequence) 
				example.getSequences().get(1);

		int start = Math.max(0, query.firstNonGapPosition() - diff);
		int end = Math.min(region.getLength(), query.lastNonGapPosition() + diff);

		return new Region(refSeq.getName(), start, end);
	}

	/**
	 * Create blast.xml file
	 * @param blastFasta - blast.fasta
	 * @param taxonomyId - used for the cluster name.
	 * @param genotypeToSubtypeToSequences - Map<genotype, Map<subType, all the sequences that belong to the subType> >
	 * @return the new blast.xml Document
	 * @throws ParserConfigurationException
	 * @throws FileFormatException 
	 * @throws IOException 
	 * @throws ParameterProblemException 
	 * @throws FileNotFoundException 
	 * @throws AlignmentException 
	 */
	private static Document blastDocument(Mode mode,
			File blastFasta, List<PhyloAlignment> phyloAlignments)
					throws ParserConfigurationException, FileNotFoundException, ParameterProblemException, IOException, FileFormatException, AlignmentException {

		Document doc = newDocument();
		Element genotypeAnalysesElem = regaGenotypeAnalysesDoc(doc, blastFasta.getName());
		Node clustersElem = genotypeAnalysesElem.appendChild(doc.createElement("clusters"));

		File tempDirectory = FileUtil.createTempDirectory();

		for (PhyloAlignment phyloAlignment: phyloAlignments) {
			String taxonomyId = phyloAlignment.alignmentName;
			Element toolClusterElem = (Element) clustersElem.appendChild(doc.createElement("cluster"));
			toolClusterElem.setAttribute("id", taxonomyId);
			toolClusterElem.setAttribute("name", taxonomyId);
			Element descriptionElem = (Element) toolClusterElem.appendChild(doc.createElement("description"));
			descriptionElem.appendChild(doc.createTextNode(taxonomyId));

			if (mode == Mode.OneAlilgnment) {
				// Choose the first sequence in every genotype cluster to be the representative in blast.xml
				for (Map<String, List<RegaSequence>> subtypeToSequences :
					phyloAlignment.genotypeToSubtypeToSequences.values()) {
					RegaSequence firstSeq = subtypeToSequences.values().iterator().next().get(0);
					Element taxusElem = (Element) toolClusterElem.appendChild(doc.createElement("taxus"));
					taxusElem.setAttribute("name", firstSeq.getName());
				}
			}else {
				SequenceAlignment seqAlign = new SequenceAlignment(new FileInputStream(blastFasta), 
						SequenceAlignment.FILETYPE_FASTA, SequenceAlignment.SEQUENCE_DNA);
				// Find all sequences in blast.fasta that belong to the alignment (there ac number can be found)
				for (AbstractSequence seq: seqAlign.getSequences()) {
					RegaSequence region = phyloAlignment.find(seq.getName());
					if (region != null){
						Element taxusElem = (Element) toolClusterElem.appendChild(doc.createElement("taxus"));
						taxusElem.setAttribute("name", seq.getName());
						region(seq, region, tempDirectory);
					}
				}
			}
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
	 * Create phylo-{taxonomyId}.xml xml Document
	 * @param fastaAlingmentFile - all the fasta sequences for the analysis (must be formated as documeted on top)
	 * @param genotypeToSubtypeToSequences  - Map<genotype, Map<subType, all the sequences that belong to the subType> >
	 * @return the new phylo-{taxonomyId}.xml Document
	 * @throws ParserConfigurationException
	 */
	private static Document clustersDoc(PhyloAlignment phyloAlignment) throws ParserConfigurationException {
		Document doc = newDocument();
		List<String> genotypeClusterIds = new ArrayList<String>();
		Map<String, List<RegaSequence>> majorList = new HashMap<String, List<RegaSequence>>();

		// Add sub type clusters, needed for phylo minor. (<identify> 1a,1b,1c,... )

		String outgroupName = null;
		Element genotypeAnalysesElem = regaGenotypeAnalysesDoc(doc, phyloAlignment.aligmentFastaFileName());
		Element clustersElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("clusters"));
		for (Entry<String, Map<String, List<RegaSequence>>> i : phyloAlignment.genotypeToSubtypeToSequences.entrySet()) {
			String genotype = i.getKey();
			Map<String, List<RegaSequence>> subtypeToSequences = i.getValue();
			Element parentElem = clustersElem;
			genotypeClusterIds.add(genotype);
			final Element subtypeParentElem = parentElem;

			for (Entry<String, List<RegaSequence>> j : subtypeToSequences.entrySet()) {
				String subtype = j.getKey();
				List<RegaSequence> subtypeSeqs = j.getValue();
				if (genotype.equals(OUT_GROUP_NAME) && subtypeSeqs.size() > 0)
					outgroupName = subtypeSeqs.get(0).getName(); // save out group name

				if (!subtype.isEmpty()) {
					if (!majorList.containsKey(genotype)) 
						majorList.put(genotype, new ArrayList<RegaSequence>());
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

				for (RegaSequence seq : subtypeSeqs) {
					Element taxusElem = (Element) subtypeClusterElem.appendChild(doc.createElement("taxus"));
					taxusElem.setAttribute("name", seq.getName());
				};
			};
		};

		// Add genotype clusters, needed for phylo major. (<identify> 1,2,3,... )

		if (phyloAlignment.genotypeToSubtypeToSequences.size() > 1) {// 1 genotype -> only major
			for (Map.Entry<String, List<RegaSequence>> e: majorList.entrySet()) {
				String genotype  = e.getKey();
				List<RegaSequence> sequences = e.getValue();
				Element majorClusterElem = (Element) clustersElem.appendChild(doc.createElement("cluster"));
				majorClusterElem.setAttribute("id", genotype);
				majorClusterElem.setAttribute("name", "Genotype " + genotype);
				for (RegaSequence seq : sequences) {
					Element taxusElem = (Element) majorClusterElem.appendChild(doc.createElement("taxus"));
					taxusElem.setAttribute("name", seq.getName());
				};
			}

			createAnalysisElement(doc, genotypeClusterIds, genotypeAnalysesElem, AnalysisType.Major, outgroupName);
			genotypeClusterIds.remove(OUT_GROUP_NAME);
			createSelfScanElement(doc, genotypeClusterIds, genotypeAnalysesElem);

			for (Entry<String, Map<String, List<RegaSequence>>> i : 
				phyloAlignment.genotypeToSubtypeToSequences.entrySet()) {
				List<String> clusterIds = new ArrayList<String>();
				Map<String, List<RegaSequence>> subtypeToSequences = i.getValue();
				if (!i.getKey().equals(OUT_GROUP_NAME))
					for (Entry<String, List<RegaSequence>> j : subtypeToSequences.entrySet()) {
						if (!j.getKey().isEmpty()) { 
							clusterIds.add(i.getKey() + j.getKey());
						}
					}
				if (!clusterIds.isEmpty()) {
					AnalysisType minor = AnalysisType.Minor;
					minor.setAnalysisNumber(i.getKey());
					createAnalysisElement(doc, clusterIds, genotypeAnalysesElem, minor, outgroupName);
				}
			}
		} else {
			String genotype = null;
			List<String> clusterIds = new ArrayList<String>();
			for (Entry<String, Map<String, List<RegaSequence>>> i : 
				phyloAlignment.genotypeToSubtypeToSequences.entrySet()) {
				genotype = i.getKey();
				Map<String, List<RegaSequence>> subtypeToSequences = i.getValue();
				if (!i.getKey().equals(OUT_GROUP_NAME))
					for (Entry<String, List<RegaSequence>> j : subtypeToSequences.entrySet()) {
						clusterIds.add(genotype + j.getKey());
					}
				else
					clusterIds.add(OUT_GROUP_NAME);
			}
			createAnalysisElement(doc, clusterIds, genotypeAnalysesElem, AnalysisType.Major, outgroupName);
			clusterIds.remove(OUT_GROUP_NAME);// TODO check out group
			createSelfScanElement(doc, clusterIds, genotypeAnalysesElem);
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
	 * Add the genotype-analyses Element to the top of the file.
	 * @param doc
	 * @param filename
	 * @return
	 */
	private static Element regaGenotypeAnalysesDoc(Document doc, String fastaFileName) {
		Element genotypeAnalysesElem = (Element) doc.appendChild(doc.createElement("genotype-analyses"));
		Element alignmentElem = (Element) genotypeAnalysesElem.appendChild(doc.createElement("alignment"));
		alignmentElem.setAttribute("file", fastaFileName);
		alignmentElem.setAttribute("trim", "true");
		return genotypeAnalysesElem;

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

	private static void writeXml(File xmlFile, Document clustalDoc) throws FileNotFoundException,
			TransformerException, IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(xmlFile);
			prettyPrint(clustalDoc, fos, 4);
		} finally {
			if (fos != null)
				fos.close();
		}
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
