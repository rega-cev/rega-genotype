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
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.PositionScheme;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WPainter;
import eu.webtoolkit.jwt.WRectF;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WTableView;
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

	private static final int ASSINGMENT_COLUMN = 0;
	private static final int DATA_COLUMN = 1; // sequence count column. percentages of the chart.
	private static final int CHART_DISPLAY_COLUMN = 2;


	//private Template layout = new Template(tr("job-overview-form"), this);
	private WStandardItemModel blastResultModel = new WStandardItemModel();
	// <concludedId (cluster id), cluster data>
	private Map<String, ClusterData> clusterDataMap = new HashMap<String, ClusterData>();
	private Signal1<String> jobIdChanged = new Signal1<String>();
	private String jobId;
	private WContainerWidget chartContainer = new WContainerWidget(); // used as a layer to draw the anchors on top of the chart.
	private WPieChart chart;
	private WTableView table = new WTableView();

	public BlastJobOverviewForm(GenotypeWindow main) {
		super(main);

		// table
		table.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		table.setWidth(new WLength(300));
		table.setStyleClass("blastResultsTable");
		table.hideColumn(CHART_DISPLAY_COLUMN);
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

		chart.setModel(blastResultModel);
		chart.setLabelsColumn(CHART_DISPLAY_COLUMN);    
		chart.setDataColumn(DATA_COLUMN);
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);
	}

	@Override
	public void handleInternalPath(String internalPath) {
		table.hide();
		chartContainer.hide();
		createChart();
		clusterDataMap.clear();

		String path[] =  internalPath.split("/");
		if (path.length > 1) {
			jobId = path[1];

			if (!existsJob(jobId)) {
				showBadJobIdError();
				return;
			}

			init(jobId, "");
			jobIdChanged.trigger(jobId);
		} else {
			jobIdChanged.trigger("");
			showBadJobIdError();
		}
	}

	private void showBadJobIdError() {
		clear();
		setMargin(30);
		addWidget(new WText(tr("monitorForm.nonExistingJobId").arg(jobId)));
	}

	@Override
	protected void fillResultsWidget(String filter) {
		new BlastResultParser().parseFile(jobDir);
		fillBlastResultsChart();
		if (!clusterDataMap.isEmpty()){
			chartContainer.show();
			table.show();
			WContainerWidget c = new WContainerWidget();
			c.addWidget(chartContainer);
			c.addWidget(table);
			bindResults(c);
		}
	}

	private void fillBlastResultsChart() {
		if (clusterDataMap.isEmpty())
			return;

		// create blastResultModel
		blastResultModel = new WStandardItemModel();
		blastResultModel.insertColumns(blastResultModel.getColumnCount(), 3);
		blastResultModel.setHeaderData(ASSINGMENT_COLUMN, "Assignment");
		blastResultModel.setHeaderData(DATA_COLUMN, "Sequence count");

		Config config = Settings.getInstance().getConfig();
		for (Map.Entry<String, ClusterData> e: clusterDataMap.entrySet()){
			ClusterData toolData = e.getValue();
			int row = blastResultModel.getRowCount();
			blastResultModel.insertRows(row, 1);
			blastResultModel.setData(row, ASSINGMENT_COLUMN, toolData.concludedName);
			ToolConfig toolConfig = config.getLastPublishedToolConfig(toolData.toolId);
			if (toolConfig != null) {
				blastResultModel.setData(row, ASSINGMENT_COLUMN, createToolLink(toolData.toolId, jobId), ItemDataRole.LinkRole);
				blastResultModel.setData(row, DATA_COLUMN, createToolLink(toolData.toolId, jobId), ItemDataRole.LinkRole);
			}
			blastResultModel.setData(row, DATA_COLUMN, toolData.sequences.size()); // percentage
			blastResultModel.setData(row, CHART_DISPLAY_COLUMN, 
					toolData.concludedName + " (" + toolData.sequences.size() + ")");
		}
		chart.setModel(blastResultModel);
		table.setModel(blastResultModel);
	}

	public Signal1<String> jobIdChanged() {
		return jobIdChanged;
	}

	private WLink createToolLink(final String toolId, final String jobId) {
		ToolConfig toolConfig = Settings.getInstance().getConfig().getLastPublishedToolConfig(toolId);
		if (toolConfig == null)
			return null;

		String url = GenotypeMain.getApp().getServletContext().getContextPath()
		+ "/typingtool/" + toolConfig.getPath() + "/"
		+ BLAST_JOB_ID_PATH + "/" + getMain().getOrganismDefinition().getToolConfig().getVersion() + "/" + jobId;

		return new WLink(url);
	}
	
	// Classes
	private class ClusterData {
		private String toolId = new String();
		private String concludedName = new String();
		List<SequenceData> sequences = new ArrayList<SequenceData>();
	}

	private class SequenceData {
		private String sequenceName = new String();
		//private String nucleotides  = new String();
		public SequenceData(String sequenceName) {
			this.sequenceName = sequenceName;
		}
	} 

	/**
	 * Parse result.xml file from job dir and fill the output to blastResultModel. 
	 */
	private class BlastResultParser extends GenotypeResultParser {
		@Override
		public void endSequence() {
			String toolId = GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/result[@id='blast']/cluster/tool-id");
			String seqName = GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/@name");
			String concludedId = GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/result[@id='blast']/cluster/concluded-id");
			String concludedName = GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/result[@id='blast']/cluster/concluded-name");

			if (concludedName == null)
				concludedName = "Unassigned";

			ClusterData toolData = clusterDataMap.containsKey(concludedId) ?
					clusterDataMap.get(concludedId) : new ClusterData();

			toolData.toolId = toolId;	
			toolData.concludedName = concludedName;
			toolData.sequences.add(new SequenceData(seqName));
			clusterDataMap.put(concludedId, toolData);
		}
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
}
