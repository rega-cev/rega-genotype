package rega.genotype.ngs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import rega.genotype.Constants.Mode;
import rega.genotype.FileFormatException;
import rega.genotype.Sequence;
import rega.genotype.SequenceAlignment;
import rega.genotype.framework.async.LongJobsScheduler;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.ngs.model.NgsResultsModel;
import rega.genotype.ngs.model.NgsResultsModel.State;
import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.JobForm;
import rega.genotype.ui.framework.widgets.ChartTableWidget;
import rega.genotype.ui.framework.widgets.DownloadsWidget;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.ngs.CovMap;
import rega.genotype.utils.Utils;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.chart.WChartPalette;
import eu.webtoolkit.jwt.chart.WStandardPalette;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;

/**
 * Present ngs analysis state in job overview tab. 
 * 
 * @author michael
 */
public class NgsWidget extends WContainerWidget{

	private static final String PE1 = "QC report of reads 1";
	private static final String PE2 = "QC report of reads 2";
	private static final String SE = "Report";

	private ResultsView consensusTable;
	private File workDir;
	private WText subTypingHeaderT = new WText("<h2>Sub-Typing Tool Results</h2>");

	private static class FastqFilePair {
		public FastqFilePair(File original, File preprocessed) {
			this.original = original;
			this.preprocessed = preprocessed;
		}
		File original;
		File preprocessed;
	}

	public NgsWidget(final File workDir) {
		super();
		this.workDir = workDir;
		subTypingHeaderT.hide();
	}

	public void refresh(NgsResultsModel model, final OrganismDefinition organismDefinition) {
		clear();

		WContainerWidget horizontalLayout = new WContainerWidget(this);
		horizontalLayout.addStyleClass("flex-container");
		
		Template preprocessing = new Template(tr("ngs.preprocessing-view"));
		Template filtering = new Template(tr("ngs.filtering-view"));
		Template identification = new Template(tr("ngs.assembly-view"));

		fillStateTemplate(preprocessing, State.Preprocessing, model);
		fillStateTemplate(filtering, State.Diamond, model);
		fillStateTemplate(identification, State.Spades, model);

		preprocessing.bindEmpty("qc-1");
		preprocessing.bindEmpty("qc-2");
		preprocessing.bindEmpty("qcp-1");
		preprocessing.bindEmpty("qcp-2");
		preprocessing.bindEmpty("removed-pe1");
		preprocessing.bindEmpty("removed-pe2");

		filtering.bindEmpty("removed-pe1");
		filtering.bindEmpty("removed-pe2");

		preprocessing.setCondition("if-preprocessing", !model.getSkipPreprocessing());

		if (model.getState().code >= State.QC.code) {
			File qcDir = new File(workDir, NgsFileSystem.QC_REPORT_DIR);
			if (model.isPairEnd()) {
				preprocessing.bindWidget("qc-1", qcAnchor(NgsFileSystem.qcPE1File(qcDir), PE1));
				preprocessing.bindWidget("qc-2", qcAnchor(NgsFileSystem.qcPE2File(qcDir), PE2));
			} else {
				preprocessing.bindWidget("qc-1", qcAnchor(NgsFileSystem.qcSEFile(qcDir), SE));
			}
		}

		if (model.getState().code >= State.Diamond.code) {
			preprocessing.setCondition("if-finished", true);
			File qcDir = new File(workDir, NgsFileSystem.QC_REPORT_AFTER_PREPROCESS_DIR);
			if (model.isPairEnd()) {
				// qc
				preprocessing.bindWidget("qcp-1", qcAnchor(NgsFileSystem.qcPE1File(qcDir), PE1));
				preprocessing.bindWidget("qcp-2", qcAnchor(NgsFileSystem.qcPE2File(qcDir), PE2));
			} else {
				// qc
				preprocessing.bindWidget("qcp-1", qcAnchor(NgsFileSystem.qcSEFile(qcDir), SE));
			}
		}

		if (model.getState().code >= State.Spades.code) {
			filtering.setCondition("if-finished", true);
		}

		// top view

		Template view = new Template(tr("ngs.view"), horizontalLayout);
		view.addStyleClass("ngs-view flex-elem ");
		view.bindString("input-name", model.getInputName());
		view.bindWidget("preprocessing", preprocessing);
		view.bindWidget("filtering", filtering);
		view.bindWidget("assembly", identification);

		if (!model.getErrors().isEmpty() ){
			view.setCondition("if-errors", true);
			view.bindString("errors", model.getErrors());
		}

		// job state msg.

		String jobState = "";
		if (model.getState().code == State.QC.code)
			jobState = "<p> QC job state: Running</p>";
		else if (model.getState().code == State.Preprocessing.code)
			jobState = "<p> Preprocessing job state: Running</p>";
		else if (model.getState().code == State.QC2.code) 
			jobState = "<p> QC job state: Running</p>";
		else if (model.getState().code == State.Diamond.code) {
			String schedState = LongJobsScheduler.getInstance().getJobState(workDir);
			jobState = "<p> Diamond blast job state:" + schedState + "</p>";
		} else if (model.getState().code == State.Spades.code) {
			String schedState = LongJobsScheduler.getInstance().getJobState(workDir);
			jobState = "<p> Sapdes job state:" + schedState + "</p>";
		} 

		view.bindString("job-state", jobState);
			
		// style

		if (model.getState().code < State.Diamond.code){
			preprocessing.addStyleClass("working");
			filtering.addStyleClass("waiting");
			identification.addStyleClass("waiting");
		} else if (model.getState() == State.Diamond){
			filtering.addStyleClass("working");
			identification.addStyleClass("waiting");
		} else if (model.getState() == State.Spades){
			identification.addStyleClass("working");
		}

		boolean isBlastTool = organismDefinition.getToolConfig().getToolMenifest().isBlastTool();
		boolean showSingleResult = !isBlastTool && model.getConsensusBuckets().size() == 1;

		boolean hasUnassigned = false;
		List<ConsensusBucket> consensusBuckets = new ArrayList<ConsensusBucket>();
		if (isBlastTool) {
			consensusBuckets.addAll(model.getConsensusBuckets());
		} else {
			for (ConsensusBucket b: model.getConsensusBuckets())
				if (!b.getConcludedId().equals("Unassigned"))
					consensusBuckets.add(b);
				else
					hasUnassigned = true;
		}

		if (hasUnassigned)
			view.bindWidget("redirect-to-pan-viral", createPanViralToolAnchor());
		else
			view.bindEmpty("redirect-to-pan-viral");
		
		// consensus table.
		if (!consensusBuckets.isEmpty()) {
			WChartPalette palette = new WStandardPalette(WStandardPalette.Flavour.Muted);

			final NgsConsensusSequenceModel consensusModel = new NgsConsensusSequenceModel(
					consensusBuckets, model.getReadLength(), palette,
					organismDefinition.getToolConfig(), workDir);
			if (showSingleResult) { 
				SingleResultView singleView = new SingleResultView(consensusModel, workDir);
				addWidget(singleView);
			} else {
				consensusTable = new ResultsView(consensusModel, 
						NgsConsensusSequenceModel.READ_COUNT_COLUMN,
						NgsConsensusSequenceModel.COLOR_COLUMN,
						palette, workDir);
				consensusTable.init();

				if (consensusModel.getRowCount() > 1)
					consensusTable.addTotals();

				addWidget(consensusTable.getTable());
				horizontalLayout.addWidget(consensusTable.getChartContainer());
				consensusTable.getChartContainer().addStyleClass("flex-elem-auto");
			}
		}

		if (model.getState() == State.FinishedAll) {
			if (!showSingleResult && 
					!(isBlastTool && model.getConsensusBuckets().size() == 0))
				addWidget(new DownloadsWidget(null, workDir, organismDefinition, true, Mode.Ngs));
			addWidget(subTypingHeaderT);
		}
	}

