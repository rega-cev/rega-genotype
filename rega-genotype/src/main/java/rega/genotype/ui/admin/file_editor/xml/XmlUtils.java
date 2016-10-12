package rega.genotype.ui.admin.file_editor.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Utility class to read write xml files
 * 
 * @author michael
 */
public class XmlUtils {
	public static final String STRINGS_XML_FILE_NAME = "strings.xml";
	public static final String VALUE_TYPE_ATTR = "user_value_type";

	public static enum ValueType {
		Xml, Fasta;
	
		public int getCode() {
			switch (this) {
			case Fasta: return 1;
			case Xml: return 0;
			default: return -1;
			}
		}

		public static ValueType fromCode(int code) {
			if (code == Fasta.getCode())
				return Fasta;
			else 
				return Xml;
		}
		
		public static ValueType fromString(String string, ValueType defaultType) {
			if (string == null)
				return defaultType;

			if (string.equals(Fasta.toString()))
				return Fasta;
			else
				return defaultType;
		}

	}
	public static class XmlMsgNode {
		public String id = new String();
		public String value = new String(); // string representation of all the node content (the user will edit that)
		public ValueType valueType = ValueType.Xml;
	}

	/**
	 * Read all root message elements, save string representation of all the node content (the user will edit that)
	 * @param xmlFile string.xml file
	 * @return Map<id, XmlMsgNode>
	 * @throws JDOMException
	 * @throws IOException
	 */
	public static Map<String, XmlMsgNode> readMessages(File xmlFile) throws JDOMException, IOException { 
		SAXBuilder builder = new SAXBuilder();
		Document document = builder.build(xmlFile);
		Element root = document.getRootElement();
		Map<String, XmlMsgNode> ans = new HashMap<String, XmlUtils.XmlMsgNode>();
		List<Element> messages = root.getChildren("message");

		XMLOutputter outp = new XMLOutputter();

		for (Element e: messages) {
			XmlMsgNode msgNode = new XmlMsgNode();
			msgNode.id = e.getAttributeValue("id");
			msgNode.valueType = ValueType.fromString(
					e.getAttributeValue(VALUE_TYPE_ATTR), ValueType.Xml);
			if (msgNode.valueType == ValueType.Fasta)
				msgNode.value = e.getText();
			else
				for (Object co: e.getChildren()){
					if (co instanceof Element) {
						Element c = (Element) co;
						msgNode.value += outp.outputString(c);
					}
				}
			ans.put(msgNode.id, msgNode);
		}

		return ans;
	}

	/**
	 * 
	 * @param xmlFile string.xml file
	 * @param messages Map<id, XmlMsgNode>
	 * @throws IOException
	 * @throws JDOMException
	 */
	public static void writeMessages(File xmlFile, Map<String, XmlMsgNode> messages) throws IOException, JDOMException {
		if (!xmlFile.exists())
			xmlFile.createNewFile();

		Element root = new Element("messages");
		Document document = new Document(root);
		document.setRootElement(root);

		root.removeChildren("message");

		for (XmlMsgNode m: messages.values()) {
			Element e = new Element("message");
			e.setAttribute("id", m.id);
			e.setAttribute(VALUE_TYPE_ATTR, m.valueType.toString());
			e.setText(m.value);
			root.addContent(e);
		}

		XMLOutputter xmlOutput = new XMLOutputter(){
			@Override
			public String escapeElementEntities(String arg0) {
				return arg0; // disable escaping.
			}
		};
		xmlOutput.setFormat(Format.getPrettyFormat());
		xmlOutput.output(document, new FileWriter(xmlFile));
	}
}
