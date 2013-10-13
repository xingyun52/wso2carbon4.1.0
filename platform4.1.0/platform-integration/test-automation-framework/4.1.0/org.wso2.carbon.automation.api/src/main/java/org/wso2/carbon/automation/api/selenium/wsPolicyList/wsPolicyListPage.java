package org.wso2.carbon.automation.api.selenium.wsPolicyList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class wsPolicyListPage {

    private static final Log log = LogFactory.getLog(wsPolicyListPage.class);
    private WebDriver driver;

    public wsPolicyListPage(WebDriver driver) throws IOException {
        this.driver = driver;
        UIElementMapper uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        if (!driver.findElement(By.id(uiElementMapper.getElement("wspolicy.list.dashboard.middle.text"))).
                getText().contains("Policy")) {

            throw new IllegalStateException("This is not the Ws Policy  Add Page");
        }
    }

    public boolean checkonUploadpolicy(String policyName) throws InterruptedException {
        log.info(policyName);
        Thread.sleep(5000);
        // driver.findElement(By.xpath(uiElementMapper.getElement("service.check.save.service"))).click();
        String ServicenameOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/form[4]/table/tbody/tr/td/a")).getText();
        log.info(ServicenameOnServer);
        if (policyName.equals(ServicenameOnServer)) {
            log.info("Uploaded Ws Policy exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/form[4]/table/tbody/tr[";
            String resourceXpath2 = "]/td/a";

            for (int i = 2; i < 10; i++) {
                String servicenameOnAppserver = resourceXpath + i + resourceXpath2;
                String actualresorcename = driver.findElement(By.xpath(servicenameOnAppserver)).getText();
                log.info("val on app is -------> " + actualresorcename);
                log.info("Correct is    -------> " + policyName);
                try {
                    if (policyName.equals(actualresorcename)) {
                        log.info("Uploaded Ws polciy    exists");
                        return true;
                    }

                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the Uploaded Ws polciy");
                    return false;
                }
            }
        }
        return false;
    }

}
