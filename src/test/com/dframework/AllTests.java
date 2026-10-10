package com.dframework;

import com.dframework.foundation.components.DCloserTest;
import com.dframework.foundation.components.DFrameTest;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.runner.Version;

/**
 * A TestSuite which will carry out all unit tests in sequence.
 * 
 * @author dale.macdonald
 *
 */
public class AllTests extends TestSuite {

	
	public static Test suite() {
		TestSuite suite= new TestSuite();
		suite.addTestSuite(DFrameTest.class);
		suite.addTestSuite(DCloserTest.class);
		//suite.addTestSuite(HealthStatusTest.class);
		return suite;
	}
	
	/** Simple way to report junit version */
	public static void main(String[] args) {
		System.out.println("JUnit version is: " + Version.id());
	}
}
