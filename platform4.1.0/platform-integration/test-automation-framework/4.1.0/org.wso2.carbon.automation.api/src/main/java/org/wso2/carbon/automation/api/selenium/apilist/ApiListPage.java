package org.wso2.carbon.automation.api.selenium.apilist;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class ApiListPage {

    private static final Log log = LogFactory.getLog(LoginPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public ApiListPage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("carbon.Main.tab"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("api.add.link"))).click();

        log.info("API Add Page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("api.dashbord.middle.text"))).
                getText().contains("API")) {

            throw new IllegalStateException("This is not the API  Add Page");
        }
    }

    public boolean checkOnUploadApi(String apiName) throws InterruptedException {

        driver.findElement(By.linkText(uiElementMapper.getElement("api.list.link"))).click();
        log.info(apiName);
        Thread.sleep(5000);

        String apiNameeOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/td[3]/table" +
                                                              "/tbody/tr[2]/td/div/div/form[4]/table/tbody" +
                                                              "/tr/td/a")).getText();
        log.info(apiNameeOnServer);
        if (apiName.equals(apiNameeOnServer)) {
            log.info("Uploaded Api exists");
            return true;
        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/" +
                                   "form[4]/table/tbody/tr[";
            String resourceXpath2 = "]/td/a";
            for (int i = 2; i < 10; i++) {
                String apiNameOnAppserver = resourceXpath + i + resourceXpath2;

                String actualApiname = driver.findElement(By.xpath(apiNameOnAppserver)).getText();
                log.info("val on app is -------> " + actualApiname);
                log.info("Correct is    -------> " + apiName);

                try {
                    if (apiName.equals(actualApiname)) {
                        log.info("Uploaded API    exists");
                        return true;
                    }
                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the Uploaded API");
                    return false;
                }
            }
        }
        return false;
    }

    public LoginPage logout() throws IOException {
        driver.findElement(By.linkText(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }

}
