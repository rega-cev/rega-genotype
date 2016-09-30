package rega.genotype.tools.blast;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.JobOverviewSummary;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
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
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WPainter;
import eu.webtoolkit.jwt.WRectF;
import eu.webtoolkit.jwt.WStandardItem;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WTableView;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
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

	private static final int ASSINGMENT_COLUMN =    0;
	private static final int DATA_COLUMN =          1; // sequence count column. percentages of the chart.
	private static final int CHART_DISPLAY_COLUMN = 2;
	private static final int PERCENTAGE_COLUMN =    3;
	private static final int COLOR_COLUMN =         4;


	//private Template layout = new Template(tr("job-overview-form"), this);
	private WStandardItemModel blastResultModel = new WStandardItemModel();
	// <concludedId (cluster id), cluster data>
	private Signal1<String> jobIdChanged = new Signal1<String>();
	private String jobId;
	private WContainerWidget chartContainer = new WContainerWidget(); // used as a layer to draw the anchors on top of the chart.
	private WPieChart chart;
	private WTableView table = new WTableView();

	private BlastResultParser blastResultParser;

	public BlastJobOverviewForm(GenotypeWindow main) {
		super(main);

		// table
		table.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		table.setWidth(new WLength(500));
		table.setStyleClass("blastResultsTable");
		table.setHeaderHeight(new WLength(20));
		table.hideColumn(CHART_DISPLAY_COLUMN);
		table.setColumnWidth(ASSINGMENT_COLUMN, new WLength(340));
		table.setColumnWidth(DATA_COLUMN, new WLength(80));
		table.setColumnWidth(PERCENTAGE_COLUMN, new WLength(60));
		table.setColumnWidth(COLOR_COLUMN, new WLength(60));

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
		chart.setDataColumn(DATA_COLUMN);
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);
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
		blastResultParser = new BlastResultParser();
		return blastResultParser;
	}

	@Override
	public void fillResultsWidget() {
		createChart();

		if (blastResultParser == null || blastResultParser.clusterDataMap.isEmpty())
			return;

		Map<String, ClusterData> clusterDataMap = blastResultParser.clusterDataMap;
		
		// create blastResultModel
		blastResultModel = new WStandardItemModel();
		blastResultModel.insertColumns(blastResultModel.getColumnCount(), 5);
		
		blastResultModel.setHeaderData(ASSINGMENT_COLUMN, tr("detailsForm.summary.assignment"));
		blastResultModel.setHeaderData(DATA_COLUMN, tr("detailsForm.summary.numberSeqs"));
		blastResultModel.setHeaderData(PERCENTAGE_COLUMN, tr("detailsForm.summary.percentage"));
		blastResultModel.setHeaderData(COLOR_COLUMN, tr("detailsForm.summary.legend"));

		// find total 
		double total = 0;
		for (Map.Entry<String, ClusterData> e: clusterDataMap.entrySet()){
			ClusterData toolData = e.getValue();
			total += toolData.sequenceNames.size();
		}

		Config config = Settings.getInstance().getConfig();
		int i = 0;
		for (Map.Entry<String, ClusterData> e: clusterDataMap.entrySet()){
			ClusterData toolData = e.getValue();
			int row = blastResultModel.getRowCount();
			blastResultModel.insertRows(row, 1);
			blastResultModel.setData(row, ASSINGMENT_COLUMN, toolData.concludedName);
			String toolId = config.getToolId(toolData.taxonomyId);
			ToolConfig toolConfig = toolId == null ? null : config.getCurrentVersion(toolId);
			if (toolConfig != null) {
				blastResultModel.setData(row, ASSINGMENT_COLUMN, createToolLink(toolData.taxonomyId, jobId), ItemDataRole.LinkRole);
				blastResultModel.setData(row, DATA_COLUMN, createToolLink(toolData.taxonomyId, jobId), ItemDataRole.LinkRole);
			}
			blastResultModel.setData(row, DATA_COLUMN, toolData.sequenceNames.size()); // percentage
			blastResultModel.setData(row, CHART_DISPLAY_COLUMN, 
					toolData.concludedName + " (" + toolData.sequenceNames.size() + ")");

			WColor color = chart.getPalette().getBrush(i).getColor();
			blastResultModel.setData(row, COLOR_COLUMN, color, 
					ItemDataRole.UserRole + 1);

			blastResultModel.setData(row, PERCENTAGE_COLUMN, 
					(double)toolData.sequenceNames.size() / total * 100.0);

			i++;
		}

		chart.setModel(blastResultModel);

		// copy model to table model

		WStandardItemModel tableModel = new WStandardItemModel();
		for (int r = 0; r < blastResultModel.getRowCount(); r++){
			List<WStandardItem> items = new ArrayList<WStandardItem>();
			for (int c = 0; c < blastResultModel.getColumnCount(); c++)
				items.add(blastResultModel.getItem(r, c));
			tableModel.appendRow(items);
		}

		// Add totals only to table model.
		int row = tableModel.getRowCount();
		tableModel.insertRows(row, 1);
		tableModel.setData(row, ASSINGMENT_COLUMN, "Totals");
		tableModel.setData(row, DATA_COLUMN, total); // percentage
		tableModel.setData(row, PERCENTAGE_COLUMN, 100.0);

		table.setModel(tableModel);

		chartContainer.show();
		table.show();
		WContainerWidget c = new WContainerWidget();
		c.addWidget(chartContainer);
		c.addWidget(table);
		bindResults(c);
	}

	public Signal1<String> jobIdChanged() {
		return jobIdChanged;
	}

	private WLink createToolLink(final String taxonomyId, final String jobId) {
		Config config = Settings.getInstance().getConfig();
		ToolConfig toolConfig = config.getCurrentVersion(config.getToolId(taxonomyId));
		if (toolConfig == null)
			return null;

		String url = GenotypeMain.getApp().getServletContext().getContextPath()
		+ "/typingtool/" + toolConfig.getPath() + "/"
		+ BLAST_JOB_ID_PATH + "/" + getMain().getOrganismDefinition().getToolConfig().getId()
		+ "/" + getMain().getOrganismDefinition().getToolConfig().getVersion() + "/" + jobId;

		WLink link = new WLink(url);
		link.setTarget(AnchorTarget.TargetNewWindow);

		return link;
	}
	
	// Classes
	public static class ClusterData {
		String taxonomyId = new String();
		String concludedName = new String();
		String concludedId = new String();
		List<String> sequenceNames = new ArrayList<String>();
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
	public class BlastResultParser extends GenotypeResultParser {
		private Map<String, ClusterData> clusterDataMap = new HashMap<String, ClusterData>();
		private WApplication app;

		public BlastResultParser() {
			this.app = WApplication.getInstance();
			setReaderBlocksOnEof(true);
		}

		@Override
		public void updateUi() {
			UpdateLock updateLock = app.getUpdateLock();
			updateView();
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
			String concludedId = GenotypeLib
					.getEscapedValue(this,
							"/genotype_result/sequence/result[@id='blast']/cluster/concluded-id");
			String concludedName = GenotypeLib
					.getEscapedValue(this,
							"/genotype_result/sequence/result[@id='blast']/cluster/concluded-name");

			if (concludedName == null)
				concludedName = "Unassigned";

			ClusterData toolData = clusterDataMap.containsKey(concludedId) ? clusterDataMap.get(concludedId) : new ClusterData();

			if (concludedId != null && !concludedId.equals("Unassigned"))
				toolData.taxonomyId = taxonomyId;

			toolData.concludedName = concludedName;
			toolData.sequenceNames.add(seqName);
			toolData.concludedId = concludedId;
			clusterDataMap.put(concludedId, toolData);
		}
	}
}
