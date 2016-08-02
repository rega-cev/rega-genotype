/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

/**
 * Exception thrown when a method is used with an illegal parameter, indicating a programming
 * error.
 * 
 * @author koen
 */
public class ParameterProblemException extends Exception {
    private static final long serialVersionUID = 8445944358674547232L;

    public ParameterProblemException(String message) {
        super(message);
    }
}
