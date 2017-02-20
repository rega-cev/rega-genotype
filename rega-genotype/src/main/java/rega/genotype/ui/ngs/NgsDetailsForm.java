package rega.genotype.ui.ngs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.EnumSet;

import rega.genotype.ApplicationException;
import rega.genotype.ngs.NgsFileSystem;
import rega.genotype.ngs.NgsResultsParser;
import rega.genotype.ngs.NgsResultsTracer;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.ngs.model.NgsResultsModel;
import rega.genotype.ui.forms.AbstractForm;
import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.framework.widgets.ObjectListModel;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.ngs.CovMap.SequenceAxis;
import rega.genotype.utils.FileUtil;
import rega.genotype.utils.GsonUtil;
import rega.genotype.utils.SamtoolsUtil;
import rega.genotype.utils.SamtoolsUtil.RefType;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WBrush;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WComboBox;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WPushButton;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.chart.Axis;
import eu.webtoolkit.jwt.chart.AxisScale;
import eu.webtoolkit.jwt.chart.ChartType;
import eu.webtoolkit.jwt.chart.FillRangeType;
import eu.webtoolkit.jwt.chart.SeriesType;
import eu.webtoolkit.jwt.chart.WAxisSliderWidget;
import eu.webtoolkit.jwt.chart.WCartesianChart;
import eu.webtoolkit.jwt.chart.WDataSeries;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;
import eu.webtoolkit.jwt.utils.StreamUtils;

public class NgsDetailsForm extends AbstractForm{
	public static final String BUCKET_PATH = "bucket";
	private NgsResultsModel model;
	private ConsensusBucket bucket;

	private WContainerWidget covMapContainer = new WContainerWidget();
	private File workDir;
	
	public NgsDetailsForm(GenotypeWindow main, final File workDir, final String bucketId) {
		super(main);
		this.workDir = workDir;

		// prepare NGS data
		NgsResultsParser ngsParser = new NgsResultsParser(){
			protected void endParsingBucket(ConsensusBucket bucket) {
				if (bucket.getBucketId().equals(bucketId)) {
					NgsDetailsForm.this.bucket = bucket;
					NgsDetailsForm.this.model = getModel();
					endParsing();
				}
			}
		};
		final File ngsResultFile = new File(workDir, NgsResultsTracer.NGS_RESULTS_FILL);
		ngsParser.parseFile(ngsResultFile);
	}

	private void endParsing() {

		final WPushButton startB = new WPushButton("Create map");
		final WComboBox chooseCovMapTypeCB = new WComboBox();
		chooseCovMapTypeCB.addItem("Consensus alignment");
		chooseCovMapTypeCB.addItem("Refrence");

		Template chooseTypeTemplate = new Template(tr("choose-cov-map-type"), this);
		String text = tr("choose-cov-map-type-text").arg(bucket.getConcludedName()).arg(bucket.getRefName()).toString();
		chooseTypeTemplate.bindString("text", text);
		chooseTypeTemplate.bindString("concluded-name", bucket.getConcludedName());
		chooseTypeTemplate.bindWidget("start", startB);
		chooseTypeTemplate.bindWidget("cb", chooseCovMapTypeCB);
		chooseTypeTemplate.bindWidget("cov-map", covMapContainer);

		startB.clicked().addListener(startB, new Signal.Listener() {
			public void trigger() {
				covMapContainer.clear();
				covMapContainer.addWidget(new WText("Creating coverage map please wait.."));
				RefType refType = chooseCovMapTypeCB.getCurrentIndex() == 1 ? RefType.Refrence : RefType.Consensus;
				creatCovMap(refType);
			}
		});
	}

