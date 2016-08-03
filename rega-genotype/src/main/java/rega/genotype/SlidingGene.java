/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype;
import java.util.ArrayList;
import java.util.List;

/*
 * Utility class to create a list of sliding windows used by ScanAnalysis
 */
public class SlidingGene {
    static List<SequenceAlignment> generateSlidingWindow(SequenceAlignment alignment, int windowSize, int step) {
        int alignmentLength = alignment.getLength();
        List<SequenceAlignment> result = new ArrayList<SequenceAlignment>();
        
        int i = 0;
        for (i = 0; i < alignmentLength - windowSize; i += step) {
            result.add(alignment.getSubSequence(i, i + windowSize));
        }

        result.add(alignment.getSubSequence(i, alignmentLength));
        
        return result;
    }
}
