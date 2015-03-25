package edu.vt.qav.tests.grid;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

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
import org.testng.annotations.Test;

/*
 * This test is the base class for all Grid tests.
 * Connects to the hub.
 * Creates augmented WebDrivers for screenshots.
 * Reads testng.xml for list of desired operating systems.
 * Sends email.
 * 
 * All available platforms and browsers will be attempted.  We catch an
 * exception if WebDriver creation fails.  At the end we will have a set of
 * WebDrivers that represents all registered nodes and browsers.
 * 
 * @author Brian Long
 */
public class RemoteTest {

    protected String                 TEST_CLASS      = this.getClass()
                                                             .getCanonicalName();
    protected DesiredCapabilities    capability1;
    protected static String          hub;
    protected boolean                acceptNextAlert = true;
    protected static List<WebDriver> webDriverList   = new ArrayList<WebDriver>();
    protected String                 verificationErrorString;
    protected String                 baseUrl;

    @BeforeSuite
    @Parameters({ "hub", "oslist" })
    // defined in resources/testng.xml
    public void setUp(String hub, String oslist) throws Exception {

        // why isn't there an iterator for DesiredCapabilities?
        List<DesiredCapabilities> browserList = new ArrayList<DesiredCapabilities>();
        browserList.add(DesiredCapabilities.android());
        browserList.add(DesiredCapabilities.chrome());
        browserList.add(DesiredCapabilities.firefox());
        browserList.add(DesiredCapabilities.internetExplorer());
        browserList.add(DesiredCapabilities.ipad());
        browserList.add(DesiredCapabilities.iphone());
        browserList.add(DesiredCapabilities.opera());
        browserList.add(DesiredCapabilities.safari());
        System.out.println("browserList has " + browserList.size() + " items.");

        for (Platform p : Platform.values()) {
            if (!oslist.contains(p.name())) {
                System.out.println("os-list does not contain " + p.name());
                continue;
            } else {
                for (DesiredCapabilities browser : browserList) {
                    try { // try to create a webdriver on every known browser
                        capability1 = browser;
                        capability1.setPlatform(p);
                        System.out
                                .println("Attempting to create RemoteWebDriver with capability "
                                        + capability1.toString());
                        WebDriver wd = new RemoteWebDriver(new URL(hub
                                + ":4444/wd/hub"), capability1);
                        System.out
                                .println("...success.  Attempting to augment RemoteWebDriver...");
                        WebDriver wda = new Augmenter().augment(wd);
                        System.out
                                .println("...success.  Attempting to add wda to WebDriverList...");
                        webDriverList.add(wda);
                        System.out.println("Capability "
                                + capability1.toString()
                                + " successfully added.");
                    } catch (org.openqa.selenium.WebDriverException wde) {
                        System.out.println("No node found with capability "
                                + capability1.toString());
                        // wde.printStackTrace();
                    }
                }
            }
        }
        System.out.println("RemoteTest::setUp(): " + webDriverList.size()
                + " clients.");
        if (webDriverList.size() == 0) {
            throw new Exception("No items in WebDriverList!");
        }
    }

    @Test(dataProvider = "webDriverList", threadPoolSize = 20)
    private void GridTest(WebDriver wda) {
        baseUrl = "http://www.seti.it.vt.edu/QAV/index.html";
        wda.get(baseUrl);
        try {
            Thread.sleep(30000); // show our webpage for a few seconds then go
                                 // away
        } catch (InterruptedException ie) {
            // carry on
        }
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

    private boolean isElementPresent(By by, WebDriver driver) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean isAlertPresent(WebDriver driver) {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException e) {
            return false;
        }
    }

    private String closeAlertAndGetItsText(WebDriver driver) {
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
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
        final String username = "brian.long@vt.edu";
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
            message.setFrom(new InternetAddress("from@yourserver.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse("to@yourserver.com"));
            message.setSubject("Test Results for " + TEST_CLASS);

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
}
