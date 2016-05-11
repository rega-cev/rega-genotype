package rega.genotype.ui.tools.blast;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.JobOverviewSummary;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WTableView;
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
	// <toolId, tool data>
	private Map<String, ToolData> toolDataMap = new HashMap<String, ToolData>();
	private Signal1<String> jobIdChanged = new Signal1<String>();
	private String jobId;
	private WPieChart chart;
	private WTableView table = new WTableView();

	public BlastJobOverviewForm(GenotypeWindow main) {
		super(main);

		// create chart
		chart = new WPieChart();
		chart.setModel(blastResultModel);
		chart.setLabelsColumn(CHART_DISPLAY_COLUMN);    
		chart.setDataColumn(DATA_COLUMN);
		chart.resize(800, 300);
		chart.setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
		chart.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);

		// table
		table.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		table.setWidth(new WLength(300));
		table.setStyleClass("blastResultsTable");
		table.hideColumn(CHART_DISPLAY_COLUMN);
	}

	@Override
	public void handleInternalPath(String internalPath) {
		table.hide();
		chart.hide();
		toolDataMap.clear();

		String path[] =  internalPath.split("/");
		if (path.length > 1) {
			jobId = path[1];

			init(jobId, "");
			jobIdChanged.trigger(jobId);
		} else {
			jobIdChanged.trigger("");
		}
	}

	@Override
	protected void fillResultsWidget(String filter) {
		new BlastResultParser().parseFile(jobDir);
		fillBlastResultsChart();
		if (!toolDataMap.isEmpty()){
			table.hide();
			chart.show();
			table.show();
			WContainerWidget c = new WContainerWidget();
			c.addWidget(chart);
			c.addWidget(table);
			bindResults(c);
		}
	}

	private void fillBlastResultsChart() {
		if (toolDataMap.isEmpty())
			return;

		// create blastResultModel
		blastResultModel = new WStandardItemModel();
		blastResultModel.insertColumns(blastResultModel.getColumnCount(), 3);
		blastResultModel.setHeaderData(ASSINGMENT_COLUMN, "Assignment");
		blastResultModel.setHeaderData(DATA_COLUMN, "Sequence count");

		Config config = Settings.getInstance().getConfig();
		for (Map.Entry<String, ToolData> e: toolDataMap.entrySet()){
			String toolId = e.getKey();
			int row = blastResultModel.getRowCount();
			blastResultModel.insertRows(row, 1);
			blastResultModel.setData(row, ASSINGMENT_COLUMN, e.getValue().concludedName);
			ToolConfig toolConfig = config.getLastPublishedToolConfig(toolId);
			if (toolConfig != null) {
				blastResultModel.setData(row, ASSINGMENT_COLUMN, createToolLink(toolId, jobId), ItemDataRole.LinkRole);
				blastResultModel.setData(row, DATA_COLUMN, createToolLink(toolId, jobId), ItemDataRole.LinkRole);
			}
			blastResultModel.setData(row, DATA_COLUMN, e.getValue().sequences.size()); // percentage
			blastResultModel.setData(row, CHART_DISPLAY_COLUMN, 
					e.getValue().concludedName + " (" + e.getValue().sequences.size() + ")");
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
	private class ToolData {
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

			ToolData toolData = toolDataMap.containsKey(toolId) ?
					toolDataMap.get(toolId) : new ToolData();

			toolData.concludedName = concludedName;
			toolData.sequences.add(new SequenceData(seqName));
			toolDataMap.put(toolId, toolData);
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
