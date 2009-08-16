package rega.genotype.ui.i18n.resources;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.sf.witty.wt.WWidget;
import net.sf.witty.wt.i8n.IWMessageResource;
import net.sf.witty.wt.i8n.WMessage;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class GenotypeResourceManager implements IWMessageResource {
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
		try {
			doc = builder.build(this.getClass().getResourceAsStream(xmlName));
			Element root = doc.getRootElement();
			for(Object o : root.getChildren()) {
				Element e = (Element)o;
				if(e.getName().equals("resource")) {
					resources.put(e.getAttributeValue("name"), e.getTextTrim());
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
    
	public WMessage getCommonValue(String form, String item) {
		return WWidget.lt(common.getChild(form).getChild(item).getTextTrim());
	}
	
	public WMessage getOrganismValue(String form, String item) {
		return WWidget.lt(organism.getChild(form).getChild(item).getTextTrim());
	}
	
	public String getValue(WMessage message) {
		return resources.get(message.key());
	}

	public void refresh() {
		common = processDoc(commonXml);
		organism = processDoc(organismXml);
	}
}
