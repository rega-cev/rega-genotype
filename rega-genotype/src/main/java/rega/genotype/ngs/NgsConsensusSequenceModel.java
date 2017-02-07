package rega.genotype.ngs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import rega.genotype.ApplicationException;
import rega.genotype.config.Config.ToolConfig;
import rega.genotype.ngs.model.ConsensusBucket;
import rega.genotype.tools.blast.BlastJobOverviewForm;
import rega.genotype.utils.SamtoolsUtil;
import eu.webtoolkit.jwt.AnchorTarget;
import eu.webtoolkit.jwt.ItemDataRole;
import eu.webtoolkit.jwt.Orientation;
import eu.webtoolkit.jwt.WAbstractTableModel;
import eu.webtoolkit.jwt.WLink;
import eu.webtoolkit.jwt.WModelIndex;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.chart.WChartPalette;
import eu.webtoolkit.jwt.servlet.WebRequest;
import eu.webtoolkit.jwt.servlet.WebResponse;
import eu.webtoolkit.jwt.utils.StreamUtils;

/**
 * Display data for every assembled consensus sequence from ngs-results.xml  
 * 
 * @author michael
 */
public class NgsConsensusSequenceModel extends WAbstractTableModel {
	public static final int ASSINGMENT_COLUMN =    0;
	public static final int SEQUENCE_COUNT_COLUMN =1; // sequence count column. percentages of the chart.
	//public static final int CHART_DISPLAY_COLUMN = 2;
	public static final int READ_COUNT_COLUMN =    2; 
	public static final int TOTAL_LENGTH_COLUMN =  3; // % of Genome
	public static final int DEEP_COV_COLUMN =      4;
	public static final int SRC_COLUMN =           5;
	public static final int COLOR_COLUMN =         6;
	public static final int IMAGE_COLUMN =         7;
	public static final int DETAILS_COLUMN =       8;

	private WString[] headers = {
			tr("detailsForm.summary.assignment"),
			tr("detailsForm.summary.contig-count"),
			tr("detailsForm.summary.read-cunt"),
			tr("detailsForm.summary.total-len"),
			tr("detailsForm.summary.deep-cov"),
			tr("detailsForm.summary.src"),
			tr("detailsForm.summary.legend"),
			tr("detailsForm.summary.image"),
			tr("detailsForm.summary.details")
			};
	private List<ConsensusBucket> buckets;
	private int readLength; // from qc.
	private WChartPalette palette;
	private ToolConfig toolConfig;
	private File jobDir;
	private String pe1Name;
	private String pe2Name;

	public NgsConsensusSequenceModel(List<ConsensusBucket> buckets, 
			int readLength, WChartPalette palette, ToolConfig toolConfig, File jobDir,
			String pe1Name, String pe2Name) {
		this.buckets = buckets;
		this.readLength = readLength;
		this.palette = palette;
		this.toolConfig = toolConfig;
		this.jobDir = jobDir;
		this.pe1Name = pe1Name;
		this.pe2Name = pe2Name;
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
			final ConsensusBucket bucket = buckets.get(index.getRow());
			switch (index.getColumn()) {
			case ASSINGMENT_COLUMN:
				return bucket.getConcludedName();
			case SEQUENCE_COUNT_COLUMN:
				return bucket.getContigs().size();
			case READ_COUNT_COLUMN: {
				return bucket.getTotalReadCount(readLength);
			} case TOTAL_LENGTH_COLUMN:{
				return bucket.getCovPercentage();
			} case DEEP_COV_COLUMN: {
				return bucket.getDeepCov(readLength);
			} case SRC_COLUMN :
				return bucket.getSrcDatabase();
			case COLOR_COLUMN:
				return "";
			case IMAGE_COLUMN:
				return "";
			case DETAILS_COLUMN:
				return "sam";
			}
		} else if (role == ItemDataRole.LinkRole) {
			ConsensusBucket bucket = buckets.get(index.getRow());
			if (index.getColumn() == ASSINGMENT_COLUMN
					|| index.getColumn() == SEQUENCE_COUNT_COLUMN) {
				return BlastJobOverviewForm.createToolLink(bucket.getConcludedTaxonomyId(),
						jobDir.getName(), toolConfig);
			}
		} else if (role == ItemDataRole.UserRole + 1) {
			if (index.getColumn() == COLOR_COLUMN)
				return palette.getBrush(index.getRow()).getColor();
		}
		return null;
	}

	public File samFile(ConsensusBucket bucket) throws ApplicationException {
		File samFile = SamtoolsUtil.samFile(bucket, jobDir);
		if (!samFile.exists())
			samFile = createSamFile(bucket);
			
		return samFile;
	}

	public File createSamFile(final ConsensusBucket bucket) throws ApplicationException {
		return SamtoolsUtil.createSamFile(bucket, jobDir, pe1Name, pe2Name);
	}
	
	public WLink samLink(final ConsensusBucket bucket) {
		WResource r = new WResource() {
			
			@Override
			protected void handleRequest(WebRequest request, WebResponse response)
					throws IOException {
				File samFile;
				try {
					samFile = createSamFile(bucket);
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

	public ConsensusBucket getBucket(int row) {
		return buckets.get(row);
	}

	public WLink bamLink(final ConsensusBucket bucket) {
		WResource r = new WResource() {
			@Override
			protected void handleRequest(WebRequest request, WebResponse response)
					throws IOException {
				File bam = NgsFileSystem.bamSortedFile(jobDir,
						bucket.getDiamondBucket(), bucket.getRefName());

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
}
