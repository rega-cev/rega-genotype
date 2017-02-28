package rega.genotype.ngs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

	private ResultsView consensusTable;
	private File workDir;
	private WText subTypingHeaderT = new WText("<h2>Sub-Typing Tool Results</h2>");

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
		if (model.getState().code >= State.Preprocessing.code) {
			preprocessing.setCondition("if-finished", true);
			File qcDir = new File(workDir, NgsFileSystem.QC_REPORT_DIR);
			if (model.isPairEnd()) {
				// qc
				preprocessing.bindWidget("qc-1", qcAnchor(NgsFileSystem.qcPE1File(qcDir), "PE 1"));
				preprocessing.bindWidget("qc-2", qcAnchor(NgsFileSystem.qcPE2File(qcDir), "PE 2"));
				// removed reads 
				preprocessing.bindWidget("removed-pe1", 
						removedByPreprocessingAnchor(
								NgsFileSystem.fastqPE1(workDir), 
								NgsFileSystem.preprocessedPE1(workDir), "PE 1"));

				preprocessing.bindWidget("removed-pe2", removedByPreprocessingAnchor(
						NgsFileSystem.fastqPE2(workDir), 
						NgsFileSystem.preprocessedPE2(workDir), "PE 2"));
			} else {
				// qc
				preprocessing.bindWidget("qc-1", qcAnchor(NgsFileSystem.qcSEFile(qcDir), "SE"));
				// removed reads 
				preprocessing.bindWidget("removed-pe1", removedByPreprocessingAnchor(
						NgsFileSystem.fastqSE(workDir), 
						NgsFileSystem.preprocessedSE(workDir), "SE"));
			}
		}

		if (model.getState().code >= State.Diamond.code) {
			File qcDir = new File(workDir, NgsFileSystem.QC_REPORT_AFTER_PREPROCESS_DIR);
			if (model.isPairEnd()) {
				preprocessing.bindWidget("qcp-1", qcAnchor(NgsFileSystem.qcPE1File(qcDir), "PE 1"));
				preprocessing.bindWidget("qcp-2", qcAnchor(NgsFileSystem.qcPE2File(qcDir), "PE 2"));
			} else {
				preprocessing.bindWidget("qcp-1", qcAnchor(NgsFileSystem.qcSEFile(qcDir), "SE"));
			}
		}

		if (model.getState().code == State.Diamond.code) {
			String jobState = LongJobsScheduler.getInstance().getJobState(workDir);
			new WText("<p> Diamond blast job state:" + jobState + "</p>", this);
		}
		if (model.getState().code == State.Spades.code) {
			String jobState = LongJobsScheduler.getInstance().getJobState(workDir);
			new WText("<p> Sapdes job state:" + jobState + "</p>", this);
		}

		if (model.getState().code >= State.Spades.code) {
			filtering.setCondition("if-finished", true);
			if (model.isPairEnd()) {
				// removed reads 
				filtering.bindWidget("removed-pe1", 
						removedByFiltringAnchor(
								NgsFileSystem.fastqPE1(workDir), "PE 1"));

				filtering.bindWidget("removed-pe2", removedByFiltringAnchor(
						NgsFileSystem.fastqPE2(workDir), "PE 2"));
			} else {
				// removed reads 
				filtering.bindWidget("removed-pe1", removedByFiltringAnchor(
						NgsFileSystem.fastqSE(workDir), "SE"));
			}
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

		List<ConsensusBucket> consensusBuckets = new ArrayList<ConsensusBucket>();
		if (isBlastTool) {
			consensusBuckets.addAll(model.getConsensusBuckets());
		} else {
			for (ConsensusBucket b: model.getConsensusBuckets())
				if (!b.getConcludedId().equals("Unassigned"))
					consensusBuckets.add(b);
		}

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

				addWidget(consensusTable);
				horizontalLayout.addWidget(consensusTable.getChartContainer());
				consensusTable.getChartContainer().addStyleClass("flex-elem-auto");
			}
		}

		if (model.getState() == State.FinishedAll) {
			if (!showSingleResult)
				addWidget(new DownloadsWidget(null, workDir, organismDefinition, true, Mode.Ngs));
			addWidget(subTypingHeaderT);
		}
	}

	public void showSubTypingHeader() {
		subTypingHeaderT.show();
	}

	private void fillStateTemplate(Template template, 
			State state, NgsResultsModel model){

		Long startTime = null, endTime = null;
		Integer startReads = null, endReads = null;

		switch (state) {
		case Init:
		case QC:
		case Preprocessing:
		case QC2:
			startTime = model.getStateStartTime(State.Init);
			endTime = model.getStateStartTime(State.Diamond);
			startReads = model.getReadCountStartState(State.Init);
			endReads = model.getReadCountStartState(State.Diamond);
			break;
		case Diamond:
			startTime = model.getStateStartTime(State.Diamond); 
			endTime = model.getStateStartTime(State.Spades);
			startReads = model.getReadCountStartState(State.Diamond);
			endReads = model.getReadCountStartState(State.Spades);
			break;
		case Spades:
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
			template.bindString("end-reads", (startReads - endReads) + "");
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

	private WAnchor removedByPreprocessingAnchor(final File fastq, final File preprocessed,
			final String anchorName) {
		WAnchor removedReads = new WAnchor(
				removedByPreprocessingLink(fastq, preprocessed), anchorName);
		removedReads.setInline(false);
		return removedReads;
	}

	private WAnchor removedByFiltringAnchor(final File fastq,
			final String anchorName) {
		WAnchor removedReads = new WAnchor(
				removedByFilteringLink(fastq), anchorName);
		removedReads.setInline(false);
		return removedReads;
	}

	private WLink removedByPreprocessingLink(final File fastq, final File preprocessed) {
		WResource r = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response)
					throws IOException {

				try {
					Set<String> readNames = SequenceAlignment.getReadNames(preprocessed);

					FileReader fr = new FileReader(fastq.getAbsolutePath());
					LineNumberReader lnr = new LineNumberReader(fr);
					while (true){
						Sequence s = SequenceAlignment.readFastqFileSequence(lnr, SequenceAlignment.SEQUENCE_DNA);
						if (s == null )
							break;
						if (!readNames.contains(s.getName())) {
							response.getOutputStream().println("@" + s.getName());
							response.getOutputStream().println(s.getSequence());
							response.getOutputStream().println("+");
							response.getOutputStream().println(s.getQuality());
						}
					}
				} catch (FileFormatException e) {
					e.printStackTrace();
					response.setStatus(404);
					return;
				}
				response.getOutputStream();
			}
		};
		WLink link = new WLink(r);
		link.setTarget(AnchorTarget.TargetNewWindow);
		return link;
	}

	private WLink removedByFilteringLink(final File fastq) {
		WResource r = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response)
					throws IOException {

				try {
					Set<String> readNames = new HashSet<String>();

					File diamondResutlsDir = NgsFileSystem.diamondResutlsDir(workDir);
					for (File d: diamondResutlsDir.listFiles()) {
						if (d.isDirectory())
							for (File f: d.listFiles())
								if (f.getName().equals(fastq.getName()))
									readNames.addAll(SequenceAlignment.getReadNames(f));
					}

					FileReader fr = new FileReader(fastq.getAbsolutePath());
					LineNumberReader lnr = new LineNumberReader(fr);
					while (true){
						Sequence s = SequenceAlignment.readFastqFileSequence(lnr, SequenceAlignment.SEQUENCE_DNA);
						if (s == null )
							break;
						if (!readNames.contains(s.getName())) {
							response.getOutputStream().println("@" + s.getName());
							response.getOutputStream().println(s.getSequence());
							response.getOutputStream().println("+");
							response.getOutputStream().println(s.getQuality());
						}
					}
				} catch (FileFormatException e) {
					e.printStackTrace();
					response.setStatus(404);
					return;
				}
				response.getOutputStream();
			}
		};
		WLink link = new WLink(r);
		link.setTarget(AnchorTarget.TargetNewWindow);
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
		String jobId = AbstractJobOverview.jobId(workDir);
		details.setLink(new WLink(WLink.Type.InternalPath, 
				"/job/" + jobId + "/" + JobForm.BUCKET_PATH + "/"
				+ bucket.getBucketId()));
		details.setTarget(AnchorTarget.TargetNewWindow);
		return details;
	}

	public static class ResultsView extends ChartTableWidget {
		private File workDir;
		public ResultsView(NgsConsensusSequenceModel model, int chartDataColumn,
				int colorColumn, WChartPalette chartPalette, File workDir) {
			super(model, chartDataColumn, colorColumn, chartPalette);
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

				table.getElementAt(row + 1, tableCol(column)).addWidget(covMap);
				break;
			case NgsConsensusSequenceModel.DETAILS_COLUMN:
				table.getElementAt(row + 1, tableCol(column)).addWidget(
						details(model().getBucket(row), workDir));
				break;

			default:
				super.addWidget(row, column);
				break;
			}
		}
		public void addTotals() {
			addText(table.getRowCount(), 0, "Totals");
			addTotals(NgsConsensusSequenceModel.SEQUENCE_COUNT_COLUMN, false);
			addTotals(NgsConsensusSequenceModel.READ_COUNT_COLUMN, true);
		}

		public void addTotals(int c, boolean approx) {
			int row = table.getRowCount() - 1;

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