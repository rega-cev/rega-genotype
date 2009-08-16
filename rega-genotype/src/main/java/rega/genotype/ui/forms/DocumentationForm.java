/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;

import org.jdom.Element;

import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.recombination.Table;
import rega.genotype.ui.util.GenotypeLib;
import eu.webtoolkit.jwt.WBreak;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;

/**
 * Base documentationform widget which parses the resources.xml entries.
 * DocumentationForm supports: headers, rules, figures, sequences, csv-tables and HTML-formatted text.
 */
public class DocumentationForm extends AbstractForm {
	public DocumentationForm(GenotypeWindow main, String title) {
		super(main, title);
	}

	protected void fillForm(String formName, String formContent) {
		String ruleNumber;
		String ruleName;
		int headerNr=0;
		
		Element text = getMain().getResourceManager().getOrganismElement(formName, formContent);
		for(Object o : text.getChildren()) {
			final Element e = (Element)o;
			if(e.getName().equals("header")) {
				WText header = new WText(lt((++headerNr) + ". " + getMain().getResourceManager().extractFormattedText(e) +":"), this);
				header.setStyleClass("decisionTreeHeader");
			} else if(e.getName().equals("rule")){
				ruleNumber = e.getAttributeValue("number");
				ruleName = e.getAttributeValue("name");
				new WText(lt(ruleNumber + ": " + ruleName + "<br></br>" + getMain().getResourceManager().extractFormattedText(e) + "<br></br>"), this);
			} else if(e.getName().equals("figure")) {
				WContainerWidget imgDiv = new WContainerWidget(this);
				imgDiv.setStyleClass("imgDiv");
				GenotypeLib.getWImageFromResource(getMain().getOrganismDefinition(),e.getTextTrim(), imgDiv);
			} else if(e.getName().equals("sequence")) {
				String sequence = "<div class=\"sequenceName\">>" + e.getAttributeValue("name") +"</div>";
				sequence += "<div class=\"sequence\">";
				sequence += e.getTextTrim() + "</div>";
				new WText(lt(sequence), this);
			} else if(e.getName().equals("table")) {
				createTable(e.getTextTrim(), this);
			} if(e.getName().equals("text")) {
				new WText(lt(getMain().getResourceManager().extractFormattedText(e)), this);
			}
		}
	}
	
	private WTable createTable(String csvFile, WContainerWidget parent) {
		Table csvTable = new Table(
				getClass().getResourceAsStream(
						getMain().getOrganismDefinition().getOrganismDirectory()+csvFile
						), false);
		WTable table = new WTable(parent);
		table.setStyleClass(getCssClass(csvFile));

		for(int i = 0; i<csvTable.numRows(); i++) {
			for(int j = 0; j<csvTable.numColumns(); j++) {
				table.elementAt(i, j).addWidget(new WText(lt(csvTable.valueAt(j, i))));
			}
		}
		return table;
	}
	
	private static String getCssClass(String path){
		return path.replace(File.separatorChar, '_').replace('.', '-');
	}
}
