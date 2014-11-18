package edu.vt.qav.tests.listeners;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;


/*
 * This is the base listener.
 * TODO: link to screenshot/email functionality in RemoteTest
 * TODO: check testng dox for inclusion w/o testng.xml
 * 
 * @author Brian Long
 */
public class BaseTestListener implements ITestListener {
	@Override
	public void onFinish(ITestContext arg0) {
	    System.out.println("onFinish()");
	    System.out.println(arg0.toString());
		
	}

	@Override
	public void onStart(ITestContext arg0) {
	    System.out.println("onStart()");
	    System.out.println(arg0.toString());
		
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult arg0) {
	    System.out.println("onTestFailedButWithinSuccessPercentage()");
	    System.out.println(arg0.toString());
		
	}

	@Override
	public void onTestFailure(ITestResult arg0) {
	    System.out.println("onTestFailure()");
	    System.out.println(arg0.toString());
	}

	@Override
	public void onTestSkipped(ITestResult arg0) {
	    System.out.println("onTestSkipped()");

	}

	@Override
	public void onTestStart(ITestResult arg0) {
	    System.out.println("onTestStart()");
	    System.out.println(arg0.toString());
		
	}

	@Override
	public void onTestSuccess(ITestResult arg0) {
	    System.out.println("onTestSuccess()");
	    System.out.println(arg0.toString());
		
	}
}