	private WAnchor createPanViralToolAnchor() {
		WAnchor a = new WAnchor(new WLink("TODO")); // TODO
		a.setText("Some contigs could not be identified by the current tool, try the Pan-viral tool.");
		a.setMargin(20, Side.Bottom);
		return a;
	}

	public void showSubTypingHeader() {
		subTypingHeaderT.show();
	}

	private void fillStateTemplate(Template template, 
			State state, NgsResultsModel model){

		Long startTime = null, endTime = null;
		Integer startReads = null, endReads = null;

		WLink removedReads = null;
		
		switch (state) {
		case Init:
		case QC:
		case Preprocessing:
		case QC2: {
			startTime = model.getStateStartTime(State.Init);
			endTime = model.getStateStartTime(State.Diamond);
			startReads = model.getReadCountStartState(State.Init);
			endReads = model.getReadCountStartState(State.Diamond);
			FastqFilePair[] fastqFiles;
			if (model.isPairEnd())
				fastqFiles = new FastqFilePair[]{
					new FastqFilePair(NgsFileSystem.fastqPE1(workDir), NgsFileSystem.preprocessedPE1(workDir)),
					new FastqFilePair(NgsFileSystem.fastqPE2(workDir), NgsFileSystem.preprocessedPE2(workDir))};
			else
				fastqFiles = new FastqFilePair[]{
					new FastqFilePair(NgsFileSystem.fastqSE(workDir), NgsFileSystem.preprocessedSE(workDir))};

			removedReads = removedByPreprocessingLink(fastqFiles);
			break;
		} case Diamond: {
			startTime = model.getStateStartTime(State.Diamond); 
			endTime = model.getStateStartTime(State.Spades);
			startReads = model.getReadCountStartState(State.Diamond);
			endReads = model.getReadCountStartState(State.Spades);
			File[] fastqFiles;
			if (model.isPairEnd())
				fastqFiles = new File[]{
					NgsFileSystem.fastqPE1(workDir), NgsFileSystem.fastqPE2(workDir)};
			else
				fastqFiles = new File[]{NgsFileSystem.fastqSE(workDir)};
			removedReads = removedByFilteringLink(fastqFiles);
			break;
		} case Spades:
		case FinishedAll:
			startTime = model.getStateStartTime(State.Spades);
			endTime = model.getStateStartTime(State.FinishedAll);
			startReads = model.getReadCountStartState(State.Spades);
			endReads = model.getReadCountStartState(State.FinishedAll);
			break;
		}

		template.bindString("time", printTime(startTime, endTime));

		if (endReads != null) {
			template.bindString("start-reads", startReads + "");
			template.bindWidget("end-reads", new WAnchor(
					removedReads, (startReads - endReads) + ""));
		} else {
			template.bindEmpty("start-reads");
			template.bindEmpty("end-reads");
		}
	}

