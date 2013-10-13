package org.wso2.carbon.automation.api.selenium.myprofile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class MyProfilePage {

    private static final Log log = LogFactory.getLog(LoginPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public MyProfilePage(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.linkText(uiElementMapper.getElement("my.profile.add.page.link"))).click();

        log.info("profile Add Page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("my.profile.dashbord.middle.text"))).
                getText().contains("Profiles")) {

            throw new IllegalStateException("This is not the My Profile  Add Page");
        }
    }

    public boolean checkonUploadProfile(String apiName) throws InterruptedException {

        driver.findElement(By.linkText(uiElementMapper.getElement("api.list.link"))).click();
        log.info(apiName);
        Thread.sleep(5000);
        String profileNameOnServer = driver.findElement(By.xpath("/html/body/table/tbody/tr[2]/td[3]" +
                                                                 "/table/tbody/tr[2]/td/div/div/form[4]/table/tbody/tr/td/a")).getText();

        log.info(profileNameOnServer);

        if (apiName.equals(profileNameOnServer))

        {
            log.info("Uploaded Api exists");
            return true;

        } else {
            String resourceXpath = "/html/body/table/tbody/tr[2]/td[3]/table/tbody/tr[2]/td/div/div" +
                                   "/form[4]/table/tbody/tr[";
            String resourceXpath2 = "]/td/a";

            for (int i = 2; i < 10; i++) {
                String profileNameOnAppServer = resourceXpath + i + resourceXpath2;
                String actualProfilename = driver.findElement(By.xpath(profileNameOnAppServer)).getText();
                log.info("val on app is -------> " + actualProfilename);
                log.info("Correct is    -------> " + apiName);

                try {

                    if (apiName.equals(actualProfilename)) {
                        log.info("Uploaded Profile    exists");
                        return true;

                    }

                } catch (NoSuchElementException ex) {
                    log.info("Cannot Find the Uploaded Profile");

                    return false;
                }

            }
        }

        return false;
    }

    public void uploadProfile(String provider, String name, String context, String version)
            throws InterruptedException, IOException {

        driver.findElement(By.id(uiElementMapper.getElement("my.profile.region.tab.id"))).click();

        driver.findElement(By.linkText(uiElementMapper.getElement("my.profile.new.profile.add.link"))).click();

        WebElement profileName = driver.findElement(By.id(uiElementMapper.getElement("my.profile.name.id")));
        profileName.sendKeys(provider);

        WebElement profileFirstName = driver.findElement(By.id(uiElementMapper.getElement("my.profile.firstname.id")));
        profileFirstName.sendKeys(name);

        WebElement profileLastName = driver.findElement(By.id(uiElementMapper.getElement("my.profile.lastname.id")));
        profileLastName.sendKeys(context);

        WebElement profileEmail = driver.findElement(By.id(uiElementMapper.getElement("my.profile.email.id")));
        profileEmail.sendKeys(version);

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("validate()");

        //driver.findElement(By.xpath(uiElementMapper.getElement("my.profile.successful.save.pane"))).click();
        log.info("successfuly Saved");

    }

    public LoginPage logout() throws IOException {
        driver.findElement(By.xpath(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }

}
