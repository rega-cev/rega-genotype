package rega.genotype.ui.forms;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import net.sf.witty.wt.WBreak;
import net.sf.witty.wt.WContainerWidget;
import net.sf.witty.wt.WImage;
import net.sf.witty.wt.WResource;
import net.sf.witty.wt.WText;

import org.apache.commons.io.IOUtils;
import org.jdom.Element;

import rega.genotype.ui.framework.GenotypeWindow;

public class TutorialForm extends IForm {
	public TutorialForm(GenotypeWindow main) {
		super(main, "tutorial-form");
		
		Element text = main.getResourceManager().getOrganismElement("tutorial-form", "tutorial-text");
		for(Object o : text.getChildren()) {
			final Element e = (Element)o;
			if(e.getName().equals("text")) {
				new WText(lt(getMain().getResourceManager().extractFormattedText(e)), this);
			} else if(e.getName().equals("figure")) {
				WContainerWidget imgDiv = new WContainerWidget(this);
				imgDiv.setStyleClass("imgDiv");
				new WImage(new WResource() {
		            @Override
		            public String resourceMimeType() {
		                return "image/gif";
		            }
		            @Override
		            protected void streamResourceData(OutputStream stream) {
		                try {
		                    IOUtils.copy(this.getClass().getClassLoader().getResourceAsStream(getMain().getOrganismDefinition().getOrganismDirectory()+File.separatorChar+e.getTextTrim()), stream);
		                } catch (IOException e) {
		                    e.printStackTrace();
		                }
		            }
		        }, imgDiv);
			}
			new WBreak(this);
		}
	}
}
