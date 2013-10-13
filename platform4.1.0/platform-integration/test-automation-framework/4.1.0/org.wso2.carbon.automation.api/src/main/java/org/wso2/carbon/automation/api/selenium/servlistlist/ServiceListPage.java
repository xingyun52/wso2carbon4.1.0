package org.wso2.carbon.automation.api.selenium.servlistlist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class ServiceListPage {

    private static final Log log = LogFactory.getLog(ServiceListPage.class);
    private WebDriver driver;

    public ServiceListPage(WebDriver driver) throws IOException {
        this.driver = driver;
        UIElementMapper uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        if (!driver.findElement(By.id(uiElementMapper.getElement("service.list.dashboard.middle.text"))).
                getText().contains("Service List")) {

            throw new IllegalStateException("This is not the Service List Page");
        }
    }

    public boolean checkonUploadService(String serviceName) throws InterruptedException {
        Thread.sleep(5000);
        String ServicenameOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/td[3]" +
                                                                 "/table/tbody/tr[2]/td/div/div/form[4]/table/tbody/tr/td")).getText();
        log.info(ServicenameOnServer);
        if (serviceName.equals(ServicenameOnServer)) {
            log.info("Uploaded Service exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/" +
                                   "form[4]/table/tbody/tr[";
            String resourceXpath2 = "]/td";
            for (int i = 2; i < 10; i++) {
                String servicenameOnAppserver = resourceXpath + i + resourceXpath2;
                String actualresorcename = driver.findElement(By.xpath(servicenameOnAppserver)).getText();
                log.info("val on app is -------> " + actualresorcename);
                log.info("Correct is    -------> " + serviceName);
                try {

                    if (serviceName.equals(actualresorcename)) {
                        log.info("Uploaded Service exists");
                        return true;

                    }

                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the Uploaded Element");
                    return false;
                }

            }

        }

        return false;
    }

}
