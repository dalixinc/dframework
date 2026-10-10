package com.dframework.foundation.components;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * A junit 4 implementation of tests, just for the record.
 * 
 * @author dale.macdonald
 *
 */
public class DFrameTestV4 {

	@Before
	public void setUp() {
		
	}
	
	@After
	public void tearDown() {
		
	}
	
	@Test
	public void one() {
		assertEquals("1", "1");
	}
	
	@Ignore
	public void two() {
		assertEquals("2", "1");
	}
}
