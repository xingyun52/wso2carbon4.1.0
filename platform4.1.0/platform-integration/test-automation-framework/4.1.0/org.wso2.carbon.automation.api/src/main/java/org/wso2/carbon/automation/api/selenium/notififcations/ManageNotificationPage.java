package org.wso2.carbon.automation.api.selenium.notififcations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class ManageNotificationPage {

    private static final Log log = LogFactory.getLog(LoginPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public ManageNotificationPage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("configure.tab.id"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("notification.adding.link"))).click();
        log.info("notification Add Page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("notification.dashboard.middle.text"))).
                getText().contains("Manage Notifications")) {
            throw new IllegalStateException("This is not the correct Page");
        }
    }

    public boolean checkonUplodedNotification(String notificationsubscribe)
            throws InterruptedException {

        log.info("---------------------------->>>> " + notificationsubscribe);
        Thread.sleep(5000);
        String notificationNameOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/" +
                                                                      "td[3]/table/tbody/tr[2]/td/div/div/table/tbody/tr/td[5]")).getText();
        log.info(notificationNameOnServer);
        if (notificationsubscribe.equals(notificationNameOnServer)) {
            log.info("newly Created notification exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/table/tbody/tr[";
            String resourceXpath2 = "]/td[5]";

            for (int i = 2; i < 10; i++) {
                String notificationNameOnAppserver = resourceXpath + i + resourceXpath2;
                String actualUsername = driver.findElement(By.xpath(notificationNameOnAppserver)).getText();
                log.info("val on app is -------> " + actualUsername);
                log.info("Correct is    -------> " + notificationsubscribe);
                try {

                    if (notificationsubscribe.equals(actualUsername)) {
                        log.info("newly Created notification   exists");
                        return true;

                    }

                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the newly Created notification");
                    return false;

                }

            }

        }

        return false;
    }

}
