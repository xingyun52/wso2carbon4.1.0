package org.wso2.carbon.automation.api.selenium.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.wso2.carbon.automation.api.selenium.login.LoginPage;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;
import java.util.NoSuchElementException;

public class ResourceHome {

    private static final Log log = LogFactory.getLog(LoginPage.class);
    private WebDriver driver;
    private UIElementMapper uiElementMapper;

    public ResourceHome(WebDriver driver) throws IOException {
        this.driver = driver;
        this.uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        driver.findElement(By.id(uiElementMapper.getElement("resource.Main.tab"))).click();
        driver.findElement(By.id(uiElementMapper.getElement("resource.Region3.tab"))).click();
        driver.findElement(By.linkText(uiElementMapper.getElement("resource.browse.link"))).click();

        if (!driver.findElement(By.id(uiElementMapper.getElement("resource.dashboard.middle.text"))).
                getText().contains("Browse")) {

            log.info("in the Resources Browse page");

        }

        try {
            driver.findElement(By.id(uiElementMapper.getElement("resource.detailed.view"))).click();
        } catch (NoSuchElementException ex) {
            log.info("element not found");

        }

    }

    public void uploadResourceFromFile(String FilePath) throws InterruptedException {

        log.info("------------------>" + FilePath);
        driver.findElement(By.linkText(uiElementMapper.getElement("resource.add.resource.link"))).click();
        WebElement CarbonuploadResource = driver.findElement(By.id(uiElementMapper.getElement
                ("resource.add.resource.input.field")));
        CarbonuploadResource.sendKeys(FilePath);
        driver.findElement(By.id(uiElementMapper.getElement("resource.add.resource.name"))).sendKeys("TestFile");
        driver.findElement(By.xpath(uiElementMapper.getElement("resource.add.button"))).click();
        if (!driver.findElement(By.id(uiElementMapper.getElement("resource.upload.successfull.message"))).
                getText().contains("successfully")) {
            log.info("Successfully uploaded a resource");
        }
        driver.findElement(By.className(uiElementMapper.getElement("resource.upload.suffessfull.button"))).click();

    }

    public void uploadResourceFromUrl(String URL) throws InterruptedException {
        log.info(URL);
        driver.findElement(By.linkText(uiElementMapper.getElement("resource.add.resource.link"))).click();
        new Select(driver.findElement(By.id("addMethodSelector"))).selectByVisibleText("Import content from URL");
        WebElement CarbonuploadResource = driver.findElement(By.id(uiElementMapper.getElement("resource.add.Url.input.id")));
        CarbonuploadResource.sendKeys(URL);
        driver.findElement(By.xpath(uiElementMapper.getElement("resource.add.Url.button.xpath"))).click();
        if (!driver.findElement(By.id(uiElementMapper.getElement("resource.upload.successfull.message"))).
                getText().contains("successfully")) {
            log.info("Successfully uploaded a resource");

        }

        driver.findElement(By.xpath(uiElementMapper.getElement("resource.add.Url.Successfull.close"))).click();

    }

    public boolean checkonUploadSuccess(String uploadedElement) {
        String resourceId = "resourceView";

        for (int i = 1; i < 10; i++) {
            String actResourceName = resourceId + i;

            String actualresorcename = driver.findElement(By.id(actResourceName)).getText();
            log.info("value here is -------> " + actualresorcename);
            log.info("actval ere is -------> " + uploadedElement);
            try {
                if (uploadedElement.equals(actualresorcename)) {
                    log.info("Uploaded resource exists");
                    return true;

                }

            } catch (NoSuchElementException ex) {
                log.info("Cannot Find the Uploaded Element");
                return false;
            }

        }

        return false;
    }

    public void uploadCollection(String folderName) throws InterruptedException {

        driver.findElement(By.linkText(uiElementMapper.getElement("resource.add.collection.link"))).click();
        WebElement ResourceUploadField = driver.findElement(By.id(uiElementMapper.getElement
                ("resource.add.Collection.input.field")));
        ResourceUploadField.sendKeys(folderName);
        driver.findElement(By.id(uiElementMapper.getElement("resource.add.collection.description"))).sendKeys("My File");
        driver.findElement(By.xpath(uiElementMapper.getElement("resource.collection.add.button"))).click();
        if (!driver.findElement(By.id(uiElementMapper.getElement("resource.upload.successfull.collection.message"))).
                getText().contains("successfully")) {
            log.info("Successfully uploaded a Collection");
        }

        driver.findElement(By.xpath(uiElementMapper.getElement("resource.upload.collection.successfull.close.button"))).click();

    }

}
