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
		String s = ">abcd description\n\nAC\nGT";
		//testSequences(s, "abcd", "ACGT");
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
