package com.dframework;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.dframework.foundation.components.DCloserTest;
import com.dframework.foundation.components.DFrameTestV4;

/**
 * A junit 4 version of my test suite - simply for practice.
 * 
 * @author dale.macdonald
 * 
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ 
	DFrameTestV4.class,
	DCloserTest.class //So old-style tests will work peachily
})
public class AllTestsV4 {

}






//=================================================================================

// http://stackoverflow.com/questions/457276/junit4-test-suites

// NB, classes with tests in NO LONGER need to extend TestCase


/**
 * Template 1
 * 
 */
//import org.junit.runners.Suite;
//import org.junit.runner.RunWith;
//
//@RunWith(Suite.class)
//@Suite.SuiteClasses({TestClass1.class, TestClass2.class})
//public class TestSuite {
//  //nothing
//}


/**
 * Template 2
 */
//package my.package.tests;
//
//@RunWith(Suite.class)
//@Suite.SuiteClasses({
//    testMyService.class,
//    testMyBackend.class,
//    ...
//})
//
//public class AllTests {}



//Now you can run this in a couple different ways:
//
//right-click and run in Eclipse as Junit test
//create a runable Java Application; Main class='org.junit.runner.JUnitCore' and Args='my.package.tests.AllTests'
//run from the command line:

// $ java -cp build/classes/:/usr/share/java/junit4.jar:/usr/share/java/hamcrest-core.jar org.junit.runner.JUnitCore my.package.tests.AllTests










