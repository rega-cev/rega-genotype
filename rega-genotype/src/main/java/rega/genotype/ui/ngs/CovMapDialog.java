package rega.genotype.ui.ngs;

import java.io.File;
import java.util.EnumSet;

import rega.genotype.ApplicationException;
import rega.genotype.ngs.NgsConsensusSequenceModel;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.ui.framework.widgets.ObjectListModel;
import rega.genotype.ui.framework.widgets.StandardDialog;
import rega.genotype.ui.framework.widgets.Template;
import rega.genotype.ui.ngs.CovMap.SequenceAxis;
import rega.genotype.utils.SamtoolsUtil;
import eu.webtoolkit.jwt.WApplication.UpdateLock;
import eu.webtoolkit.jwt.chart.Axis;
import eu.webtoolkit.jwt.chart.AxisScale;
import eu.webtoolkit.jwt.chart.ChartType;
import eu.webtoolkit.jwt.chart.FillRangeType;
import eu.webtoolkit.jwt.chart.SeriesType;
import eu.webtoolkit.jwt.chart.WAxisSliderWidget;
import eu.webtoolkit.jwt.chart.WCartesianChart;
import eu.webtoolkit.jwt.chart.WDataSeries;
import eu.webtoolkit.jwt.Side;
import eu.webtoolkit.jwt.Signal2;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WBrush;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WDialog;
import eu.webtoolkit.jwt.WLength;
import eu.webtoolkit.jwt.WText;

public class CovMapDialog extends StandardDialog{

	private ConsensusBucket bucket;
	private NgsConsensusSequenceModel model;

	public CovMapDialog(final ConsensusBucket bucket,
			final NgsConsensusSequenceModel model) {
		super("Coverage map: " + bucket.getConcludedName());
		this.bucket = bucket;
		this.model = model;

		getOkB().hide();
		getCancelB().setText("Close");
		getContents().addWidget(new WText("Creating coverage map please wait.."));
		final WApplication app = WApplication.getInstance();

		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					File samFile = model.samFile(bucket);
					File consensusWorkDir = samFile.getParentFile();
					File covFile = new File(consensusWorkDir, "aln.cov");

					if (!covFile.exists())
						SamtoolsUtil.samToCovMap(samFile, covFile, consensusWorkDir);
					UpdateLock updateLock = app.getUpdateLock();
					getContents().clear();
					paintChart(covFile, CovMapDialog.this);
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
	    samAnchor.setText("sequence-alignment.sam");
	    WAnchor bamAnchor = new WAnchor(model.bamLink(bucket));
	    bamAnchor.setText("sequence-alignment.bam");

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
}
