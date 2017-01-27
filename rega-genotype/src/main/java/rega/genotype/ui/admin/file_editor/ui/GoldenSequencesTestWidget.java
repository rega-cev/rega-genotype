package rega.genotype.ui.admin.file_editor.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rega.genotype.GenotypeTool;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader.VerificationTableItem;
import rega.genotype.ui.framework.Constants;
import rega.genotype.ui.framework.widgets.FastaUploadWidget;
import rega.genotype.ui.framework.widgets.StandardTableView;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.util.FileUpload;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.ExcelUtils;
import rega.genotype.utils.FileUtil;
import rega.genotype.viruses.generic.GenericTool;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.WAbstractItemModel;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WStandardItem;
import eu.webtoolkit.jwt.WStandardItemModel;
import eu.webtoolkit.jwt.WText;

/**
 * Run full analysis on a list of sequences and check that the results correspond to expected results list.
 * 
 * @author michael
 */
public class GoldenSequencesTestWidget extends Template {
	FastaUploadWidget fastaUpload = new FastaUploadWidget();
	FileUpload resultsUpload = new FileUpload();
	WText resultsTableT = new WText();
	WText infoT = new WText();
	WPushButton runB = new WPushButton("Run");
	WContainerWidget reportC = new WContainerWidget();
	WPushButton downloadB = new WPushButton("Download xlsx");
	private File workDir;
	
	public GoldenSequencesTestWidget(final ToolConfig toolConfig, final File workDir) {
		super(tr("admin.config.golden-sequences-test-widget"));
		this.workDir = workDir;
		final List<VerificationTableItem> verificationTable =
				ConfigXmlReader.readVerificationTable(toolConfig.getConfigurationFile());

		if (verificationTable.isEmpty()) {
			setTemplateText(tr("admin.config.golden-sequences-test-widget.empty-table"));
			return;
		}

		String infoText = "<div><b>Upload expected results in excel format.</b></div> <div>Table columns: ";

		for (int i = 0; i < verificationTable.size(); ++i) {
			if (i != 0)
				infoText += ", ";
			infoText += verificationTable.get(i).getDescription();
		}
		infoText +="</div>";
		infoText +="<div>Note: ? means that this data can not be calculateed by the tool.</div>";
		resultsTableT.setText(infoText);
		resultsTableT.setMargin(15, Side.Top);

		resultsUpload.getWFileUpload().setFilters(".xlsx");

		runB.setMargin(15, Side.Top, Side.Bottom);

		bindWidget("info", infoT);
		bindWidget("run", runB);
		bindWidget("results-table-format", resultsTableT);
		bindWidget("results-table-upload", resultsUpload);
		bindWidget("fasta-upload", fastaUpload);
		bindWidget("report", reportC);
		bindWidget("download", downloadB);

		final File traceFile = new File(workDir.getAbsolutePath(), Constants.RESULT_FILE_NAME);

		
		if (traceFile.exists()){
			showResults(verificationTable, traceFile);
		}

		runB.clicked().addListener(runB, new Signal.Listener() {
			public void trigger() {
				reportC.clear();
				infoT.setText("");
				infoT.addStyleClass("info-text");

				if (fastaUpload.getText().isEmpty()) {
					infoT.setText("Fasta file is empty");
					return;
				}

				if (resultsUpload.getWFileUpload().getUploadedFiles().isEmpty()) {
					infoT.setText("Upload results file first.");
					return;
				} 

				try {
					FileUtils.copyFile(new File(resultsUpload.getWFileUpload().getSpoolFileName()),
							new File(workDir, ToolVerificationWidget.EXPECTED_RESULTS_FILE));
				} catch (IOException e1) {
					e1.printStackTrace();
					infoT.setText("Internal error could not copy expected results.");
					return;
				}
				
				traceFile.delete();
				
				analyze(traceFile, toolConfig);
				showResults(verificationTable, traceFile);
			}
		});
	}

	private void analyze(final File traceFile, final ToolConfig toolConfig) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				// run analysis
				final File sequenceFile = new File(workDir, "in.fasta");
				try {
					FileUtil.writeStringToFile(sequenceFile, fastaUpload.getText());
				} catch (IOException e) {
					infoT.setText("Interanl error");
					e.printStackTrace();
					return;
				}

