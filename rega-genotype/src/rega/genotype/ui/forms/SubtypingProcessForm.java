package rega.genotype.ui.forms;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WTable;
import net.sf.witty.wt.WText;

import org.jdom.Element;

import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.recombination.Table;
import rega.genotype.ui.util.GenotypeLib;

public class SubtypingProcessForm extends IForm {

	public SubtypingProcessForm(GenotypeWindow main) {
		super(main, "subtypingProcess-form");
		
		Element text = main.getResourceManager().getOrganismElement("subtypingProcess-form", "subtypingProcess-text");
		for(Object o : text.getChildren()) {
			final Element e = (Element)o;
			if(e.getName().equals("text")) {
				new WText(lt(getMain().getResourceManager().extractFormattedText(e)), this);
			} else if(e.getName().equals("header")) {
				new WText(lt(getMain().getResourceManager().extractFormattedText(e)), this);
			} else if(e.getName().equals("figure")) {
				WContainerWidget imgDiv = new WContainerWidget(this);
				imgDiv.setStyleClass("imgDiv");
				GenotypeLib.getWImageFromResource(getMain().getOrganismDefinition(),e.getTextTrim(), imgDiv);
			} else if(e.getName().equals("header")) {
				new WText(lt(getMain().getResourceManager().extractFormattedText(e)), this);
			} else if(e.getName().equals("table")) {
				createTable(e.getTextTrim(), this);
			}
			new WBreak(this);
		}
	}
	
	private WTable createTable(String csvFile, WContainerWidget parent) {
		Table csvTable = new Table(
				getClass().getClassLoader().getResourceAsStream(
						getMain().getOrganismDefinition().getOrganismDirectory()+csvFile
						), false);
		WTable table = new WTable(parent);

		for(int i = 0; i<csvTable.numRows(); i++) {
			for(int j = 0; j<csvTable.numColumns(); j++) {
				table.putElementAt(i, j, new WText(lt(csvTable.valueAt(j, i))));
			}
		}
		return table;
	}
}