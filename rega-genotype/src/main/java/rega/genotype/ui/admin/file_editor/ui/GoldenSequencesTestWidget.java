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

import rega.genotype.FileFormatException;
import rega.genotype.GenotypeTool;
import rega.genotype.ParameterProblemException;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.data.GenotypeResultParser;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader;
import rega.genotype.ui.admin.file_editor.xml.ConfigXmlReader.VerificationTableItem;
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
import eu.webtoolkit.jwt.TextFormat;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WFileResource;
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

		String infoText = "<div>Upload expected results in excel format.</div> <div>Table columns: ";

		for (int i = 0; i < verificationTable.size(); ++i) {
			if (i != 0)
				infoText += ", ";
			infoText += verificationTable.get(i).getDescription();
		}
		infoText +="</div>";
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

		final File traceFile = new File(workDir.getAbsolutePath(), "result.xml");

		
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

				
				// Create report table 
//				WTable reportTable = new WTable(reportC);
//				for (int i = 0; i < totals.length; ++i) {
//					reportTable.getElementAt(0, i).addWidget(new WText(verificationTable.get(i).getDescription()));
//					reportTable.getElementAt(1, i).addWidget(new WText(totals[i]+""));
//				}
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
				} catch (IOException e) {
					e.printStackTrace();
					infoT.setText("Analysis failed: " + e.getMessage());
					return;
				} catch (ParameterProblemException e) {
					e.printStackTrace();
					infoT.setText("Analysis failed: " + e.getMessage());
					return;
				} catch (FileFormatException e) {
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

				File file = ExcelUtils.write(resultsModel, 
						new File(workDir, ToolVerificationWidget.GOLDEN_SEQUENCES_RESULTS_FILE));

				updateLock = app.getUpdateLock();
				WFileResource r = new WFileResource("text", file.getAbsolutePath());
				r.suggestFileName("golden_sequences_analysys_result.xlsx");
				downloadB.setLink(new WLink(r));
				infoT.setText("Analysis finished.");

				app.triggerUpdate();
				updateLock.release();
			}
		});
		t.setName("parserThread");
		t.start();
	}
}