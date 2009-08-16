/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import java.io.File;

import org.jdom.Element;

import rega.genotype.ui.framework.GenotypeWindow;
import rega.genotype.ui.util.GenotypeLib;
import rega.genotype.utils.Table;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WTable;
import eu.webtoolkit.jwt.WText;

/**
 * A documentation form reads its contents from the resource file.
 * 
 * DocumentationForm supports: headers, rules, figures, sequences, csv-tables and HTML-formatted text.
 */
public class DocumentationForm extends AbstractForm {
	public DocumentationForm(GenotypeWindow main, String formName, String formContent) {
		super(main, formName);
		
		fillForm(formName, formContent);
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
				header.setId("");
				header.setStyleClass("decisionTreeHeader");
			} else if(e.getName().equals("rule")){
				ruleNumber = e.getAttributeValue("number");
				ruleName = e.getAttributeValue("name");
				WText w = new WText(lt(ruleNumber + ": " + ruleName + "<br></br>" + getMain().getResourceManager().extractFormattedText(e) + "<br></br>"), this);
				w.setId("");
			} else if(e.getName().equals("figure")) {
				WContainerWidget imgDiv = new WContainerWidget(this);
				imgDiv.setId("");
				imgDiv.setStyleClass("imgDiv");
				GenotypeLib.getWImageFromResource(getMain().getOrganismDefinition(),e.getTextTrim(), imgDiv);
			} else if(e.getName().equals("sequence")) {
				String sequence = "<div class=\"sequenceName\">>" + e.getAttributeValue("name") +"<br/></div>";
				sequence += "<div class=\"sequence\">";
				sequence += e.getTextTrim() + "</div>";
				WText w = new WText(lt(sequence), this);
				w.setId("");
			} else if(e.getName().equals("table")) {
				createTable(e.getTextTrim(), this);
			} if(e.getName().equals("text")) {
				WText w = new WText(lt(getMain().getResourceManager().extractFormattedText(e)), this);
				w.setId("");
			}
		}
	}
	
	private WTable createTable(String csvFile, WContainerWidget parent) {
		Table csvTable = new Table(
				getClass().getResourceAsStream(
						getMain().getOrganismDefinition().getOrganismDirectory()+csvFile
						), false);
		WTable table = new WTable(parent);
		table.setId("");
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
