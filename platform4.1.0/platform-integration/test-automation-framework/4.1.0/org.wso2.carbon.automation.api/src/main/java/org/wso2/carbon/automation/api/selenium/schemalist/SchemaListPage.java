package org.wso2.carbon.automation.api.selenium.schemalist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class SchemaListPage {

    private static final Log log = LogFactory.getLog(SchemaListPage.class);
    private WebDriver driver;

    public SchemaListPage(WebDriver driver) throws IOException {
        this.driver = driver;
        UIElementMapper uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        if (!driver.findElement(By.id(uiElementMapper.getElement("schema.list.dashboard.middle.text"))).
                getText().contains("List")) {

            throw new IllegalStateException("This is not Schema List Page");
        }
    }

    public boolean checkonUploadSchema(String wsdlName) throws InterruptedException {
        log.info(wsdlName);
        Thread.sleep(5000);
        // driver.findElement(By.xpath(uiElementMapper.getElement("service.check.save.service"))).click();
        String ServicenameOnServer = driver.findElement(By.xpath(" /html/body/table/tbody/tr[2]/td[3]" +
                                 "/table/tbody/tr[2]/td/div/div/form[4]/table/tbody/tr/td/a")).getText();

        log.info(ServicenameOnServer);

        if (wsdlName.equals(ServicenameOnServer)) {
            log.info("Uploaded Schema exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/" +
                                                                      "div/form[4]/table/tbody/tr[";

            String resourceXpath2 = "]/td/a";
            for (int i = 2; i < 10; i++) {
                String servicenameOnAppserver = resourceXpath + i + resourceXpath2;
                String actualresorcename = driver.findElement(By.xpath(servicenameOnAppserver)).getText();
                log.info("val on app is -------> " + actualresorcename);
                log.info("Correct is    -------> " + wsdlName);
                try {
                    if (wsdlName.equals(actualresorcename)) {
                        log.info("Uploaded Schema    exists");
                        return true;

                    }

                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the Uploaded Wsdl");
                    return false;
                }

            }

        }

        return false;
    }

}