	private void creatCovMap(final RefType refType) {
		final WApplication app = WApplication.getInstance();

		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					File samFile = samFile(bucket, refType);
					File consensusWorkDir = samFile.getParentFile();
					File covFile = new File(consensusWorkDir, 
							refType == RefType.Refrence ? NgsFileSystem.ALINGMENT_REF_COV_FILE : NgsFileSystem.ALINGMENT_CONSENSUS_COV_FILE);

					if (!covFile.exists())
						SamtoolsUtil.samToCovMap(samFile, covFile, consensusWorkDir, refType);
					UpdateLock updateLock = app.getUpdateLock();
					covMapContainer.clear();
					addCovMap(covFile, refType);

					app.triggerUpdate();
					updateLock.release();
				} catch (ApplicationException e) {
					e.printStackTrace();
					StandardDialog d = new StandardDialog("Coverage map: " + bucket.getConcludedName());
					d.getOkB().hide();
					d.getCancelB().setText("Close");
					d.getContents().addWidget(new WText("Error: " + e.getMessage()));

					return;
				}
			}
		});
		t.start();
	}

	private void addCovMap(final File covFile, final RefType refType) {
		ObjectListModel<Integer> covMapModel = SamtoolsUtil.covMapModel(
				covFile, bucket.getConsensusSequence().getLength() + 1);

		Template template = new Template(tr("cov-map"), covMapContainer); 

		final WCartesianChart chart = new WCartesianChart();
		chart.setBackground(new WBrush(new WColor(220, 220, 220)));
		chart.setModel(covMapModel);
		chart.setType(ChartType.ScatterPlot);

		final SequenceAxis sequenceAxis = new SequenceAxis(
				bucket.getConsensusSequence());
		chart.setAxis(sequenceAxis, Axis.XAxis);

		chart.getAxis(Axis.XAxis).setScale(AxisScale.LinearScale);
		chart.getAxis(Axis.XAxis).setMaximum(covMapModel.getRowCount());
		chart.getAxis(Axis.XAxis).setMinimumZoomRange(10);

		WDataSeries s = new WDataSeries(0, SeriesType.LineSeries);
		s.setFillRange(FillRangeType.MinimumValueFill);
		chart.addSeries(s);
		chart.resize(new WLength(800), new WLength(400));

		WAxisSliderWidget sliderWidget = new WAxisSliderWidget(s);

		sliderWidget.resize(new WLength(800), new WLength(80));
		sliderWidget.setSelectionAreaPadding(40, EnumSet.of(Side.Left,
				Side.Right));

	    //wcon
	    WAnchor samAnchor = new WAnchor(samLink(bucket, refType));
	    samAnchor.setText("sequence-alignment.sam");
	    WAnchor bamAnchor = new WAnchor(bamLink(bucket, refType));
	    bamAnchor.setText("sequence-alignment.bam");

	    int totalCov = 0;
	    int totalContigsLength = (int) bucket.getTotalContigsLen();
	    for (Integer cov: covMapModel.getObjects())
	    	totalCov += cov;

	    try {
		    Integer readCount = refType == RefType.Refrence ? countRefReads(bucket) : countConsensusReads(bucket);
	    	template.bindString("read-count", "" + readCount);
		} catch (ApplicationException e) {
			e.printStackTrace();
			template.bindEmpty("read-count");
		}

    	template.bindString("deep-cov", "" + (totalCov / totalContigsLength));
    	template.bindString("title", refType == RefType.Refrence ? tr("cov-map.ref-title") : tr("cov-map.consensus-title"));
    	
		template.bindWidget("map", chart);
		template.bindWidget("slider", sliderWidget);
		template.bindWidget("sam", samAnchor);
		template.bindWidget("bam", bamAnchor);

		sequenceAxis.zoomRangeChanged().addListener(chart, new Signal2.Listener<Double, Double>() {
			public void trigger(Double min, Double max) {
				chart.update();
			}
		});
	}

	public File samFile(final ConsensusBucket bucket, final RefType refType) throws ApplicationException {
		File samFile = SamtoolsUtil.samFile(bucket, workDir, refType);
		if (!samFile.exists())
			samFile = createSamFile(bucket, refType);
			
		return samFile;
	}

	public File createSamFile(final ConsensusBucket bucket, final RefType refType) throws ApplicationException {
		return SamtoolsUtil.createSamFile(bucket, workDir, 
				model.getFastqPE1FileName(), model.getFastqPE2FileName(), refType);
	}

	public WLink samLink(final ConsensusBucket bucket, final RefType refType) {
		WResource r = new WResource() {
			
			@Override
			protected void handleRequest(WebRequest request, WebResponse response)
					throws IOException {
				File samFile;
				try {
					samFile = createSamFile(bucket, refType);
				} catch (ApplicationException e1) {
					e1.printStackTrace();
					response.setStatus(404);
					return;
				}
				response.setContentType("text");
				try {
					FileInputStream fis = new FileInputStream(samFile);
					try {
						StreamUtils.copy(fis, response.getOutputStream());
						response.getOutputStream().flush();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						StreamUtils.closeQuietly(fis);
					}
				} catch (FileNotFoundException e) {
					System.err.println("Could not find file: " + samFile.getAbsolutePath());
					response.setStatus(404);
				}
			}
		};
		r.suggestFileName("sequence-alignment.sam");
		WLink link = new WLink(r);
		link.setTarget(AnchorTarget.TargetDownload);
		return link;
	}

	public WLink bamLink(final ConsensusBucket bucket, final RefType refType) {
		WResource r = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response)
					throws IOException {
				File bam = refType == RefType.Refrence ?
						NgsFileSystem.bamRefSortedFile(workDir, bucket.getDiamondBucket(), bucket.getRefName())
						: NgsFileSystem.bamConsensusSortedFile(workDir, bucket.getDiamondBucket(), bucket.getRefName());

				response.setContentType("text");
				try {
					FileInputStream fis = new FileInputStream(bam);
					try {
						StreamUtils.copy(fis, response.getOutputStream());
						response.getOutputStream().flush();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						StreamUtils.closeQuietly(fis);
					}
				} catch (FileNotFoundException e) {
					System.err.println("Could not find file: " + bam.getAbsolutePath());
					response.setStatus(404);
				}
			}
		};
		r.suggestFileName("sequence-alignment.bam");
		WLink link = new WLink(r);
		link.setTarget(AnchorTarget.TargetDownload);
		return link;
	}

	public Integer countRefReads(final ConsensusBucket bucket) throws ApplicationException {

		File bam = NgsFileSystem.bamRefSortedFile(workDir,
				bucket.getDiamondBucket(), bucket.getRefName());

		SamtoolsResults samtoolsResults = SamtoolsResults.read(bam.getParentFile());
		if (samtoolsResults != null 
				&& samtoolsResults.refReadCount != null)
			return samtoolsResults.refReadCount;

		Integer ans = SamtoolsUtil.countReads(bam);

		samtoolsResults = new SamtoolsResults();
		try {
			samtoolsResults.refReadCount = ans;
		} catch (NumberFormatException e){
			e.printStackTrace();
			return null;
		}
		samtoolsResults.save(bam.getParentFile());
		return samtoolsResults.refReadCount;
	}

	public Integer countConsensusReads(final ConsensusBucket bucket) throws ApplicationException {

		File bam = NgsFileSystem.bamConsensusSortedFile(workDir,
				bucket.getDiamondBucket(), bucket.getRefName());

		SamtoolsResults samtoolsResults = SamtoolsResults.read(bam.getParentFile());
		if (samtoolsResults != null 
				&& samtoolsResults.consensusReadCount != null)
			return samtoolsResults.consensusReadCount;

		Integer ans = SamtoolsUtil.countReads(bam);

		samtoolsResults = new SamtoolsResults();
		try {
			samtoolsResults.consensusReadCount = ans;
		} catch (NumberFormatException e){
			e.printStackTrace();
			return null;
		}
		samtoolsResults.save(bam.getParentFile());
		return samtoolsResults.consensusReadCount;
	}

	public static class SamtoolsResults {
		public static final String SAMTOOLS_RESULTS_FILE = "samtools-results.json";
		public Integer consensusReadCount = null;
		public Integer refReadCount = null;

		public SamtoolsResults() {
		}

		public static SamtoolsResults read(File xmlDir) {
			File f = new File(xmlDir, SAMTOOLS_RESULTS_FILE);
			if (!f.exists())
				return null;

			return GsonUtil.parseJson(FileUtil.readFile(f),
					SamtoolsResults.class);
		}

		public void save(File externalDir) {
			try {
				FileUtil.writeStringToFile(
						new File(externalDir, SAMTOOLS_RESULTS_FILE), 
						GsonUtil.toJson(this));
			} catch (IOException e) {
				e.printStackTrace();
				assert(false);
			}
		}
	}

	@Override
	public void handleInternalPath(String internalPath) {		
	}
}