				GenotypeTool genotypeTool;
				try {
					genotypeTool = new GenericTool(toolConfig, workDir);
					genotypeTool.analyze(sequenceFile.getAbsolutePath(), traceFile.getAbsolutePath());
				} catch (Exception e) { // Show the tool admin every error in analysis (if it is xml error he can fix it)
					e.printStackTrace();
					infoT.setText("Analysis failed: " + e.getMessage());
					return;
				}
			}
		});
		t.setName("analysis_thread");
		t.start();
	}
	
	private Map<String, List<String>> createExpectedResultsMap() {
		final Map<String, List<String>> expectedResultsMap = new HashMap<String, List<String>>();
		
		File expectedResultsFile = new File(workDir, ToolVerificationWidget.EXPECTED_RESULTS_FILE);
		XSSFWorkbook workbook;
		try {
			workbook = new XSSFWorkbook( new FileInputStream(expectedResultsFile));
		} catch (Exception e) {
			e.printStackTrace();
			infoT.setText("Expected results file error: " + e.getMessage());
			return null;
		} 

		XSSFSheet sheet = workbook.getSheetAt(0);

		Iterator<Row> rowIterator = sheet.iterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			ArrayList<String> expectedList = new ArrayList<String>();
			Iterator<Cell> cellIterator = row.cellIterator();
			while (cellIterator.hasNext()) {
				Cell cell = cellIterator.next();
				String value = ExcelUtils.readCell(cell);
				expectedList.add(value);
			}
			if (expectedList.size() > 0)
				expectedResultsMap.put(expectedList.get(0), expectedList);
		}
		
		try {
			workbook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return expectedResultsMap;
	}

	private WStandardItemModel createResultsModel(final List<VerificationTableItem> verificationTable) {

		// report table
		final WStandardItemModel resultsModel = new WStandardItemModel(0, verificationTable.size() * 2 - 1);
		// key (sequence name) header
		resultsModel.setHeaderData(0, verificationTable.get(0).getDescription());
		for (int c = 2; c <= resultsModel.getColumnCount(); c++){ 
			if (c % 2 == 0)
				resultsModel.setHeaderData(c - 1, "Expected " + verificationTable.get(c / 2).getDescription());
			else
				resultsModel.setHeaderData(c - 1, "Calculated " + verificationTable.get(c / 2).getDescription());
		}
		return resultsModel;
	}


	private static class StatisticsKey {
		List<String> headers = new ArrayList<String>();

		private void add(String header) {
			headers.add(header);
		}
		private String combinHeaders() {
			String ans = "";
			for (String h: headers)
				ans += h;
			return ans;
		}
		@Override
		public int hashCode() {
			return combinHeaders().hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj instanceof StatisticsKey)
				return ((StatisticsKey)obj).combinHeaders().equals(combinHeaders());
			return false;
		}
	}

	private static class StatisticsData {
		int goldStandardAssignement = 0, tp = 0, tn = 0, fp = 0, fn = 0;
	}

	private WStandardItemModel createStatisticsModel(final List<VerificationTableItem> verificationTable,
			final WStandardItemModel resultsModel) {

		int rowHeaderCount = (verificationTable.size() - 1) ;
		String[] dataHeaders = {"Gold standard assignement","TP","TN","FP","FN","SENS","SPEC","PPV","NPV"};


		final WStandardItemModel statisticsModel = new WStandardItemModel(0, rowHeaderCount + dataHeaders.length);

		// headers
		for (int i = 1; i < verificationTable.size(); ++i)
			statisticsModel.setHeaderData(i - 1, 
					verificationTable.get(i).getDescription());

		for (int c = rowHeaderCount; c < dataHeaders.length + rowHeaderCount; ++c)
			statisticsModel.setHeaderData(c, dataHeaders[c - rowHeaderCount]);

		// data

		int totalPositive = resultsModel.getRowCount(); // TODO check if they do negative tests
		Map<StatisticsKey, StatisticsData> data = new HashMap<StatisticsKey, StatisticsData>();

		// Compute row keys
		for (int r =0; r < resultsModel.getRowCount(); r++) {
			StatisticsKey expectedKey = new StatisticsKey();
			StatisticsKey calculatedKey = new StatisticsKey();
			for (int c = 1; c < resultsModel.getColumnCount(); c+=2) {
				Object expected = resultsModel.getData(r, c);
				Object calculated = resultsModel.getData(r, c +1);
				String expectedStr = expected == null ? "?" : expected.toString();
				String calculatedStr = calculated == null ? "?" : calculated.toString();
				expectedKey.add(expectedStr);
				calculatedKey.add(calculatedStr);
			}

			if (!data.containsKey(expectedKey))
				data.put(expectedKey, new StatisticsData());

			StatisticsData expectedStatisticsData = data.get(expectedKey);
			expectedStatisticsData.goldStandardAssignement++;
			if (expectedKey.equals(calculatedKey)) {
				expectedStatisticsData.tp++;
			} else {
				if (!data.containsKey(calculatedKey))
					data.put(calculatedKey, new StatisticsData());
				StatisticsData calculatedStatisticsData = data.get(calculatedKey);
				calculatedStatisticsData.fp++;
			}

			data.put(expectedKey, expectedStatisticsData);
		}

		for(Map.Entry<StatisticsKey, StatisticsData> e: data.entrySet()) {
			final List<WStandardItem> items = new ArrayList<WStandardItem>();
			for (String h: e.getKey().headers)
				items.add(new WStandardItem(h));

			StatisticsData sd = e.getValue();
			sd.tn = totalPositive - sd.goldStandardAssignement - sd.fp;
			sd.fn = sd.goldStandardAssignement - sd.tp;

			String sensitivity = persentageStr(sd.tp,(sd.tp+sd.fn));
			String specificity = persentageStr(sd.tn,(sd.fp+sd.tn));
			String positivePredictiveValue = persentageStr(sd.tp,(sd.tp+sd.fp));
			String negativePredictiveValue = persentageStr(sd.tn,(sd.fn+sd.tn));

			items.add(new WStandardItem("" + sd.goldStandardAssignement));
			items.add(new WStandardItem("" + sd.tp));
			items.add(new WStandardItem("" + sd.tn));
			items.add(new WStandardItem("" + sd.fp));
			items.add(new WStandardItem("" + sd.fn));

			items.add(new WStandardItem(sensitivity));
			items.add(new WStandardItem(specificity));
			items.add(new WStandardItem(positivePredictiveValue));
			items.add(new WStandardItem(negativePredictiveValue));

			statisticsModel.appendRow(items);
		}
		return statisticsModel;
	}

	private void fillResultsModel(final List<VerificationTableItem> verificationTable,
			final File traceFile, final WStandardItemModel resultsModel, final WApplication app) {

		final Map<String, List<String>> expectedResultsMap = createExpectedResultsMap();

		if (expectedResultsMap == null)
			return;

		final int[] totals = new int[verificationTable.size()];
		// parse analysis results.
		GenotypeResultParser parser = new GenotypeResultParser() {
			//TODO:
			String info = new String();
			@Override
			public void endSequence() {
				String seqName = GenotypeLib.getEscapedValue(this,
						"/genotype_result/sequence/@name");
				List<String> expectedList = expectedResultsMap.get(seqName);
				if (expectedList == null) {
					info += "<div> Sequence " + seqName + " is was not found in fasta file </div>";
					return; 
				}

				UpdateLock updateLock = app.getUpdateLock();

				int row = Math.max(resultsModel.getRowCount() -1, 0);
				resultsModel.insertRow(row, new WStandardItem(seqName));
				for (int i = 1; i < verificationTable.size(); ++i) {
					if (i < expectedList.size()) {
						String value = GenotypeLib.getEscapedValue(this,
								verificationTable.get(i).getResultsXmlVariable());
						
						resultsModel.setData(row, i * 2 -1, expectedList.get(i));
						resultsModel.setData(row, i * 2, value);

						if (value == null)
							info += "<div> Sequence " + seqName + " column " + i + " value not found in results list.</div>";
						else if (value.equals(expectedList.get(i))) {
							totals[i]++;
						}
					}
				}

				resultsModel.layoutAboutToBeChanged().trigger();
				resultsModel.layoutChanged().trigger();

				app.triggerUpdate();
				updateLock.release();
				
			}
		};
		parser.setReaderBlocksOnEof(true);
		parser.parseFile(traceFile);
	}

	private void showResults(final List<VerificationTableItem> verificationTable,
			final File traceFile) {
		final WStandardItemModel resultsModel = createResultsModel(verificationTable);
		if (resultsModel == null)
			return;

		final WApplication app = WApplication.getInstance();
		Thread t = new Thread(new Runnable() {
			public void run() {
				UpdateLock updateLock = app.getUpdateLock();

				reportC.clear();
				StandardTableView reportTable = new StandardTableView(reportC);
				reportTable.setModel(resultsModel);
				reportTable.setTableWidth();
				infoT.setText("Analysis start, it can take some time ...");

				app.triggerUpdate();
				updateLock.release();

				// wait till result.xml is ready
				while (!traceFile.exists()){
					synchronized (this) {
						try {
							wait(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
							assert (false);
						}
					}
				}

				fillResultsModel(verificationTable, traceFile, resultsModel, app);
				WStandardItemModel statisticsModel = createStatisticsModel(verificationTable, resultsModel);

				List<WAbstractItemModel> models = new ArrayList<WAbstractItemModel>();
				models.add(statisticsModel);
				models.add(resultsModel);
				File file = ExcelUtils.write(models, 
						new File(workDir, ToolVerificationWidget.GOLDEN_SEQUENCES_RESULTS_FILE));

				updateLock = app.getUpdateLock();
				WFileResource resource = new WFileResource("text", file.getAbsolutePath());
				resource.suggestFileName("golden_sequences_analysys_result.xlsx");
				downloadB.setLink(new WLink(resource));
				infoT.setText("Analysis finished.");

				// statistics model
				StandardTableView statisticsTable = new StandardTableView(reportC);
				statisticsTable.setMargin(10, Side.Top);
				statisticsTable.setModel(statisticsModel);
				// set the last 8 columns width.
				for (int i = 1; i < 9; ++i)
					statisticsTable.setColumnWidth(statisticsModel.getColumnCount() - i, new WLength(60));
				statisticsTable.setTableWidth();

				app.triggerUpdate();
				updateLock.release();
			}
		});
		t.setName("parserThread");
		t.start();
	}

	private String persentageStr(int a, int b) {
		return (b == 0) ? "ERR: /0" : (a/b * 100) + "%";
	}
}