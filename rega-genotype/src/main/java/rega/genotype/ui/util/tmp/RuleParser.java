/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.util.tmp;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

import rega.genotype.ui.util.GenotypeLib;

/**
 * Transforms flat rules file into an xml format.
 * 
 * @author simbre1
 *
 */
public class RuleParser {
	public static void main(String [] args) throws IOException {
		String s = GenotypeLib.readFileToString(new File("/home/plibin0/projects/subtypetool/rules.txt"));
		StringTokenizer t = new StringTokenizer(s, "\n");
		int i = 0;
		while(t.hasMoreTokens()) {
			String token = t.nextToken();
			token = token.replace("&", "&amp;");
			token = token.replace("<", "&lt;");
			token = token.replace(">", "&gt;");
			
			if(i%2==0) {
				String [] split = token.split(":");
				System.out.println("<rule number=\""+split[0].trim()+"\" name=\""+split[1].trim()+"\">");
			} else {
				System.out.println(token);
				System.out.println("</rule>");
			}
			i++;
		}
	}
}
