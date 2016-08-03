/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.forms;

import rega.genotype.ui.framework.GenotypeWindow;
import eu.webtoolkit.jwt.WContainerWidget;
import eu.webtoolkit.jwt.WText;

/**
 * AbstractForm is the abstract base class for one of the main forms in the rega-genotype UI.
 * 
 * Every form has a 'title' which is used to retrieve the actual title from the resource file,
 * and unless a specific CSS class is given, also to set the CSS class.
 */
public abstract class AbstractForm extends WContainerWidget {
	private GenotypeWindow main;

	public AbstractForm(GenotypeWindow main) {
		this.main = main;
		
		setStyleClass("genotype-div");
	}
	
	public GenotypeWindow getMain() {
		return main;
	}
	
	public abstract void handleInternalPath(String internalPath);
}
