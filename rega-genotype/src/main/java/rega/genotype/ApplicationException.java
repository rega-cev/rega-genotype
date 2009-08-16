/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

/**
 * Internal exception
 */
public class ApplicationException extends Exception {
    private static final long serialVersionUID = -4157791298795785833L;

    public ApplicationException(String msg) {
		super(msg);
	}
}
