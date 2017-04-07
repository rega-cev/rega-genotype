package rega.genotype.tools.blast;

import java.util.ArrayList;
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
import rega.genotype.ui.framework.widgets.ChartTableWidget;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.ui.viruses.generic.GenericDefinition.ResultColumn;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.chart.WChartPalette;
import eu.webtoolkit.jwt.chart.WStandardPalette;

/**
 * Job overview window for the blast tool.
 * The result of the blast tool analysis is a pie chart that contains a series 
 * per input sequence. Clicking the series will redirect to the typing tool of the selected sequence.
 * 
 * @author michael
 */
public class BlastJobOverviewForm extends AbstractJobOverview {
	public static final String BLAST_JOB_ID_PATH = "blast-job";

	private WStandardItemModel blastResultModel = new WStandardItemModel();
	private String jobId;

	protected BlastResultParser blastResultParser;

	private WChartPalette palette = new WStandardPalette(WStandardPalette.Flavour.Muted);

	private List<ResultColumn> resultColumns;

	private int percentageCountCol;
	private int sequenceCountCol;
	private int legendCountCol;

	public BlastJobOverviewForm(GenotypeWindow main, List<ResultColumn> resultColumns) {
		super(main);
		this.resultColumns = resultColumns;

		sequenceCountCol = 1;
		percentageCountCol = -1;
		legendCountCol = 4;
		for (int c = 0; c < resultColumns.size(); ++c) {
			ResultColumn resultColumn = resultColumns.get(c);
			if (resultColumn.field.equals("sequence-count"))
				sequenceCountCol = c;
			else if (resultColumn.field.equals("legend"))
				legendCountCol = c;
			else if (resultColumn.field.equals("percentage"))
				percentageCountCol = c;
		}
	}

	@Override
	public void init(String jobId, String filter) {
		blastResultParser = null;

		super.init(jobId, filter);
	}
	
	@Override
	public void handleInternalPath(String internalPath) {
		bindResultsEmpty();
		String path[] =  internalPath.split("/");
		if (path.length > 1) {
			jobId = path[1];

			if (!existsJob(jobId)) {
				showBadJobIdError();
				return;
			}

			template.show();
			init(jobId, "");
		} else {
			showBadJobIdError();
		}
	}

	private void showBadJobIdError() {
		setMargin(30);
		addWidget(new WText(tr("monitorForm.nonExistingJobId").arg(jobId)));
		template.hide();
	}

	// Classes
	public static class ClusterData {
		String taxonomyId = new String();
		String concludedName = new String();
		String concludedId = new String();

		int sequenceCount = 0;

		// <column, user data from results file>
		Map<Integer, String> userData = new HashMap<Integer, String>();
	}

	@Override
	protected GenotypeResultParser createParser() {
		blastResultParser = new BlastResultParser(this, resultColumns);
		return blastResultParser;
	}

	protected double totalSequences() {
		Map<String, ClusterData> clusterDataMap = blastResultParser.clusterDataMap;
		double total = 0;
		for (Map.Entry<String, ClusterData> e: clusterDataMap.entrySet()){
			ClusterData toolData = e.getValue();
			total += toolData.sequenceCount;
		}

		return total;
	}
	private WStandardItemModel createBlastModel() {
		WStandardItemModel blastModel = new WStandardItemModel();

		Map<String, ClusterData> clusterDataMap = blastResultParser.clusterDataMap;

		// create blastResultModel
		blastModel = new WStandardItemModel();
		blastModel.insertColumns(blastModel.getColumnCount(), resultColumns.size());

		for (int c = 0; c < resultColumns.size(); ++c)
			blastModel.setHeaderData(c, resultColumns.get(c).label);

		
		// find total 
		double total = totalSequences();
		Config config = Settings.getInstance().getConfig();
		int i = 0;
		for (Map.Entry<String, ClusterData> e: clusterDataMap.entrySet()){
			ClusterData toolData = e.getValue();
			int row = blastModel.getRowCount();
			blastModel.insertRows(row, 1);

			for (int c = 0; c < resultColumns.size(); ++c) {
				ResultColumn resultColumn = resultColumns.get(c);
				if (resultColumn.field.equals("assignment")){
					blastModel.setData(row, c, toolData.concludedName);
					String toolId = config.getToolId(toolData.taxonomyId);
					ToolConfig toolConfig = toolId == null ? null : config.getCurrentVersion(toolId);
					if (toolConfig != null) {
						ToolConfig blastToolConfig = getMain().getOrganismDefinition().getToolConfig();
						blastModel.setData(row, c, createToolLink(
								toolData.taxonomyId, jobId, blastToolConfig), ItemDataRole.LinkRole);
					}
				} else if (resultColumn.field.equals("sequence-count")) {
					blastModel.setData(row, c, toolData.sequenceCount); // percentage
				} else if (resultColumn.field.equals("percentage")) {
					blastModel.setData(row, c, 
							(double)toolData.sequenceCount / total * 100.0);
				} else if (resultColumn.field.equals("legend")) {
					WColor color = palette.getBrush(i).getColor();
					blastModel.setData(row, c, color, 
							ItemDataRole.UserRole + 1);
				} else {
					blastModel.setData(row, c, toolData.userData.get(c));

				}
			}
			i++;
		}
		return blastModel;
	}

	@Override
	public void fillResultsWidget() {
		if (isNgsJob())
			return;

		if (blastResultParser == null || blastResultParser.clusterDataMap.isEmpty()) {
			bindResultsEmpty();
			return;
		}

		blastResultModel = createBlastModel();

		ChartTableWidget view = new ChartTableWidget(
				blastResultModel, sequenceCountCol, legendCountCol, palette, "");

		WContainerWidget horizontalLayout = new WContainerWidget();
		horizontalLayout.addStyleClass("flex-container");

		view.init();
		view.addText(view.getTable().getRowCount(), 0, "Totals");
		view.addTotals(sequenceCountCol, false);
		if (percentageCountCol != -1)
			view.addTotals(percentageCountCol, false);

		view.getTable().setWidth(new WLength(600));
		view.getChartContainer().addStyleClass("flex-elem");
		horizontalLayout.addWidget(view.getTable());
		horizontalLayout.addWidget(view.getChartContainer());
		bindResults(horizontalLayout);
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
		private List<ResultColumn> resultColumns;

		public BlastResultParser(BlastJobOverviewForm form, List<ResultColumn> resultColumns) {
			this.form = form;
			this.resultColumns = resultColumns;
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

			if (concludedId != null && !concludedId.equals("Unassigned"))
				toolData.concludedName = concludedName;

			toolData.sequenceCount++;

			if (toolData.concludedId == null || toolData.concludedId.equals("Unassigned"))
				toolData.concludedId = concludedId;

			for (int c = 0; c < resultColumns.size(); ++c) {
				ResultColumn rc = resultColumns.get(c);
				if (rc.field.startsWith("/genotype_result/"))
					toolData.userData.put(c, 
							GenotypeLib.getEscapedValue(this, resultColumns.get(c).field));
			}
			
			clusterDataMap.put(key, toolData);
		}
	}

	@Override
	protected boolean hasResults() {
		return blastResultParser != null && !blastResultParser.clusterDataMap.isEmpty();
	}
}
