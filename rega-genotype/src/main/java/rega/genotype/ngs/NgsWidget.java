package rega.genotype.ngs;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.FilenameUtils;

import rega.genotype.Constants.Mode;
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
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Utils;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WTableRow;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.chart.WChartPalette;
import eu.webtoolkit.jwt.chart.WStandardPalette;

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

		new WText("<h2> NGS Analysis Results </h2>", this);

		WContainerWidget horizontalLayout = new WContainerWidget(this);
		horizontalLayout.addStyleClass("flex-container");

		WTable table = new WTable(horizontalLayout);
		table.addStyleClass("ngs-detils-table flex-elem");
		
		table.getElementAt(0, 1).addWidget(new WText(
				"NGS Analysis of " + model.getInputName()));

		WTableRow preprocessingWidget = stateWidget(table,
				"Preprocessing", model.getStateStartTime(State.Init), 
				model.getStateStartTime(State.Diamond),
				model.getReadCountStartState(State.Init),
				model.getReadCountStartState(State.Diamond));

		WTableRow filteringWidget = stateWidget(table,
				State.Diamond.text, model.getStateStartTime(State.Diamond), 
				model.getStateStartTime(State.Spades),
				model.getReadCountStartState(State.Diamond),
				model.getReadCountStartState(State.Spades));

		WTableRow identificationWidget = stateWidget(table,
				State.Spades.text, model.getStateStartTime(State.Spades), 
				model.getStateStartTime(State.FinishedAll),
				model.getReadCountStartState(State.Spades),
				model.getReadCountStartState(State.FinishedAll));

		if (!model.getErrors().isEmpty() )
			new WText("<div class=\"error\">Error: " + model.getErrors() + "</div>", 
					preprocessingWidget.elementAt(1));

		Template preprocessing = new Template(tr("ngs.qc-view"), preprocessingWidget.elementAt(1));
		preprocessing.bindEmpty("qc-1");
		preprocessing.bindEmpty("qc-2");
		preprocessing.bindEmpty("qcp-1");
		preprocessing.bindEmpty("qcp-2");
		preprocessing.setCondition("if-preprocessing", !model.getSkipPreprocessing());
		if (model.getState().code >= State.Preprocessing.code) {
			File qcDir = new File(workDir, NgsFileSystem.QC_REPORT_DIR);
			if (model.isPairEnd()) {
				preprocessing.bindWidget("qc-1", qcAnchor(qcDir, model.getFastqPE1FileName()));
				preprocessing.bindWidget("qc-2", qcAnchor(qcDir, model.getFastqPE2FileName()));
			} else {
				preprocessing.bindWidget("qc-1", qcAnchor(qcDir, model.getFastqSEFileName()));
			}
		}

		if (model.getState().code >= State.Diamond.code) {
			File qcDir = new File(workDir, NgsFileSystem.QC_REPORT_AFTER_PREPROCESS_DIR);
			if (model.isPairEnd()) {
				preprocessing.bindWidget("qcp-1", qcAnchor(qcDir, model.getFastqPE1FileName()));
				preprocessing.bindWidget("qcp-2", qcAnchor(qcDir, model.getFastqPE2FileName()));
			} else {
				preprocessing.bindWidget("qcp-1", qcAnchor(qcDir, model.getFastqSEFileName()));
			}
		}

		if (model.getState().code == State.Diamond.code) {
			String jobState = LongJobsScheduler.getInstance().getJobState(workDir);
			new WText("<div> Diamond blast job state:" + jobState + "</div>", filteringWidget.elementAt(1));
		}

		if (model.getState().code == State.Spades.code) {
			String jobState = LongJobsScheduler.getInstance().getJobState(workDir);
			new WText("<div> Sapdes job state:" + jobState + "</div>", this);
		}

		// style

		if (model.getState().code < State.Diamond.code){
			preprocessingWidget.addStyleClass("working");
			filteringWidget.addStyleClass("waiting");
			identificationWidget.addStyleClass("waiting");
		} else if (model.getState() == State.Diamond){
			filteringWidget.addStyleClass("working");
			identificationWidget.addStyleClass("waiting");
		} else if (model.getState() == State.Spades){
			identificationWidget.addStyleClass("working");
		}

		boolean showSingleResult = !organismDefinition.getToolConfig().getToolMenifest().isBlastTool()
				&& model.getConsensusBuckets().size() == 1;
		// consensus table.
		if (!model.getConsensusBuckets().isEmpty()) {
			WChartPalette palette = new WStandardPalette(WStandardPalette.Flavour.Muted);
			final NgsConsensusSequenceModel consensusModel = new NgsConsensusSequenceModel(
					model.getConsensusBuckets(), model.getReadLength(), palette,
					organismDefinition.getToolConfig(), workDir);
			if (showSingleResult) { 
				SingleResultView view = new SingleResultView(consensusModel, workDir);
				table.getElementAt(table.getRowCount(), 1).addWidget(view);
			} else {
				consensusTable = new ResultsView(consensusModel, 
						NgsConsensusSequenceModel.READ_COUNT_COLUMN,
						NgsConsensusSequenceModel.COLOR_COLUMN,
						palette, workDir);
				consensusTable.init();

				if (consensusModel.getRowCount() > 1)
					consensusTable.addTotalsRow(new int[]{
							NgsConsensusSequenceModel.SEQUENCE_COUNT_COLUMN,
							NgsConsensusSequenceModel.READ_COUNT_COLUMN});

				addWidget(consensusTable);
				horizontalLayout.addWidget(consensusTable.getChartContainer());
				consensusTable.getChartContainer().addStyleClass("flex-elem");
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

	private WTableRow stateWidget(WTable table, String title, 
			Long startTime, Long endTime,
			Integer startReads, Integer endReads){
		WTableRow stateRow = table.insertRow(table.getRowCount());
		new WText("<p><b><i>" + title + "</i></b></p>", 
				stateRow.elementAt(1));
		stateRow.elementAt(0).setWidth(new WLength(70));
		new WText("<p>" + printTime(startTime, endTime) + "</p>", stateRow.elementAt(0));
		if (endReads != null) {
			new WText("Started with " + startReads + " reads, ",
					stateRow.elementAt(1));
			WAnchor removedReads = new WAnchor(new WLink("TODO"),
					(startReads - endReads) + " reads were removed.");
			removedReads.setInline(true);
			stateRow.elementAt(1).addWidget(removedReads);
		}

		//stateWidget.setMargin(10, Side.Bottom);

		return stateRow;
	}

	private String printTime(Long startTime, Long endTime) {
		if (startTime != null && endTime != null) {
			return  Utils.formatTime(endTime - startTime);
		} else {
			return "";
		}
	}

	private WAnchor qcAnchor(File qcDir, String inFileName) {
		if (qcDir.listFiles() == null)
			return null;
		File[] files = qcDir.listFiles();
		Arrays.sort(files, new Comparator<File>() {
			public int compare(File o1, File o2) {
				return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
			}
		});

		for (File f: files) {
			if (FilenameUtils.getExtension(f.getAbsolutePath()).equals("html")){
				WFileResource r = new WFileResource("html", f.getAbsolutePath());
				WLink link = new WLink(r);
				link.setTarget(AnchorTarget.TargetNewWindow);
				String readsName = FileUtil.removeExtention(inFileName);
				if (f.getName().contains(readsName)) {
					WAnchor anchor = new WAnchor(link, readsName);
					anchor.setInline(true);
					anchor.setMargin(8);
					return anchor;
				}
			}
		}
		return null;
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

		public void addTotalsRow(int[] columns) {
			int row = table.getRowCount();
			addText(row, 0, "Totals");
			for (int c: columns) {
				double total = 0.0;
				for (int r = 0; r < model.getRowCount(); ++r) {
					Object data = model.getData(r, c);
					if (data != null && data instanceof Double)
						total += (Double)data;
					else if(data != null && data instanceof Integer)
						total += (Integer)data;
				}
				addText(row, c, Utils.toApproximateString(total));
			}
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