	private String printTime(Long startTime, Long endTime) {
		if (startTime != null && endTime != null) {
			return  Utils.formatTime(endTime - startTime);
		} else {
			return "--";
		}
	}

	/**
	 * Write all reads that are in originalFastq and are not in preprocessed to zos.
	 * @param originalFastq
	 * @param preprocessed
	 * @param out
	 * @throws IOException
	 * @throws FileFormatException
	 */
	private static void writeRemovedFastq(File originalFastq, File preprocessed,
			OutputStream out) throws IOException, FileFormatException {
		writeRemovedFastq(originalFastq, SequenceAlignment.getReadNames(preprocessed), out);
	}

	private static void writeRemovedFastq(File originalFastq, Set<String> usedReadNames,
			OutputStream out) throws IOException, FileFormatException {
		FileReader fr = new FileReader(originalFastq.getAbsolutePath());
		LineNumberReader lnr = new LineNumberReader(fr);

		while (true){
			Sequence s = SequenceAlignment.readFastqFileSequence(lnr, SequenceAlignment.SEQUENCE_DNA);
			if (s == null )
				break;
			if (!usedReadNames.contains(s.getName())) {
				out.write(("@" + s.getName() + "\n").getBytes());
				out.write((s.getSequence() + "\n").getBytes());
				out.write(("+" + "\n").getBytes());
				out.write((s.getQuality() + "\n").getBytes());
			}
		}
	}

	private WLink removedByPreprocessingLink(final FastqFilePair[] filePairs) {
		WResource r = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response)
					throws IOException {
				ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());

