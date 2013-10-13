package org.wso2.carbon.automation.api.selenium.configuretab;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class RoleHomePage {

    private static final Log log = LogFactory.getLog(LoginPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public RoleHomePage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("configure.tab.id"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("users.and.roles.link.text"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("roles.add.link.id"))).click();
        log.info("Roles add page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("users.adn.roles.add.page.middle.text"))).
                getText().contains("Roles")) {

            throw new IllegalStateException("This is not the correct Page");
        }
    }

    public boolean checkonUploadrole(String roleName) throws InterruptedException {

        if (!driver.findElement(By.id(uiElementMapper.getElement("users.adn.roles.add.page.middle.text"))).
                getText().contains("Roles")) {

            throw new IllegalStateException("This is not the user add Page");
        }

        log.info(roleName);
        Thread.sleep(5000);

        String roleNameeOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/table/tbody/tr/td")).getText();
        log.info(roleNameeOnServer);
        if (roleName.equals(roleNameeOnServer)) {
            log.info("Uploaded Api exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div/table/tbody/tr[";
            String resourceXpath2 = "]/td";

            for (int i = 2; i < 10; i++) {
                String roleNameOnAppserver = resourceXpath + i + resourceXpath2;

                String actualRolename = driver.findElement(By.xpath(roleNameOnAppserver)).getText();
                log.info("val on app is -------> " + actualRolename);
                log.info("Correct is    -------> " + roleName);

                try {

                    if (roleName.equals(actualRolename)) {
                        log.info("newly Created Role   exists");
                        return true;

                    }

                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the newly Created ROle");
                    return false;
                }

            }

        }

        return false;
    }

    public void addRole(String roleName) throws InterruptedException {

        driver.findElement(By.linkText(uiElementMapper.getElement("role.add.newuser.link.id"))).click();
        driver.findElement(By.name(uiElementMapper.getElement("role.add.newuser.name.id"))).sendKeys(roleName);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("doNext()");

        driver.findElement(By.className(uiElementMapper.getElement("role.permission.id"))).click();

        Thread.sleep(3000);

        js.executeScript("doNext()");

        driver.findElement(By.name(uiElementMapper.getElement("role.add.user.to.role.name"))).sendKeys("Seleniumtest");
        driver.findElement(By.xpath(uiElementMapper.getElement("role.search.button"))).click();
        driver.findElement(By.name(uiElementMapper.getElement("role.add.user.to.role.name.tick"))).click();
        driver.findElement(By.xpath(uiElementMapper.getElement("role.add.user.finish.button"))).click();

    }

    public LoginPage logout() throws IOException {
        driver.findElement(By.xpath(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }

}
