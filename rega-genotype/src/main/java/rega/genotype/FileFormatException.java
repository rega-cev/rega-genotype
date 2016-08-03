/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;

/**
 * Exception thrown when an error was encountered reading a file
 * 
 * @author koen
 */
public class FileFormatException extends Exception {
    private static final long serialVersionUID = -3203555163977284783L;
    public FileFormatException(String errorMessage, int lineNumber) {
        super("Error parsing input at line " + lineNumber + ": " + errorMessage);
    }
}
