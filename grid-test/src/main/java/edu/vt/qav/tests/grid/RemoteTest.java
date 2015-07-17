package edu.vt.qav.tests.grid;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.log4testng.Logger;

/*
 * Performs all the setup tasks for the Selenium Grid:
 * 
 * Connects to the hub.
 * Creates augmented WebDrivers for screenshots.
 * Reads testng-grid.xml for list of desired operating systems.
 * Sends email.
 * 
 * We catch an exception if WebDriver creation fails.  
 * At the end we will have a set of WebDrivers that represents all 
 * registered nodes and browsers, further filtered by testng-grid.xml
 * to narrow down the target set, if desired.
 * 
 * q.v. "VerifyGrid.java" for the test that actually attempts to use
 * the webDriverList.
 * 
 * @author Brian Long
 */
public class RemoteTest {

    protected String                 TEST_CLASS_NAME = this.getClass()
                                                             .getCanonicalName();
    protected Logger                 logger          = Logger.getLogger(this
                                                             .getClass());
    protected boolean                acceptNextAlert = true;
    protected static List<WebDriver> webDriverList   = new ArrayList<WebDriver>();
    protected String                 verificationErrorString;
    protected String                 baseUrl;

    @BeforeSuite
    @Parameters({ "hub", "oslist", "browserlist", "versionlist" })
    // defined in resources/testng-grid.xml
    public void setUp(String hub, String oslist, String browserlist,
            String versionlist) {

        StringTokenizer st = null;

        // why isn't there an iterator for DesiredCapabilities?
        Set<DesiredCapabilities> browserList = new HashSet<DesiredCapabilities>();
        browserList.add(DesiredCapabilities.android());
        browserList.add(DesiredCapabilities.chrome());
        browserList.add(DesiredCapabilities.firefox());
        browserList.add(DesiredCapabilities.internetExplorer());
        browserList.add(DesiredCapabilities.ipad());
        browserList.add(DesiredCapabilities.iphone());
        browserList.add(DesiredCapabilities.safari());

        System.out.println("oslist:      " + oslist);
        System.out.println("browserList: " + browserlist);
        System.out.println("versionlist: " + versionlist);

        // get intersection of all platforms with those requested
        Set<String> platformNames = new HashSet<String>();
        for (Platform p : Platform.values()) {
            System.out.println("add platform " + p.name());
            platformNames.add(p.name());
        }
        System.out.println("platformNames.size " + platformNames.size());

        Set<String> configPlatforms = new HashSet<String>();
        st = new StringTokenizer(oslist, ",");
        while (st.hasMoreTokens()) {
            configPlatforms.add(st.nextToken());
        }
        System.out.println("configPlatforms.size " + configPlatforms.size());
        platformNames.retainAll(configPlatforms);

        for (Iterator<String> i = platformNames.iterator(); i.hasNext();) {
            System.out.println("Keeping platform: " + i.next());
        }
        System.out.println("platformNames.size " + platformNames.size());

        // get browser list
        Set<String> configBrowsers = new HashSet<String>();
        st = new StringTokenizer(browserlist, ",");
        while (st.hasMoreTokens()) {
            configBrowsers.add(st.nextToken());
        }

        System.out.println("configBrowsers.size " + configBrowsers.size());
        List<DesiredCapabilities> killBrowsers = new ArrayList<DesiredCapabilities>();
        for (DesiredCapabilities browser : browserList) {
            if (!configBrowsers.contains(browser.getBrowserName())) {
                System.out.println("remove unneeded browser "
                        + (browser.getBrowserName()));
                killBrowsers.add(browser);
            } else {
                System.out.println("keeping browser "
                        + browser.getBrowserName());
            }
        }
        browserList.removeAll(killBrowsers);
        System.out.println("browserList.size " + browserList.size());

        for (String platform : platformNames) {
            System.out.println("Platform = " + platform);
            for (DesiredCapabilities browser : browserList) {
                DesiredCapabilities capability1 = browser;
                try { // try to create a webdriver on every browser in
                      // browserlist
                    capability1.setPlatform(Platform.valueOf(platform));
                    if ((versionlist.length() > 0) && (platform.equals("MAC"))) { // deprecated
                        System.out.println("got versions");
                        st = new StringTokenizer(versionlist, ",");
                        while (st.hasMoreTokens()) {
                            capability1.setVersion(st.nextToken());
                            createRemoteWebDriver(hub, capability1, true);
                        }
                    } else {
                        System.out.println("got no versions for platform");
                        createRemoteWebDriver(hub, capability1, true);
                    }
                } catch (org.openqa.selenium.WebDriverException wde) {
                    System.out.println("No node found with capability "
                            + capability1.toString());
                    // wde.printStackTrace();
                }
            }
        }
        System.out.println("RemoteTest::setUp(): " + webDriverList.size()
                + " clients.");
        if (webDriverList.size() == 0) {
            System.out.println("No items in WebDriverList!");
        }
    }

