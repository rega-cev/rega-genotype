package rega.genotype.tools.blast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.ngs.model.Contig;
import rega.genotype.singletons.Settings;
import rega.genotype.ui.forms.AbstractJobOverview;
import rega.genotype.ui.forms.JobOverviewSummary;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.ChartTableWidget;
import rega.genotype.ui.util.GenotypeLib;
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

	private int ASSINGMENT_COLUMN =    0;
	private int SEQUENCE_COUNT_COLUMN =1; // sequence count column. percentages of the chart.
	private int PERCENTAGE_COLUMN =    2; // deep cov for ngs
	private int SRC_COLUMN =           3;
	private int COLOR_COLUMN =         4;

	private WStandardItemModel blastResultModel = new WStandardItemModel();
	private String jobId;

	protected BlastResultParser blastResultParser;

	private WChartPalette palette = new WStandardPalette(WStandardPalette.Flavour.Muted);


	public BlastJobOverviewForm(GenotypeWindow main) {
		super(main);
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
		blastModel.insertColumns(blastModel.getColumnCount(), 5);

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
//			blastModel.setData(row, CHART_DISPLAY_COLUMN, 
//					toolData.concludedName + " (" + toolData.sequencesData.size() + ")");

			WColor color = palette.getBrush(i).getColor();
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
		if (isNgsJob())
			return;

		if (blastResultParser == null || blastResultParser.clusterDataMap.isEmpty()) {
			bindResultsEmpty();
			return;
		}

		blastResultModel = createBlastModel();
		
		ChartTableWidget view = new ChartTableWidget(
				blastResultModel, PERCENTAGE_COLUMN, COLOR_COLUMN, palette, "");
		
		WContainerWidget horizontalLayout = new WContainerWidget();
		horizontalLayout.addStyleClass("flex-container");

		view.init();
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
	
	// Classes
	public static class ClusterData {
		public String taxonomyId = new String();
		public String concludedName = new String();
		public String concludedId = new String();
		public String src = new String();

		public ConsensusBucket bucketData = null;

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

	@Override
	protected boolean hasResults() {
		return blastResultParser != null && !blastResultParser.clusterDataMap.isEmpty();
	}
}
