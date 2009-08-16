package rega.genotype.ui.util;

import eu.webtoolkit.jwt.Signal1;
import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WMouseEvent;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WText;

public class StateLink extends WAnchor {

	private String baseUrl;

	public StateLink(WString ws, String url, WContainerWidget parent) {
		super("", ws, parent);

		text().arg("");
		
		this.setStyleClass("non-link");
		this.baseUrl = url;
		this.setRefInternalPath(baseUrl);
	}
	
	public void setVarValue(String value) {
		/*
		 * Update link text
		 */
		text().args().clear();
		text().args().add(value);
		
		if (value.equals("")) {
			setRefInternalPath(baseUrl);
			this.setStyleClass("non-link");
		} else {
			setRefInternalPath(baseUrl + '/' + value);
			this.setStyleClass("link");
		}
		
		refresh();
	}
}