    protected WebDriver createRemoteWebDriver(String hub,
            DesiredCapabilities capability1, boolean addToWDList) {
        System.out
                .println("Attempting to create RemoteWebDriver with capability "
                        + capability1.toString());
        WebDriver wd = null;
        try {
            System.out.println("URL: " + hub + ":4444/wd/hub");
            URL url = new URL(hub + ":4444/wd/hub");
            wd = new RemoteWebDriver(url, capability1);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
        wd.manage().timeouts().implicitlyWait(60, TimeUnit.SECONDS);
        System.out
                .println("...success.  Attempting to augment RemoteWebDriver...");
        WebDriver wda = new Augmenter().augment(wd);
        if (addToWDList) {
            System.out
                    .println("...success.  Attempting to add wda to WebDriverList...");
            webDriverList.add(wda);
        }
        System.out.println("Capability " + capability1.toString()
                + " successfully added.");
        return wda;
    }

    @AfterSuite
    public void tearDown() throws Exception {

        System.out.println("RemoteTest::tearDown() of " + webDriverList.size()
                + " clients.");
        for (WebDriver w : webDriverList) {
            // w.close();
            w.quit();
        }
        // this will fail until you update your email login info below
        this.sendEmail(new File("./test-output/emailable-report.html"));
    }

    protected boolean isElementPresent(By by, WebDriver driver) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    protected boolean isAlertPresent(WebDriver driver) {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    protected String acceptAlert(WebDriver driver) {
        String alertText = null;
        try {
            Alert alert = driver.switchTo().alert();
            alertText = alert.getText();
            alert.accept();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return alertText;
    }

    protected String dismissAlert(WebDriver driver) {
        String alertText = null;
        try {
            Alert alert = driver.switchTo().alert();
            alertText = alert.getText();
            alert.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return alertText;
    }

    protected String closeAlertAndGetItsText(WebDriver driver) {
        String alertText = "No Alert / No Text";
        try {
            Alert alert = driver.switchTo().alert();
            alertText = alert.getText();
            if (acceptNextAlert) {
                alert.accept();
            } else {
                alert.dismiss();
            }
            return alertText;
        } finally {
            acceptNextAlert = true;
        }
    }

    protected boolean sendEmail(File file) {
        final String username = "b.shmoove@gmail.com";
        final String password = "";

        Properties props = new Properties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("SeleniumGrid@vt.edu"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse("your.email@yourserver.com"));
            message.setSubject("Test Results for " + TEST_CLASS_NAME);

            // Create the message part
            BodyPart messageBodyPart = new MimeBodyPart();

            // Fill the message
            messageBodyPart.setText("Screenshot:");

            // Create a multipart message
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            // Part two is attachment
            messageBodyPart = new MimeBodyPart();

            DataSource source = new FileDataSource(file);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(file.getName());
            multipart.addBodyPart(messageBodyPart);

            // Send the complete message parts
            message.setContent(multipart);

            System.out.println("Sending e-mail.");
            Transport.send(message);

            System.out.println("Done.");

        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // This one emails you a screenshot.
    protected void screenShot(WebDriver wda) {

        File screenshot = ((TakesScreenshot) wda)
                .getScreenshotAs(OutputType.FILE);
        System.out.println("Screenshot: " + screenshot.getAbsolutePath());
        boolean b = this.sendEmail(screenshot);
        if (b) {
            System.out.println("Email sent.");
        } else {
            System.out.println("Email failed!");
        }

    }

    // test will be run once for each set of parms
    @DataProvider(name = "webDriverList", parallel = true)
    public static Iterator<Object[]> webdrivers() {
        System.out.println("RemoteTest:webdrivers(): " + webDriverList.size()
                + " clients.");
        Collection<Object[]> provider = new ArrayList<Object[]>();
        for (WebDriver w : webDriverList) {
            provider.add(new Object[] { w });
        }
        return provider.iterator();
    }
}
