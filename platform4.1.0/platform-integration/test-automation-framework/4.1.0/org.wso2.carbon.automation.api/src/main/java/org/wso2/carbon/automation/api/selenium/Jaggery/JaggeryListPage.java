package org.wso2.carbon.automation.api.selenium.Jaggery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class JaggeryListPage {

    private static final Log log = LogFactory.getLog(JaggeryHome.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public JaggeryListPage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
         log.info("in the jaggeryList page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("jaggery.dashboard.middle.text"))).
                getText().contains("Running Applications")) {
            throw new IllegalStateException("This is not the Jaggary list page");
        }
    }

    public boolean checkOnUploadJaggeryItem(String serviceName) throws InterruptedException {
        log.info(serviceName);
        Thread.sleep(5000);
        driver.navigate().refresh();

           String ServicenameOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/td[3]" +
                "/table/tbody/tr[2]/td/div/div/form[2]/table/tbody/tr/td[2]/a")).getText();

        log.info(ServicenameOnServer);

        if (serviceName.equals(ServicenameOnServer)) {
            log.info("Uploaded service exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div" +
                    "/form[2]/table/tbody/tr/td[";

            String resourceXpath2 = "]/a";
            for (int i = 2; i < 10; i++) {
                String servicenameOnAppserver = resourceXpath + i + resourceXpath2;
                String actualresorcename = driver.findElement(By.xpath(servicenameOnAppserver)).getText();
                log.info("val on app is -------> " + actualresorcename);
                log.info("Correct is    -------> " + serviceName);
                try {
                    if (serviceName.equals(actualresorcename)) {
                        log.info("Uploaded service exists");
                        return true;

                    }

                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the Uploaded service");
                    return false;
                }

            }

        }

        return false;
    }


    public LoginPage logout() throws IOException {
        driver.findElement(By.xpath(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }

}
