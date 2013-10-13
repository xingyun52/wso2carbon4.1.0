package org.wso2.carbon.automation.api.selenium.uriList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class UriListPage {

    private static final Log log = LogFactory.getLog(UriListPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public UriListPage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        if (!(driver.getCurrentUrl().contains("generic"))) {
            // Alternatively, we could navigate to the login page, perhaps logging out first
            throw new IllegalStateException("This is not the URI List page");
        }
    }

    public boolean checkonUploadUri(String UriName) throws InterruptedException {

        driver.findElement(By.linkText(uiElementMapper.getElement("uri.add.list.id"))).click();

        Thread.sleep(5000);

        String uriNameOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/td[3]" +
                                                             "/table/tbody/tr[2]/td/div/div/form[4]/table/tbody/tr/td")).getText();
        log.info(uriNameOnServer);
        if (UriName.equals(uriNameOnServer)) {
            log.info("Uploaded Uri exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div" +
                                   "/form[4]/table/tbody/tr[";
            String resourceXpath2 = "]/td";

            for (int i = 2; i < 10; i++) {
                String urinameOnAppserver = resourceXpath + i + resourceXpath2;

                String actualUriname = driver.findElement(By.xpath(urinameOnAppserver)).getText();
                log.info("val on app is -------> " + actualUriname);
                log.info("Correct is    -------> " + UriName);

                try {

                    if (UriName.equals(actualUriname)) {
                        log.info("Uploaded URI exists");
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
