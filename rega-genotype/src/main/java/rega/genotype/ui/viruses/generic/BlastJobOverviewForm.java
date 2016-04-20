package rega.genotype.ui.viruses.generic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rega.genotype.config.Config;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.forms.AbstractForm;
import rega.genotype.ui.framework.GenotypeMain;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.Settings;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.chart.LabelOption;
import eu.webtoolkit.jwt.chart.WPieChart;

/**
 * Job overview window for the blast tool.
 * The result of the blast tool analysis is a pie chart that contains a series 
 * per input sequence. Clicking the series will redirect to the typing tool of the selected sequence.
 * 
 * @author michael
 */
public class BlastJobOverviewForm extends AbstractForm {
	public static final String BLAST_JOB_ID_PATH = "blast-job";

	private static final int DISPLAY_COLUMN = 0;
	private static final int DATA_COLUMN = 1;

	private Template layout = new Template(tr("blast-overview"), this);
	private WStandardItemModel blastResultModel = new WStandardItemModel();
	// <toolId, tool data>
	private Map<String, ToolData> toolDataMap = new HashMap<String, ToolData>();
	private Signal1<String> jobIdChanged = new Signal1<String>();

	public BlastJobOverviewForm(GenotypeWindow main) {
		super(main);
		layout.bindString("chart", "Empty data.");
	}

	@Override
	public void handleInternalPath(String internalPath) {
		String path[] =  internalPath.split("/");
		if (path.length > 1) {
			String jobId = path[1];

			File jobDir = new File(getMain().getOrganismDefinition().getJobDir() 
					+ File.separatorChar + jobId);

			new BlastResultParser().parseFile(jobDir);
			fillBlastResultsChart(jobId);

			jobIdChanged.trigger(jobId);
		} else {
			jobIdChanged.trigger("");
		}
	}

	private void fillBlastResultsChart(String jobId) {
		// create blastResultModel
		blastResultModel = new WStandardItemModel();
		blastResultModel.insertColumns(blastResultModel.getColumnCount(), 2);
		blastResultModel.setHeaderData(DISPLAY_COLUMN, "Sequence name");
		blastResultModel.setHeaderData(DATA_COLUMN, "Tool id");

		Config config = Settings.getInstance().getConfig();
		for (Map.Entry<String, ToolData> e: toolDataMap.entrySet()){
			String toolId = e.getKey();
			int row = blastResultModel.getRowCount();
			blastResultModel.insertRows(row, 1);
			blastResultModel.setData(row, DISPLAY_COLUMN, toolId);
			ToolConfig toolConfig = config.getLastPublishedToolConfig(toolId);
			if (toolConfig != null) 
				blastResultModel.setData(row, 1, createToolLink(toolId, jobId), ItemDataRole.LinkRole);
			blastResultModel.setData(row, DATA_COLUMN, e.getValue().sequences.size()); // percentage
		}
		// create chart

		WPieChart chart = new WPieChart();
		chart.setModel(blastResultModel);
		chart.setLabelsColumn(DISPLAY_COLUMN);    
		chart.setDataColumn(DATA_COLUMN);
		chart.resize(800, 300);

	    chart.setMargin(new WLength(30), EnumSet.of(Side.Top, Side.Bottom));
	    chart.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));
		chart.setDisplayLabels(LabelOption.Outside, LabelOption.TextLabel);
		chart.setPlotAreaPadding(30);


		layout.bindWidget("chart", chart);

		for (int i = 0; i < blastResultModel.getRowCount(); ++i) {
			System.err.println(blastResultModel.getData(i, 0, ItemDataRole.DisplayRole));
		}		
	}

	public Signal1<String> jobIdChanged() {
		return jobIdChanged;
	}

	public static File sequenceFileInBlastTool(String blastJobId, String toolId){
		String blastJobDir = Settings.getInstance().getConfig().getBlastTool().getJobDir();
		return new File(blastJobDir + File.separator + blastJobId, toolId + ".fasta");
	}

	private WLink createToolLink(final String toolId, final String jobId) {
		ToolConfig toolConfig = Settings.getInstance().getConfig().getLastPublishedToolConfig(toolId);
		if (toolConfig == null)
			return null;

		saveFastaSequence(toolId, sequenceFileInBlastTool(jobId, toolId));

		String url = GenotypeMain.getApp().getServletContext().getContextPath()
		+ "/typingtool/" + toolConfig.getPath() + "/"
		+ BLAST_JOB_ID_PATH + "/" + jobId;

		return new WLink(url);
	}

	private void saveFastaSequence(String toolId, File file) {
		if (!file.exists()) {
			try {
				FileUtil.writeStringToFile(file, fastaContent(toolId));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String fastaContent(String toolId) {
		String ans = "";
		ToolData toolData = toolDataMap.get(toolId);
		for(SequenceData d:toolData.sequences){
			ans += ">" + d.sequenceName + "\n" + d.nucleotides + "\n";
		}
		return ans;
	}
	
	// Classes
	private class ToolData {
		List<SequenceData> sequences = new ArrayList<SequenceData>();
	}

	private class SequenceData {
		private String sequenceName = new String();
		private String nucleotides  = new String();
		public SequenceData(String sequenceName, String nucleotides) {
			this.sequenceName = sequenceName;
			this.nucleotides = nucleotides;
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
			String nucleotides = GenotypeLib.getEscapedValue(this, "/genotype_result/sequence/nucleotides");

			ToolData toolData = toolDataMap.containsKey(toolId) ?
					toolDataMap.get(toolId) : new ToolData();

			toolData.sequences.add(new SequenceData(seqName, nucleotides));
			toolDataMap.put(toolId, toolData);
		}
	}
}
