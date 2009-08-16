package rega.genotype.ui;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class CsvGenerator extends DefaultHandler {
	//TODO
	//header('Content-type: application/ms-excell');
	
	//TODO check with incomplete xml files
	//TODO check with xml file with 1 uncomplete sequence
	
	private StringBuilder currentPath = new StringBuilder();

	private String elementToRecord = null;
	private StringBuilder values = new StringBuilder();
	
	private Map<String, String> valuesMap = new HashMap<String, String>();
	
	private PrintStream os;
	private boolean writtenFullSequence = false;
	
	public CsvGenerator(PrintStream os) {
		this.os = os;
	}
	
	public boolean isWrittenFullSequence() {
		return writtenFullSequence;
	}
	
	@Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		String specifier = "";
		String type = attributes.getValue("id");
		if(type!=null)
			specifier += '[' + type + ']';
		
		addToCurrentPath(qName + specifier);
		
		if(getCurrentPath().equals("genotype_result.sequence")) {
			valuesMap.put("genotype_result.sequence[name]", attributes.getValue("name"));
			valuesMap.put("genotype_result.sequence[length]", attributes.getValue("length"));
		}
		
		else if(getCurrentPath().equals("genotype_result.sequence.result[blast].start")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[blast].end")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[blast].cluster.name")) {
			elementToRecord = getCurrentPath();
		} 
		
		else if(getCurrentPath().equals("genotype_result.sequence.result[pure]")) {
			valuesMap.put("genotype_result.sequence.result[pure]", "true");
		}
		
		else if(getCurrentPath().equals("genotype_result.sequence.result[pure].best.id")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[pure].best.support")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[pure].best.inner")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[pure].best.outer")) {
			elementToRecord = getCurrentPath();
		} 
		
		else if(getCurrentPath().equals("genotype_result.sequence.result[purepuzzle].best.id")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[purepuzzle].best.support")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[purepuzzle].best.inner")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[purepuzzle].best.outer")) {
			elementToRecord = getCurrentPath();
		} 
		
		else if(getCurrentPath().equals("genotype_result.sequence.result[scan].support[assigned]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[scan].nosupport[assigned]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[scan].profile[assigned]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[scan].support[best]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[scan].nosupport[best]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[scan].profile[best]")) {
			elementToRecord = getCurrentPath();
		} 
		
		else if(getCurrentPath().equals("genotype_result.sequence.result[crfscan].support[assigned]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[crfscan].nosupport[assigned]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[crfscan].profile[assigned]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[crfscan].support[best]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[crfscan].nosupport[best]")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[crfscan].profile[best]")) {
			elementToRecord = getCurrentPath();
		}
		
		else if(getCurrentPath().equals("genotype_result.sequence.result[crf].best.id")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[crf].best.support")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[crf].best.inner")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.result[crf].best.outer")) {
			elementToRecord = getCurrentPath();
		} 
		
		else if(getCurrentPath().equals("genotype_result.sequence.conclusion")) {
			valuesMap.put("genotype_result.sequence.conclusion", "true");
		}
		
		else if(getCurrentPath().equals("genotype_result.sequence.conclusion.assigned.name")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.conclusion.assigned.support")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.conclusion.assigned.major.assigned.id")) {
			elementToRecord = getCurrentPath();
		} else if(getCurrentPath().equals("genotype_result.sequence.conclusion.assigned.minor.assigned.id")) {
			elementToRecord = getCurrentPath();
		}
     }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    	if(getCurrentPath().equals(elementToRecord)) {
    		this.valuesMap.put(elementToRecord, this.values.toString().trim());
    		this.values.delete(0, values.length());
    		elementToRecord = null;
    	}
    	
    	//System.err.println(getCurrentPath());
    	
    	if(getCurrentPath().equals("genotype_result.sequence")) {
    		endSequence();
    	}
    	removeFromCurrentPath();
    }
    
    private void endSequence() {
    	StringBuilder csvLine = new StringBuilder();
    	
    	if(!writtenFullSequence) {
    		os.append("name,length,assignment,support,begin,end,type,pure,pure_support,pure_inner,pure_outer,scan_best_support,scan_assigned_support,scan_assigned_nosupport,scan_best_profile,scan_assigned_profile,crf,crf_support,crf_inner,crf_outer,crfscan_best_support,crfscan_assigned_support,crfscan_assigned_nosupport,crfscan_best_profile,crfscan_assigned_profile,major_id,minor_id\n");
    	}
    	
    	csvLine.append(getCsvValue("genotype_result.sequence[name]")+",");
    	csvLine.append(getCsvValue("genotype_result.sequence[length]")+",");

    	if(valuesMap.get("genotype_result.sequence.conclusion")==null) {
    		csvLine.append("\"Sequence error\",");
    	} else {
    		csvLine.append(getCsvValue("genotype_result.sequence.conclusion.assigned.name")+",");
    		if(valuesMap.get("genotype_result.sequence.conclusion.assigned.support")!=null) {
    			csvLine.append(getCsvValue("genotype_result.sequence.conclusion.assigned.support"));
    		}
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[blast].start"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[blast].end"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[blast].cluster.name"));
    		
    		if(valuesMap.get("genotype_result.sequence.result[pure]")!=null) {
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[pure].best.id"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[pure].best.support"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[pure].best.inner"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[pure].best.outer"));
    		} else {
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[purepuzzle].best.id"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[purepuzzle].best.support"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[purepuzzle].best.inner"));
    			csvLine.append("," + getCsvValue("genotype_result.sequence.result[purepuzzle].best.outer"));
    		}
    		
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].support[assigned]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].support[best]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].nosupport[best]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].profile[assigned]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[scan].profile[best]"));
    		
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crf].best.id"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crf].best.support"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crf].best.inner"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crf].best.outer"));
    		
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].support[assigned]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].support[best]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].nosupport[best]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].profile[assigned]"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.result[crfscan].profile[best]"));
    		
    		csvLine.append("," + getCsvValue("genotype_result.sequence.conclusion.assigned.major.assigned.id"));
    		csvLine.append("," + getCsvValue("genotype_result.sequence.conclusion.assigned.minor.assigned.id"));
    	}
    	
    	os.append(csvLine.toString()+"\n");
    	
    	valuesMap.clear();
    	writtenFullSequence =true;
    }
    
    	                   
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    	if(elementToRecord!=null) {
    		values.append(new String(ch, start, length));
    	}
    }

    private String getCsvValue(String name) {
    	String val = this.valuesMap.get(name);
    	if(val==null)
    		val="";
    	return "\"" + val + "\"";
    }
    
    public void parse(InputSource source)  throws SAXException, IOException {
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(this);
        xmlReader.setErrorHandler(this);
        xmlReader.parse(source);
    }
    
    private String getCurrentPath() {
    	return currentPath.toString();
    }
    
    private void addToCurrentPath(String p) {
    	if(currentPath.length()>0)
    		currentPath.append('.');
    	currentPath.append(p);
    }
    
    private void removeFromCurrentPath() {
    	int lastDot = currentPath.lastIndexOf(".");
    	if(lastDot==-1)
    		lastDot = 0;
    	currentPath.delete(lastDot, currentPath.length());
    }
    
    public static void main(String [] args) {
    	CsvGenerator csvgen;
		try {
			FileOutputStream fos = new FileOutputStream("/home/plibin0/projects/utrecht/genotype/result.csv");
			csvgen = new CsvGenerator(new PrintStream(fos));
			csvgen.parse(new InputSource(new FileInputStream("/home/plibin0/projects/utrecht/genotype/result.xml")));
			fos.flush();
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
