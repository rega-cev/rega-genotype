/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.util;

import eu.webtoolkit.jwt.WAnchor;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WString;

/**
 * An toggle-able anchor widget with a single parameter.
 * 
 * @author simbre1
 *
 */
public class StateLink extends WAnchor {

	private String baseUrl, internalPath;

	public StateLink(WString ws, String url, WContainerWidget parent) {
		super("", ws, parent);

		getText().arg("");
		
		this.setStyleClass("non-link");
		this.baseUrl = this.internalPath = url;
		this.setRefInternalPath(baseUrl);
	}
	
	public void setVarValue(String value) {
		/*
		 * Update link text
		 */
		getText().getArgs().clear();
		getText().getArgs().add(value);
		
		if (value.equals("")) {
			setRefInternalPath(this.internalPath = baseUrl);
			this.setStyleClass("non-link");
		} else {
			setRefInternalPath(this.internalPath = baseUrl + '/' + value);
			this.setStyleClass("link");
		}
		
		refresh();
	}

	public String internalPath() {
		return internalPath;
	}
}
