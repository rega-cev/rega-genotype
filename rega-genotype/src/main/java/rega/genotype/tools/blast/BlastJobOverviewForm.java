package rega.genotype.tools.blast;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.NgsSequence.BucketData;
import rega.genotype.NgsSequence.Contig;
import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.JobOverviewSummary;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.StandardTableView;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.PositionScheme;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.ViewItemRenderFlag;
import eu.webtoolkit.jwt.WAbstractItemDelegate;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WPainter;
import eu.webtoolkit.jwt.WRectF;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.chart.LabelOption;
import eu.webtoolkit.jwt.chart.WPieChart;

/**
 * Job overview window for the blast tool.
 * The result of the blast tool analysis is a pie chart that contains a series 
 * per input sequence. Clicking the series will redirect to the typing tool of the selected sequence.
 * 
 * @author michael
 */
public class BlastJobOverviewForm extends AbstractJobOverview {
	public static final String BLAST_JOB_ID_PATH = "blast-job";

	private int ASSINGMENT_COLUMN =    0;
	private int SEQUENCE_COUNT_COLUMN =1; // sequence count column. percentages of the chart.
	private int CHART_DISPLAY_COLUMN = 2;
	private int PERCENTAGE_COLUMN =    3; // deep cov for ngs
	private int SRC_COLUMN =           4;
	private int COLOR_COLUMN =         5;

	//private Template layout = new Template(tr("job-overview-form"), this);
	private WStandardItemModel blastResultModel = new WStandardItemModel();
	// <concludedId (cluster id), cluster data>
	private Signal1<String> jobIdChanged = new Signal1<String>();
	private String jobId;
	protected WContainerWidget chartContainer = new WContainerWidget(); // used as a layer to draw the anchors on top of the chart.
	protected WPieChart chart;
	protected StandardTableView table = new StandardTableView();

	protected BlastResultParser blastResultParser;

	private WContainerWidget resultsContainer  = new WContainerWidget();


	public BlastJobOverviewForm(GenotypeWindow main) {
		super(main);
	}

