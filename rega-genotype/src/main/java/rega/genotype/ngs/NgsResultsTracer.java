package rega.genotype.ngs;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rega.genotype.NgsSequence.BucketData;
import rega.genotype.NgsSequence.Contig;
import rega.genotype.ResultTracer;
import rega.genotype.ngs.model.DiamondBucket;
import rega.genotype.ngs.model.NgsResultsModel;
import rega.genotype.ngs.model.NgsResultsModel.State;

/**
 * JSON file that monitors NGS analysis state.
 * Used by NgsWidget.
 * 
 * @author michael
 */
public class NgsResultsTracer extends ResultTracer{
	public static final String NGS_RESULTS_FILL = "ngs-results.xml";

	// Analysis variables.

	private File workDir;
	private NgsResultsModel model = new NgsResultsModel(); // Every thing is stored in a model in case we need to print also to json.
	private long startTime = System.currentTimeMillis();

	public NgsResultsTracer(File workDir) throws IOException {
		super(createResultsFile(workDir));
		this.workDir = workDir;
	}

	public static File ngsRsultsFile(File workDir) {
		return new File(workDir, "ngs-results.xml");
	}

	private static File createResultsFile(File workDir) throws IOException {
		workDir.mkdirs();
		File ngsRsultsFile = ngsRsultsFile(workDir);
		ngsRsultsFile.createNewFile();
		return ngsRsultsFile;
	}

	public static NgsResultsTracer read(File workDir) {
		return null; // TODO
	}

	private Long time() {
		return System.currentTimeMillis() - startTime;
	}

	// ngs-results.xml is written incrementally -> every step needs to be written separately.
	public void printInit() {
		printlnOpenElement("init");
		add("pe-1-file", model.getFastqPE1FileName());
		add("pe-2-file", model.getFastqPE1FileName());
		add("end-time-ms", time());
		printlnCloseLastElement();
		w.flush();
	}

	public void printQC1() {
		printlnOpenElement("qc1");
		add("read-length", model.getReadLength());
		add("read-count", model.getReadCountInit());
		add("end-time-ms", time());
		printlnCloseLastElement();
		w.flush();
	}

	public void printPreprocessing() {
		printlnOpen("<preprocessing>");
		add("end-time-ms", time());
		printlnClose("</preprocessing>");
		w.flush();
	}

	public void printQC2() {
		printlnOpen("<qc2>");
		add("read-length", model.getReadLength());
		add("read-count", model.getReadCountAfterPrepocessing());
		add("end-time-ms", time());
		printlnClose("</qc2>");
		w.flush();
	}

	public void printFiltring() {
		printlnOpen("<filtring>");
		for (DiamondBucket b: model.getDiamondBlastResults().values()) {
			printlnOpen("<diamond-bucket id=\"" + b.getTaxonId() + "\" >");
			add("ancestors", b.getAncestors());
			add("scientific-name", b.getScientificName());
			add("read-count-total", b.getReadCountTotal());
			printlnClose("</diamond-bucket>");
		}
		add("end-time-ms", time());
		printlnClose("</filtring>");
		w.flush();
	}

	public void printAssemblyOpen() {
		printlnOpenElement("assembly");
		w.flush();
	}

	public void printAssemblybucketOpen(
			BucketData bucketData, List<Contig> contigs) {
		printlnOpenElement("bucket",
				"id=\"" + bucketData.getDiamondBucket() + "__" + bucketData.getRefName() + "\"");
		add("diamond_bucket", bucketData.getDiamondBucket());
		add("ref_name", bucketData.getRefName());
		add("ref_description", bucketData.getRefDescription());
		add("ref_length", bucketData.getRefLen());
		// TODO: write spades results
		printlnOpen("<contigs>");
		for (Contig contig: contigs) {
			printlnOpen("<contig id=\"" + contig.getId() + "\">");
			add("length", contig.getLength());
			add("cov", contig.getCov());
			add("nucleotides", contig.getSequence());
			printlnClose("</contig>");
		}
		printlnClose("</contigs>");
	}

	public void printAssemblybucketClose() {
		printlnCloseLastElement("bucket");
		add("end-time-ms", time());
		w.flush();
	}

	public void printAssemblyError(String error) {
		add("error", error);
		if (openElements.peek().equals("bucket")) {
			openElements.pop();
			printlnClose("</bucket>");
		}
		w.flush();
	}

	public void printStop() {
		while (!openElements.isEmpty()) {
			printlnCloseLastElement();
		}
		writeXMLEnd();
	}

	public void printFatalError(String error) {
		model.setErrors(error);
		add("error", error);
		printStop();
	}

	public void setStateStart(State state) {
		model.setStateTime(state, System.currentTimeMillis());
	}

	public File getWorkDir() {
		return workDir;
	}

	public void setWorkDir(File workDir) {
		this.workDir = workDir;
	}

	public NgsResultsModel getModel() {
		return model;
	}
}