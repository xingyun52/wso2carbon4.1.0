package org.wso2.carbon.automation.api.selenium.carbon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class CappHome {

    private static final Log log = LogFactory.getLog(LoginPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public CappHome(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("carbon.Main.tab"))).click();
        driver.findElement(By.id(uiElementMapper.getElement("carbon.Region1.tab"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("carbon.add.href"))).click();

        log.info("in the carbon element Add Page");
        if (!driver.findElement(By.id(uiElementMapper.getElement("carbon.dashboard.middle.text"))).
                getText().contains("Add Carbon Applications")) {

            throw new IllegalStateException("This is not the carbon Element adding page######################");
        }
    }

    public void UploadCarbonItem() throws InterruptedException {
        WebElement jaggeryUploadField = driver.findElement(By.name(uiElementMapper.getElement("carbon.file.upload.field")));
        jaggeryUploadField.sendKeys("/home/randika/Downloads/Capp_1.0.0LT.car");
        Thread.sleep(5000);

        driver.findElement(By.name(uiElementMapper.getElement("carbon.upload.button"))).click();
        Thread.sleep(5000);

        if (!driver.findElement(By.id(uiElementMapper.getElement("carbon.upload.successfull.message"))).
                getText().contains("successfully")) {

            throw new NoSuchElementException();
        }

        log.info("Successfully Uploaded");

        driver.findElement(By.className(uiElementMapper.getElement("carbon.upload.suffessfull.button"))).click();
        log.info("Ready to sign out");
    }

    public LoginPage logout() throws IOException {
        driver.findElement(By.linkText(uiElementMapper.getElement("home.greg.sign.out.xpath"))).click();
        return new LoginPage(driver);
    }

}


