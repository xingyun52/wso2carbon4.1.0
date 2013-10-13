package org.wso2.carbon.automation.api.selenium.tenant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class TenantListpage {

    private static final Log log = LogFactory.getLog(LoginPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public TenantListpage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("configure.tab.id"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("add.new.tenant.link.text"))).click();
        log.info("New Tenant list page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("tenant.role.dashboard.middle.text"))).
          getText().contains("Register A New Organization")) {
          throw new IllegalStateException("This is not the correct Page");
        }
    }

    public boolean checkonUplodedTenant(String tenantName) throws InterruptedException {

        log.info("---------------------------->>>> " + tenantName);
        Thread.sleep(5000);
        driver.findElement(By.id(uiElementMapper.getElement("configure.tab.id"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("veiw.tenant.link"))).click();
        String tenantNameOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/td[3]" +
                                                                "/table/tbody/tr[2]/td/div[2]/div/table/tbody/tr/td")).getText();
        log.info(tenantNameOnServer);
        if (tenantName.equals(tenantNameOnServer)) {
            log.info("newly Created notification exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div[2]/div/table/tbody/tr[";
            String resourceXpath2 = "]/td";

            for (int i = 2; i < 10; i++) {
                String tenantNameOnAppserver = resourceXpath + i + resourceXpath2;
                String actualUsername = driver.findElement(By.xpath(tenantNameOnAppserver)).getText();
                log.info("val on app is -------> " + actualUsername);
                log.info("Correct is    -------> " + tenantName);
                try {
                    if (tenantName.equals(actualUsername)) {
                        log.info("newly Created Organization   exists");
                        return true;

                    }

                } catch (NoSuchElementException ex) {

                    log.info("Cannot Find the newly Created organization");

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
