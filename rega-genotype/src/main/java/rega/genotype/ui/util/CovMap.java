package rega.genotype.ui.util;

import java.io.File;
import java.util.EnumSet;
import java.util.List;

import rega.genotype.ApplicationException;
import rega.genotype.Sequence;
import rega.genotype.ngs.NgsConsensusSequenceModel;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.ngs.model.Contig;
import rega.genotype.ui.framework.widgets.ObjectListModel;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.utils.SamtoolsUtil;
import eu.webtoolkit.jwt.AlignmentFlag;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.WBrush;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WPaintDevice;
import eu.webtoolkit.jwt.WPaintedWidget;
import eu.webtoolkit.jwt.WPainter;
import eu.webtoolkit.jwt.WPainterPath;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;
import eu.webtoolkit.jwt.chart.Axis;
import eu.webtoolkit.jwt.chart.AxisConfig;
import eu.webtoolkit.jwt.chart.AxisScale;
import eu.webtoolkit.jwt.chart.ChartType;
import eu.webtoolkit.jwt.chart.FillRangeType;
import eu.webtoolkit.jwt.chart.SeriesType;
import eu.webtoolkit.jwt.chart.WAxis;
import eu.webtoolkit.jwt.chart.WAxisSliderWidget;
import eu.webtoolkit.jwt.chart.WCartesianChart;
import eu.webtoolkit.jwt.chart.WDataSeries;

public class CovMap extends WPaintedWidget{
	public static final double GENOM_HIGHT = 20;
	public static final double GENOM_WIDTH = 400;
	public static final int MARGIN = 15;
	private ConsensusBucket bucket;
	private NgsConsensusSequenceModel model;
	
	public CovMap(final ConsensusBucket bucket, final NgsConsensusSequenceModel model) {
		this.bucket = bucket;
		this.model = model;

		update();
		resize((int)(MARGIN * 2 + GENOM_WIDTH), 50);
		setMargin(WLength.Auto);
		addStyleClass("hoverable");

		clicked().addListener(this, new Signal.Listener() {
			public void trigger() {

				final WApplication app = WApplication.getInstance();
				final StandardDialog d = new StandardDialog("Coverage map: " + bucket.getConcludedName());
				d.getOkB().hide();
				d.getCancelB().setText("Close");
				d.getContents().addWidget(new WText("Creating cverage map please wait. .. ... la .. la la ..."));

				Thread t = new Thread(new Runnable() {
					public void run() {
						try {
							File samFile = model.samFile(bucket);
							File consensusWorkDir = samFile.getParentFile();
							File covFile = new File(consensusWorkDir, "aln.cov");

							if (!covFile.exists())
								SamtoolsUtil.samToCovMap(samFile, covFile, consensusWorkDir);
							UpdateLock updateLock = app.getUpdateLock();
							d.getContents().clear();
							paintChart(covFile, d);
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
		});
	}

	private void paintChart(File covFile, WDialog d) {
		ObjectListModel<Integer> covMapModel = SamtoolsUtil.covMapModel(
				covFile, bucket.getConsensusSequence().getLength() + 1);

		Template template = new Template(tr("cov-map"), d.getContents()); 

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
		chart.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

		WAxisSliderWidget sliderWidget = new WAxisSliderWidget(s);

		sliderWidget.resize(new WLength(800), new WLength(80));
		sliderWidget.setSelectionAreaPadding(40, EnumSet.of(Side.Left,
				Side.Right));
		sliderWidget.setMargin(WLength.Auto, EnumSet.of(Side.Left, Side.Right));

	    //wcon
	    WAnchor samAnchor = new WAnchor(model.samLink(bucket));
	    samAnchor.setText("sam");
	    WAnchor bamAnchor = new WAnchor(model.bamLink(bucket));
	    bamAnchor.setText("bam");

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
	
	@Override
	protected void paintEvent(WPaintDevice paintDevice) {
		WPainterPath genomePath = new WPainterPath();
		genomePath.addRect(MARGIN, MARGIN, GENOM_WIDTH, GENOM_HIGHT);
		genomePath.addRect(MARGIN * 0.25, GENOM_HIGHT * 0.25 + MARGIN, 
				MARGIN * 0.75, GENOM_HIGHT * 0.5);
		genomePath.addRect(MARGIN + GENOM_WIDTH, GENOM_HIGHT * 0.25 + MARGIN, 
				MARGIN * 0.75, GENOM_HIGHT * 0.5);

		WPainter painter = new WPainter(paintDevice);
		painter.drawPath(genomePath);
		painter.drawText(5, 1, MARGIN, MARGIN, EnumSet.of(AlignmentFlag.AlignLeft), "5'");
		painter.drawText(GENOM_WIDTH + MARGIN, 1, MARGIN, MARGIN, 
				EnumSet.of(AlignmentFlag.AlignRight), "3'");
		painter.drawText(GENOM_WIDTH / 2 + MARGIN, 1, MARGIN, MARGIN, 
				EnumSet.of(AlignmentFlag.AlignCenter), bucket.getRefName());
		painter.drawText(MARGIN, GENOM_HIGHT + MARGIN + 1,
				MARGIN, MARGIN, EnumSet.of(AlignmentFlag.AlignLeft), "0");
		painter.drawText(GENOM_WIDTH, GENOM_HIGHT + MARGIN + 1,
				MARGIN, MARGIN,  EnumSet.of(AlignmentFlag.AlignRight), bucket.getRefLen()+"");

		double maxCov = maxCov(bucket.getContigs());
		for (Contig contig: bucket.getContigs()) {
			WPainterPath gcontigPath = new WPainterPath();
			double start = scale(bucket.getRefLen(), 
					contig.getEndPosition() - contig.getLength());
			double width = scale(bucket.getRefLen(), contig.getLength());
			gcontigPath.addRect(MARGIN + start, MARGIN, width, GENOM_HIGHT);
			double scale = contig.getCov() / maxCov; // scale colors by contig cov in ref.
			int alpha = (int) (scale * 255);
			painter.setBrush(new WBrush(new WColor(50, 150, 50, alpha)));
			painter.drawPath(gcontigPath);
		}
	}
	
	public static File genomeImage(final ConsensusBucket bucket, final File workDir) {
		File imagesDir = new File(workDir, "genome-images");
		imagesDir.mkdirs();
		return new File(imagesDir, bucket.getDiamondBucket() + bucket.getRefName());
	}

	public static double scale(double genomeSize, double x) {
		double scaleFactor = GENOM_WIDTH / genomeSize;
		return (double)x * scaleFactor;
	}

	private static double maxCov(List<Contig> contigs) {
		double ans = 0;
		for (Contig contig: contigs) 
			ans = Math.max(ans, contig.getCov());

		return ans;
	}

	public static class SequenceAxis extends WAxis {
		Sequence sequence;
		public SequenceAxis(Sequence sequence) {
			this.sequence = sequence;
		}

		@Override
		protected void getLabelTicks(List<TickLabel> ticks, int segment,
				AxisConfig config) {
			int showSeqZoom = 55;
			double zoomLevel = Math.pow(2, config.zoomLevel);
			if (getZoom() > showSeqZoom && zoomLevel > showSeqZoom){
				int min = (int) getZoomMinimum();
				boolean roundTop = getZoomMinimum() > 1000;
				for (int i = min; i < getZoomMaximum(); ++i)
					ticks.add(new TickLabel((double)i,
							WAxis.TickLabel.TickLength.Short, 
							new WString(sequence.getSequence().charAt(roundTop ? i - 1 : i))));
			} else
				super.getLabelTicks(ticks, segment, config);
		}
	}
}