	private void createChart() {
		if (chart != null)
			chart.remove();
		chartContainer.clear();

		chart = new WPieChart() {
			@Override
			protected void drawLabel(WPainter painter, WRectF rect,
					EnumSet<AlignmentFlag> alignmentFlags, CharSequence text,
					int row) {
				if (getModel().link(row, getDataColumn()) == null)
					super.drawLabel(painter, rect, alignmentFlags, text, row);
				else{
					WAnchor a = new WAnchor(getModel().link(row, getDataColumn()), text);
					chartContainer.addWidget(
							createLabelWidget(a, painter, rect, alignmentFlags));
				}
			}
		};
		chartContainer.addWidget(chart);
		chartContainer.setPositionScheme(PositionScheme.Relative);

		chartContainer.resize(800, 300);
		chartContainer.setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		chartContainer.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

		chart.resize(800, 300);
		chart.setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		chart.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		chart.setStartAngle(90);

		chart.setModel(blastResultModel);
		//chart.setLabelsColumn(CHART_DISPLAY_COLUMN);
		chart.setDataColumn(PERCENTAGE_COLUMN);
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);
	}

	@Override
	public void init(String jobId, String filter) {
		super.init(jobId, filter);

		// table
		table.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		table.setSortingEnabled(false);
		table.setWidth(new WLength(500));
		table.setStyleClass("blastResultsTable");
		table.setHeaderHeight(new WLength(20));
		table.hideColumn(CHART_DISPLAY_COLUMN);
		table.setColumnWidth(ASSINGMENT_COLUMN, new WLength(540));
		table.setColumnWidth(SEQUENCE_COUNT_COLUMN, new WLength(80));
		table.setColumnWidth(PERCENTAGE_COLUMN, new WLength(80));
		table.setColumnWidth(SRC_COLUMN, new WLength(60));
		table.setColumnWidth(COLOR_COLUMN, new WLength(60));
		//table.setRowHeight(new WLength(50));

		table.setItemDelegateForColumn(COLOR_COLUMN, new WAbstractItemDelegate() {
			@Override
			public WWidget update(WWidget widget, WModelIndex index, EnumSet<ViewItemRenderFlag> flags) {
				WContainerWidget w = new WContainerWidget();
				w.setStyleClass("legend-item");
				WColor c = (WColor)index.getData(ItemDataRole.UserRole + 1);
				if (c != null)
					w.getDecorationStyle().setBackgroundColor(c);

				w.setMargin(WLength.Auto, Side.Left, Side.Right);

				return w;
			}
		});
	}
	
	@Override
	public void handleInternalPath(String internalPath) {
		createChart();
		blastResultParser = null;

		table.hide();
		chartContainer.hide();

		String path[] =  internalPath.split("/");
		if (path.length > 1) {
			jobId = path[1];

			if (!existsJob(jobId)) {
				showBadJobIdError();
				return;
			}

			template.show();
			init(jobId, "");

			jobIdChanged.trigger(jobId);
		} else {
			jobIdChanged.trigger("");
			showBadJobIdError();
		}
	}

	private void showBadJobIdError() {
		setMargin(30);
		addWidget(new WText(tr("monitorForm.nonExistingJobId").arg(jobId)));
		template.hide();
	}

	@Override
	protected GenotypeResultParser createParser() {
		blastResultParser = new BlastResultParser(this);
		return blastResultParser;
	}

	protected double totalSequences() {
		Map<String, ClusterData> clusterDataMap = blastResultParser.clusterDataMap;
		double total = 0;
		for (Map.Entry<String, ClusterData> e: clusterDataMap.entrySet()){
			ClusterData toolData = e.getValue();
			total += toolData.sequencesData.size();
		}

		return total;
	}
	private WStandardItemModel createBlastModel() {
		WStandardItemModel blastModel = new WStandardItemModel();

		Map<String, ClusterData> clusterDataMap = blastResultParser.clusterDataMap;

		// create blastResultModel
		blastModel = new WStandardItemModel();
		blastModel.insertColumns(blastModel.getColumnCount(), 6);

		blastModel.setHeaderData(ASSINGMENT_COLUMN, tr("detailsForm.summary.assignment"));
		blastModel.setHeaderData(SEQUENCE_COUNT_COLUMN, tr("detailsForm.summary.numberSeqs"));
		blastModel.setHeaderData(PERCENTAGE_COLUMN, tr("detailsForm.summary.percentage"));
		blastModel.setHeaderData(SRC_COLUMN, tr("detailsForm.summary.src"));
		blastModel.setHeaderData(COLOR_COLUMN, tr("detailsForm.summary.legend"));

		// find total 
		double total = totalSequences();
		Config config = Settings.getInstance().getConfig();
		int i = 0;
		for (Map.Entry<String, ClusterData> e: clusterDataMap.entrySet()){
			ClusterData toolData = e.getValue();
			int row = blastModel.getRowCount();
			blastModel.insertRows(row, 1);
			blastModel.setData(row, ASSINGMENT_COLUMN, toolData.concludedName);
			String toolId = config.getToolId(toolData.taxonomyId);
			ToolConfig toolConfig = toolId == null ? null : config.getCurrentVersion(toolId);
			if (toolConfig != null) {
				ToolConfig blastToolConfig = getMain().getOrganismDefinition().getToolConfig();
				blastModel.setData(row, ASSINGMENT_COLUMN, createToolLink(
						toolData.taxonomyId, jobId, blastToolConfig), ItemDataRole.LinkRole);
				blastModel.setData(row, SEQUENCE_COUNT_COLUMN, createToolLink(
						toolData.taxonomyId, jobId, blastToolConfig), ItemDataRole.LinkRole);
			}
			blastModel.setData(row, SEQUENCE_COUNT_COLUMN, toolData.sequencesData.size()); // percentage
			blastModel.setData(row, CHART_DISPLAY_COLUMN, 
					toolData.concludedName + " (" + toolData.sequencesData.size() + ")");

			WColor color = chart.getPalette().getBrush(i).getColor();
			blastModel.setData(row, COLOR_COLUMN, color, 
					ItemDataRole.UserRole + 1);

			blastModel.setData(row, PERCENTAGE_COLUMN, 
					(double)toolData.sequencesData.size() / total * 100.0);

			blastModel.setData(row, SRC_COLUMN, toolData.src);

			i++;
		}
		return blastModel;
	}

	@Override
	public void fillResultsWidget() {
		createChart();

		if (blastResultParser == null || blastResultParser.clusterDataMap.isEmpty())
			return;

		blastResultModel = createBlastModel();

		chart.setModel(blastResultModel);

		// copy model to table model

		WStandardItemModel tableModel = createBlastModel();

		// Add totals only to table model.
		int row = tableModel.getRowCount();
		tableModel.insertRows(row, 1);
		tableModel.setData(row, ASSINGMENT_COLUMN, "Totals");
		tableModel.setData(row, SEQUENCE_COUNT_COLUMN, totalSequences()); // percentage

		table.setModel(tableModel);
		table.setTableWidth(true);

		chartContainer.show();
		table.show();
		resultsContainer.addWidget(chartContainer);
		resultsContainer.addWidget(table);
		resultsContainer.setWidth(table.getWidth());
		resultsContainer.setMargin(WLength.Auto, Side.Left, Side.Right);
		bindResults(resultsContainer);
	}

	public Signal1<String> jobIdChanged() {
		return jobIdChanged;
	}

	public static WLink createToolLink(final String taxonomyId, final String jobId, ToolConfig blastToolConfig) {
		Config config = Settings.getInstance().getConfig();
		ToolConfig toolConfig = config.getCurrentVersion(config.getToolId(taxonomyId));
		if (toolConfig == null)
			return null;

		String url = GenotypeMain.getApp().getServletContext().getContextPath()
		+ "/typingtool/" + toolConfig.getPath() + "/"
		+ BLAST_JOB_ID_PATH + "/" + blastToolConfig.getId()
		+ "/" + blastToolConfig.getVersion() + "/" + jobId;

		WLink link = new WLink(url);
		link.setTarget(AnchorTarget.TargetNewWindow);

		return link;
	}
	
	// Classes
	public static class ClusterData {
		public String taxonomyId = new String();
		public String concludedName = new String();
		public String concludedId = new String();
		public String src = new String();

		public BucketData bucketData = null;

		public List<SequenceData> sequencesData = new ArrayList<SequenceData>();
	}

	public static class SequenceData {
		public String name = new String();
		public String description = new String();
		public Integer length;
		public List<Contig> contigs = new ArrayList<Contig>();
	}

	// unused 
	@Override
	public List<Header> getHeaders() {
		return new ArrayList<Header>();
	}

	// unused 
	@Override
	public List<WWidget> getData(GenotypeResultParser p) {
		return null;
	}

	// unused 
	@Override
	public JobOverviewSummary getSummary(String filter) {
		return  null;
	}

	/**
	 * Parse result.xml file from job dir and fill the output to
	 * blastResultModel.
	 */
	public static class BlastResultParser extends GenotypeResultParser {
		public Map<String, ClusterData> clusterDataMap = new HashMap<String, ClusterData>();
		private WApplication app;
		private BlastJobOverviewForm form;

		public BlastResultParser(BlastJobOverviewForm form) {
			this.form = form;
			this.app = WApplication.getInstance();
			setReaderBlocksOnEof(true);
		}

		@Override
		public void updateUi() {
			UpdateLock updateLock = app.getUpdateLock();
			form.updateView();
			app.triggerUpdate();
			updateLock.release();
		}

		@Override
		public void endSequence() {
			String taxonomyId = GenotypeLib
					.getEscapedValue(this,
							"/genotype_result/sequence/result[@id='blast']/cluster/taxonomy-id");
			String seqName = GenotypeLib.getEscapedValue(this,
					"/genotype_result/sequence/@name");
			String seqDesc = GenotypeLib.getEscapedValue(this,
					"/genotype_result/sequence/@description");
			String seqLength = GenotypeLib.getEscapedValue(this,
					"/genotype_result/sequence/@length");
			String clusterSrc = GenotypeLib.getEscapedValue(this,
					"/genotype_result/sequence/result[@id='blast']/cluster/src");
			String concludedId = GenotypeLib
					.getEscapedValue(this,
							"/genotype_result/sequence/result[@id='blast']/cluster/concluded-id");
			String concludedName = GenotypeLib
					.getEscapedValue(this,
							"/genotype_result/sequence/result[@id='blast']/cluster/concluded-name");

			if (concludedName == null)
				concludedName = "Unassigned";

			String key= concludedId;

			ClusterData toolData = clusterDataMap.containsKey(key) ? clusterDataMap.get(key) : new ClusterData();

			if (concludedId != null && !concludedId.equals("Unassigned"))
				toolData.taxonomyId = taxonomyId;

			SequenceData sequenceData = new SequenceData();
			sequenceData.name = seqName;
			sequenceData.description = seqDesc;
			try {
				sequenceData.length = Integer.parseInt(seqLength);
			} catch (NumberFormatException e){
				sequenceData.length = null;
			}
			
			if (concludedId != null && !concludedId.equals("Unassigned"))
				toolData.concludedName = concludedName;
			toolData.sequencesData.add(sequenceData);
			if (toolData.concludedId == null || toolData.concludedId.equals("Unassigned"))
				toolData.concludedId = concludedId;

			toolData.src = clusterSrc;

			clusterDataMap.put(key, toolData);
		}
	}
}
