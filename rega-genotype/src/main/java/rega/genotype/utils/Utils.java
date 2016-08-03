package rega.genotype.utils;

import eu.webtoolkit.jwt.WWidget;

public class Utils {
	public static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	public static void removeSpellCheck(WWidget w) {
		w.setAttributeValue("autocomplete", "off");
		w.setAttributeValue("autocorrect", "off");
		w.setAttributeValue("autocapitalize", "off");
		w.setAttributeValue("spellcheck", "false");
	}
}