package rega.genotype;

import java.io.IOException;
import java.io.StringBufferInputStream;

import junit.framework.TestCase;

public class Sequences extends TestCase {

	public Sequences(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testFastaRead1() {
		String s = ">abcd\nACGT";
		testSequence(s, "abcd", "ACGT");
	}

	public void testFastaRead2() {
		String s = ">abcd\n\nAC\nGT";
		testSequence(s, "abcd", "ACGT");
	}

	public void testFastaRead3() {
		String s = ">abcd description\n\nAC\nGT";
		testSequence(s, "abcd", "ACGT");
	}

	public void testFastaReadMultiple1() {
		String s1 = ">abcd description\n\nAC\nGT";
		String s2 = ">efgh description2\nACGTACGTACGT\nGT";
		testSequences(s1, s2, "abcd", "ACGT", "efgh", "ACGTACGTACGTGT");
	}

	private void testSequences(String s1, String s2, String name1,
			String seq1, String name2, String seq2) {
		SequenceAlignment a;
		try {
			a = new SequenceAlignment(new StringBufferInputStream(s1 + '\n' + s2),
					SequenceAlignment.FILETYPE_FASTA);			
			assertEquals(2, a.getSequences().size());
			assertEquals(seq1, a.getSequences().get(0).getSequence());
			assertEquals(name1, a.getSequences().get(0).getName());
			assertEquals(seq2, a.getSequences().get(1).getSequence());
			assertEquals(name2, a.getSequences().get(1).getName());
		} catch (ParameterProblemException e) {
			fail("ParameterProblemException");
		} catch (IOException e) {
			fail("IOException");
		} catch (FileFormatException e) {
			fail("FileFormatException");
		}		
	}

	private void testSequence(String s, String name, String seq) {
		SequenceAlignment a;
		try {
			a = new SequenceAlignment(new StringBufferInputStream(s),
					SequenceAlignment.FILETYPE_FASTA);			
			assertEquals(a.getSequences().size(), 1);
			assertEquals(a.getSequences().get(0).getSequence(), seq);
			assertEquals(a.getSequences().get(0).getName(), name);
		} catch (ParameterProblemException e) {
			fail("ParameterProblemException");
		} catch (IOException e) {
			fail("IOException");
		} catch (FileFormatException e) {
			fail("FileFormatException");
		}
	}
}
