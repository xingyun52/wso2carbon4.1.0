package org.wso2.carbon.automation.api.selenium.resourcebrowse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.WebDriver;
import org.wso2.carbon.automation.api.selenium.util.UIElementMapper;

import java.io.IOException;

public class ResourceBrowsePage {

    private static final Log log = LogFactory.getLog(ResourceBrowsePage.class);

    public ResourceBrowsePage(WebDriver driver) throws IOException {
        UIElementMapper uiElementMapper = UIElementMapper.getInstance();
        // Check that we're on the right page.
        if (!(driver.getCurrentUrl().contains("resources"))) {
            // Alternatively, we could navigate to the login page, perhaps logging out first
            throw new IllegalStateException("This is not the resource Browse page");
        }
    }
}
