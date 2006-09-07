/* *******************************************************************************
 * Copyright (c) 2006 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * David Dykstal (IBM) - initial contribution.
 * *******************************************************************************/
package org.eclipse.rse.tests.framework.examples;

import junit.framework.TestSuite;

import org.eclipse.rse.tests.framework.ITestSuiteProvider;

public class MixedSuiteProvider implements ITestSuiteProvider {

	public TestSuite getSuite(String arg) {
		TestSuite suite = new TestSuite("Mixed Suite, arg = " + arg);
		if (arg == null || arg.equals("success")) {
			suite.addTest(TestSuite.createTest(SuccessTests.class, "testSuccess01"));
			suite.addTest(TestSuite.createTest(SuccessTests.class, "testSuccess02"));
			suite.addTest(TestSuite.createTest(SuccessTests.class, "testSuccess03"));
		}
		if (arg == null || arg.equals("failure")) {
			suite.addTest(TestSuite.createTest(FailureTests.class, "testFailure01"));
			suite.addTest(TestSuite.createTest(FailureTests.class, "testFailure02"));
			suite.addTest(TestSuite.createTest(FailureTests.class, "testFailure03"));
		}
		if (arg == null || arg.equals("error")) {
			suite.addTest(TestSuite.createTest(ErrorTests.class, "testError01"));
			suite.addTest(TestSuite.createTest(ErrorTests.class, "testError02"));
			suite.addTest(TestSuite.createTest(ErrorTests.class, "testError03"));
		}
		if (arg == null) {
			suite.addTest(TestSuite.createTest(SuccessTests.class, "testSuccess01"));
			suite.addTest(TestSuite.createTest(FailureTests.class, "testFailure01"));
			suite.addTest(TestSuite.createTest(ErrorTests.class, "testError01"));
			suite.addTest(TestSuite.createTest(SuccessTests.class, "testSuccess02"));
			suite.addTest(TestSuite.createTest(FailureTests.class, "testFailure02"));
			suite.addTest(TestSuite.createTest(ErrorTests.class, "testError02"));
			suite.addTest(TestSuite.createTest(SuccessTests.class, "testSuccess03"));
			suite.addTest(TestSuite.createTest(FailureTests.class, "testFailure03"));
			suite.addTest(TestSuite.createTest(ErrorTests.class, "testError03"));
		}
		return suite;
	}
	
}