				try {
					for (FastqFilePair fp: filePairs) {
						zos.putNextEntry(new ZipEntry(fp.original.getName()));
						writeRemovedFastq(fp.original, fp.preprocessed, zos);
					}
				} catch (FileFormatException e) {
					e.printStackTrace();
					response.setStatus(404);
					return;
				} finally {
					zos.close();
				}
			}
		};
		r.suggestFileName("removed-reads-preprocessing.zip");
		WLink link = new WLink(r);
		link.setTarget(AnchorTarget.TargetDownload);
		return link;
	}

	/**
	 * @return the names of all read that passed filtering.
	 * @throws FileFormatException 
	 * @throws IOException 
	 */
	private Set<String> readThatPassedFiltring() throws IOException, FileFormatException {
		Set<String> readNames = new HashSet<String>();

		File diamondResutlsDir = NgsFileSystem.diamondResutlsDir(workDir);
		for (File d: diamondResutlsDir.listFiles()) {
			if (d.isDirectory())
				for (File f: d.listFiles())
					if (f.getName().endsWith(".fastq"))
						readNames.addAll(SequenceAlignment.getReadNames(f));
		}
		return readNames;
	}

	private WLink removedByFilteringLink(final File[] fastqFiles) {
		WResource r = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response)
					throws IOException {

				try {
					Set<String> readNames = readThatPassedFiltring();

					ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());

					try {
						for (File fastq: fastqFiles) {
							zos.putNextEntry(new ZipEntry(fastq.getName()));
							writeRemovedFastq(fastq, readNames, zos);
						}
					} catch (FileFormatException e) {
						e.printStackTrace();
						response.setStatus(404);
						return;
					} finally {
						zos.close();
					}
				} catch (FileFormatException e) {
					e.printStackTrace();
					response.setStatus(404);
					return;
				}
				response.getOutputStream();
			}
		};
		r.suggestFileName("removed-reads-filtering.zip");
		WLink link = new WLink(r);
		link.setTarget(AnchorTarget.TargetDownload);
		return link;
	}

	private WAnchor qcAnchor(File inFile, String anchorName) {
		if (inFile == null || !inFile.exists())
			return null;

		WFileResource r = new WFileResource("html", inFile.getAbsolutePath());
		WLink link = new WLink(r);
		link.setTarget(AnchorTarget.TargetNewWindow);
		WAnchor anchor = new WAnchor(link, anchorName);
		anchor.setInline(true);
		return anchor;
	}

	public static WAnchor details(ConsensusBucket bucket, File workDir) {
		WAnchor details = new WAnchor();
		details.setText("Details");
		details.setLink(detailsLink(bucket, workDir));
		return details;
	}

	public static WLink detailsLink(ConsensusBucket bucket, File workDir) {
		 WLink details = new WLink(WLink.Type.InternalPath, 
				 detailsUrl(bucket, workDir));
		 details.setTarget(AnchorTarget.TargetNewWindow);
		 return details;
	}

	public static String detailsUrl(ConsensusBucket bucket, File workDir) {
		String jobId = AbstractJobOverview.jobId(workDir);
		return "/job/" + jobId + "/" + JobForm.BUCKET_PATH + "/"
				+ bucket.getBucketId();
	}

	public static class ResultsView extends ChartTableWidget {
		private File workDir;
		public ResultsView(NgsConsensusSequenceModel model, int chartDataColumn,
				int colorColumn, WChartPalette chartPalette, File workDir) {
			super(model, chartDataColumn, colorColumn, chartPalette, "Approximate Reads Count");
			this.workDir = workDir;
		}

		private NgsConsensusSequenceModel model() {
			return (NgsConsensusSequenceModel) model;
		}
		@Override
		protected void addWidget(final int row, final int column) {
			switch (column) {
			case NgsConsensusSequenceModel.DEEP_COV_COLUMN:
			case NgsConsensusSequenceModel.READ_COUNT_COLUMN:
				addText(row + 1, column, Utils.toApproximateString(
						(Number)model.getData(row,column)));
				break;
			case NgsConsensusSequenceModel.IMAGE_COLUMN:
				CovMap covMap = new CovMap(
						model().getBucket(row), model());
				WAnchor a = new WAnchor(detailsLink(
						model().getBucket(row), workDir));
				a.addWidget(covMap);
				getTable().getElementAt(row + 1, tableCol(column)).addWidget(a);
				break;
			case NgsConsensusSequenceModel.DETAILS_COLUMN:
				getTable().getElementAt(row + 1, tableCol(column)).addWidget(
						details(model().getBucket(row), workDir));
				break;

			default:
				super.addWidget(row, column);
				break;
			}
		}
		public void addTotals() {
			addText(getTable().getRowCount(), 0, "Totals");
			addTotals(NgsConsensusSequenceModel.SEQUENCE_COUNT_COLUMN, false);
			addTotals(NgsConsensusSequenceModel.READ_COUNT_COLUMN, true);
		}

		public void addTotals(int c, boolean approx) {
			int row = getTable().getRowCount() - 1;

			double total = 0.0;
			for (int r = 0; r < model.getRowCount(); ++r) {
				Object data = model.getData(r, c);
				if (data != null && data instanceof Double)
					total += (Double)data;
				else if(data != null && data instanceof Integer)
					total += (Integer)data;
			}
			if (approx)
				addText(row, c, Utils.toApproximateString(total));
			else
				addText(row, c, total);
		}
	}

	public static class SingleResultView extends Template {
		public SingleResultView(final NgsConsensusSequenceModel model, final File workDir) {
			super(tr("single-ngs-result-view"));

			bindString("read-count",
					Utils.toApproximateString((Number)model.getData(
							0, NgsConsensusSequenceModel.READ_COUNT_COLUMN)));

			Double len = (Double) model.getData(0, NgsConsensusSequenceModel.TOTAL_LENGTH_COLUMN);
			bindString("cov-len", len.intValue() + "");

			bindString("depth",
					Utils.toApproximateString((Number)model.getData(
							0, NgsConsensusSequenceModel.DEEP_COV_COLUMN)));
			
			bindWidget("img", new CovMap(model.getBucket(0), model));
			bindWidget("details", details(model.getBucket(0), workDir));
			bindWidget("xml", DownloadsWidget.createXmlDownload(Mode.Ngs, workDir));
			bindWidget("consensus", DownloadsWidget.createFastaDownload(NgsFileSystem.CONSENSUSES_FILE,
					tr("monitorForm.consensuses"), Mode.Ngs, workDir, null));
		}
	}
}
