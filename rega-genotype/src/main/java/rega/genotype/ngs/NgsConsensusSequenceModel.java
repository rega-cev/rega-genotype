package rega.genotype.ngs;

import java.util.List;

import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.ngs.model.Contig;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WBrush;
import eu.webtoolkit.jwt.WColor;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WPen;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.chart.WChartPalette;
import eu.webtoolkit.jwt.chart.WStandardPalette;

/**
 * Display data for every assembled consensus sequence from ngs-results.xml  
 * 
 * @author michael
 */
public class NgsConsensusSequenceModel extends WAbstractTableModel {
	public static final int ASSINGMENT_COLUMN =    0;
	public static final int SEQUENCE_COUNT_COLUMN =1; // sequence count column. percentages of the chart.
	public static final int CHART_DISPLAY_COLUMN = 2;
	public static final int READ_COUNT_COLUMN =    3; 
	public static final int TOTAL_LENGTH_COLUMN =  4; // % of Genome
	public static final int DEEP_COV_COLUMN =      5;
	public static final int SRC_COLUMN =           6;
	public static final int COLOR_COLUMN =         7;
	public static final int IMAGE_COLUMN =         8;

	private WString[] headers = {
			tr("detailsForm.summary.assignment"),
			tr("detailsForm.summary.contig-count"),
			tr("----"),
			tr("detailsForm.summary.read-cunt"),
			tr("detailsForm.summary.total-len"),
			tr("detailsForm.summary.deep-cov"),
			tr("detailsForm.summary.src"),
			tr("detailsForm.summary.legend"),
			tr("detailsForm.summary.image")
			};
	private List<ConsensusBucket> buckets;
	private int readLength; // from qc.
	private WChartPalette palette;

	public NgsConsensusSequenceModel(List<ConsensusBucket> buckets, 
			int readLength, WChartPalette palette) {
		this.buckets = buckets;
		this.readLength = readLength;
		this.palette = palette;
	}

	@Override
	public Object getHeaderData(int section, Orientation orientation, int role) {
		if (role == ItemDataRole.DisplayRole && orientation == Orientation.Horizontal)
			return headers[section];

		return super.getHeaderData(section, orientation, role);
	}

	@Override
	public int getColumnCount(WModelIndex parent) {
		return headers.length;
	}

	@Override
	public int getRowCount(WModelIndex parent) {
		return buckets.size();
	}

	@Override
	public Object getData(WModelIndex index, int role) {
		if (role == ItemDataRole.DisplayRole) {
			ConsensusBucket bucket = buckets.get(index.getRow());
			switch (index.getColumn()) {
			case ASSINGMENT_COLUMN:
				return bucket.getConcludedName();
			case SEQUENCE_COUNT_COLUMN:
				return bucket.getContigs().size();
			case READ_COUNT_COLUMN: {
				return readCount(bucket);
			} case TOTAL_LENGTH_COLUMN:{
				return contigsLen(bucket) / bucket.getRefLen()  * 100;
			} case DEEP_COV_COLUMN: {
				return readCount(bucket) * (double)readLength / contigsLen(bucket);
			} case SRC_COLUMN :
				return bucket.getSrcDatabase();
			case COLOR_COLUMN:
				return "TODO";
			case IMAGE_COLUMN:
				return "TODO";
			}
		} else if (role == ItemDataRole.LinkRole) {
			if (index.getColumn() == ASSINGMENT_COLUMN
					|| index.getColumn() == SEQUENCE_COUNT_COLUMN) {
				// TODO
			}
		} else if (role == ItemDataRole.UserRole + 1) {
			if (index.getColumn() == COLOR_COLUMN)
				return palette.getBrush(index.getRow()).getColor();
		}
		return null;
	}

	private double contigsLen(ConsensusBucket bucket){
		double contigsLen = 0;
		for (Contig contig: bucket.getContigs()) 
			contigsLen += contig.getLength();
		return contigsLen;
	}

	private double readCount(ConsensusBucket bucket){
		double readCount = 0;
		for (Contig contig: bucket.getContigs()) 
			readCount += contig.getCov() * contig.getLength() / readLength;
		return readCount;
	}
}
