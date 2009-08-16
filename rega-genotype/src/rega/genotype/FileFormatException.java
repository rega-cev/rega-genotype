package rega.genotype;

/**
 * A file format exception was encountered during reading a file
 */
public class FileFormatException extends Exception {
    private static final long serialVersionUID = -3203555163977284783L;
    public FileFormatException(String errorMessage, int lineNumber) {
        super("Error parsing input at line " + lineNumber + ": " + errorMessage);
    }
}
