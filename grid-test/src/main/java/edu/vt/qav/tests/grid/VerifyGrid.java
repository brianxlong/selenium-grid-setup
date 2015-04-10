package edu.vt.qav.tests.grid;

import org.openqa.selenium.WebDriver;
import org.testng.annotations.Test;

/*
 * This test just verifies that the grid is up and everything is connected.
 * 
 * All available platforms and browsers will be attempted.  We catch an
 * exception if WebDriver creation fails.  At the end we will have a set of
 * WebDrivers that represents all registered nodes and browsers.
 * 
 * We will navigate to the QAV page for 10 seconds on each platform/browser
 * combination, and then go away.
 * 
 * @author Brian Long
 */
public class VerifyGrid extends RemoteTest {

    @Test(dataProvider = "webDriverList", threadPoolSize = 20, groups = "grid")
    private void GridTest(WebDriver wda) {
        baseUrl = "http://www.seti.it.vt.edu/QAV/index.html";
        wda.get(baseUrl);
        try {
            Thread.sleep(15000); // show our webpage for a few seconds then go
                                 // away
        } catch (InterruptedException ie) {
            // carry on
        }
    }
}
