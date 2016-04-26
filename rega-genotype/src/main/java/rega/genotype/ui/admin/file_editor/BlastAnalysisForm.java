package rega.genotype.ui.admin.file_editor;

import java.util.Arrays;
import java.util.List;

import rega.genotype.AlignmentAnalyses.Cluster;
import rega.genotype.BlastAnalysis;
import rega.genotype.SequenceAlignment;
import rega.genotype.ui.framework.widgets.FormTemplate;
import rega.genotype.ui.framework.widgets.ObjectListComboBox;
import rega.genotype.utils.Utils;
import eu.webtoolkit.jwt.WComboBox;
import eu.webtoolkit.jwt.WLineEdit;
import eu.webtoolkit.jwt.WString;

/**
 * Edit analysis area of blast.xml
 * 
 * @author michael
 */
public class BlastAnalysisForm extends FormTemplate{
	private WLineEdit optionLE = new WLineEdit();
	private WComboBox sequenceTypeCB;
	private WLineEdit absCutOffLE = new WLineEdit();
	private WLineEdit absEValueLE = new WLineEdit();
	private WLineEdit absPValueLE = new WLineEdit();
	private WLineEdit relativeCutOffLE = new WLineEdit();
	private WLineEdit relativeEValueLE = new WLineEdit();
	private WLineEdit relativePValueLE = new WLineEdit();

	private BlastAnalysis analysis;

	enum SequenceType {DNA, AA}

	public BlastAnalysisForm(BlastAnalysis analysis) {
		super(tr("admin.analisysis-template"));
		this.analysis = analysis;

		List<SequenceType> list = Arrays.asList(SequenceType.values());
		sequenceTypeCB = new ObjectListComboBox<SequenceType>(list) {
			@Override
			protected WString render(SequenceType t) {
				return new WString(t.name());
			}
		};

		// set values 
		optionLE.setText(Utils.nullToEmpty(analysis.getOptions()));

		if (analysis.getOwner().getAlignment() != null)
			switch (analysis.getOwner().getAlignment().getSequenceType()) {
			case SequenceAlignment.SEQUENCE_ANY:
			case SequenceAlignment.SEQUENCE_DNA:
				sequenceTypeCB.setCurrentIndex(0);
				break;
			case SequenceAlignment.SEQUENCE_AA:
				sequenceTypeCB.setCurrentIndex(1);
			}
		else
			sequenceTypeCB.setCurrentIndex(0);

		bindWidget("option", optionLE);
		bindWidget("sequence-type", sequenceTypeCB);
		bindWidget("absolute-cut-off", absCutOffLE);
		bindWidget("absolute-evalue", absEValueLE);
		bindWidget("absolute-pvalue", absPValueLE);
		bindWidget("relative-cut-off", relativeCutOffLE);
		bindWidget("relative-evalue", relativeEValueLE);
		bindWidget("relative-pvalue", relativePValueLE);

		initInfoFields();
	}

	public boolean save() {
		if (validate()) {
			String identify = "";
			for(Cluster cluster: analysis.getOwner().getAllClusters()){
				identify += cluster.getId();
			}
			analysis.setId(identify);
			analysis.setOptions(optionLE.getText());
			int type = sequenceTypeCB.getCurrentIndex() + 1;
			analysis.getOwner().getAlignment().setSequenceType(type);
			return true;
		} else 
			return false;
	}
}
