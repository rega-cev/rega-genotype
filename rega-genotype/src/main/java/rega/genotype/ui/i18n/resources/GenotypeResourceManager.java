/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.i18n.resources;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;

import eu.webtoolkit.jwt.WLocalizedStrings;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WWidget;
import eu.webtoolkit.jwt.utils.StringUtils;

public class GenotypeResourceManager extends WLocalizedStrings {
	private Map<String, String> resources = new HashMap<String, String>();
	
	private String commonXml;
	private String organismXml;
	
	private Element common;
	private Element organism;
	
	public GenotypeResourceManager(String commonXml, String organismXml) {
		this.commonXml = commonXml;
		this.organismXml = organismXml;
		
		refresh();
	}
	
	private Element processDoc(String xmlName) {
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		String name;
		String value;
		try {
			doc = builder.build(this.getClass().getResourceAsStream(xmlName));
			Element root = doc.getRootElement();
			for(Object o : root.getChildren()) {
				Element e = (Element)o;
				if(e.getName().equals("resource")) {
					name = e.getAttributeValue("name");
					value = extractFormattedText(e);
					resources.put(name, value);
				}
			}
			return root;
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
    
	public WString getCommonValue(String form, String item) {
		return WWidget.lt(extractFormattedText(common.getChild(form).getChild(item)));
	}
	
	public WString getOrganismValue(String form, String item) {
		return WWidget.lt(extractFormattedText(organism.getChild(form).getChild(item)));
	}
	
	public WString getOrganismValue(String form, String item, List<String> args) {
		String value = extractFormattedText(organism.getChild(form).getChild(item));
		
		for(int i = 0; i<args.size(); i++) {
			value = value.replace("{"+(i+1)+"}", args.get(i));
		}
		
		return WWidget.lt(value);
	}
	
	public Element getOrganismElement(String form, String item) {
		return organism.getChild(form).getChild(item);
	}
	
	public String extractFormattedText(Element child) {
		StringBuilder textToReturn = new StringBuilder();
		extractFormattedText(textToReturn, child);
		String result = textToReturn.toString().trim();
		if (result.charAt(result.length()-1)==':')
			result += ' ';
		return result;
	}
	
	private void extractFormattedText(StringBuilder textToReturn, Element child) {
		for(Object o : child.getContent()) {
			if(o instanceof Text) {
				textToReturn.append(StringUtils.escapeText(((Text)o).getText(), false));
			} else {
				Element e = (Element)o;
				textToReturn.append("<"+e.getName());
				for(Object oa : e.getAttributes()) {
					Attribute a = (Attribute)oa;
					textToReturn.append(" "+ a.getName() + "=\"" + a.getValue() + "\"");
				}
				textToReturn.append(">");
				extractFormattedText(textToReturn, e);
				textToReturn.append("</"+e.getName()+">");
			}
		}
	}

	public void refresh() {
		common = processDoc(commonXml);
		organism = processDoc(organismXml);
	}

	@Override
	public String resolveKey(String key) {
		String v = resources.get(key);
		if (v == null)
			return "??" + key + "??";
		else
			return v;
	}
}
