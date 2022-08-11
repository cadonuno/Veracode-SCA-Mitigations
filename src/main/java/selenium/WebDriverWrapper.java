package selenium;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;
import util.TimeUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public final class WebDriverWrapper {

    private static final String SELENIUM_DRIVER_NAME = "webdriver.gecko.driver";
    private static final String SELENIUM_DRIVER_LOCATION = "C:\\Veracode\\geckodriver.exe";
    private static final int POLLING_TIMEOUT = 30;

    static {
        System.setProperty(SELENIUM_DRIVER_NAME, SELENIUM_DRIVER_LOCATION);
    }

    private final WebDriver webDriver;

    public WebDriverWrapper() {
        this.webDriver = WebDriverProvider.getDriver(SELENIUM_DRIVER_NAME);
    }

    public boolean hasElement(By elementToCheck) {
        try {
            waitForElementPresent(elementToCheck);
        } catch (TimeoutException e) {
            return false;
        }
        return true;
    }

    public void waitForElementPresent(By elementToCheck) throws TimeoutException {
        boolean hasFound = false;
        Instant start = Instant.now();
        while (!hasFound) {
            try {
                webDriver.findElement(elementToCheck);
                hasFound = true;
            } catch (NoSuchElementException notFound) {
                checkTimeout(start);
            }
        }
    }

    public static void checkTimeout(Instant start) throws TimeoutException {
        if (getTimeElapsed(start) > POLLING_TIMEOUT) {
            timeout();
        }
        TimeUtils.sleepFor(1);
    }

    public void clickElement(By elementToClick) throws TimeoutException {
        boolean hasFound = false;
        Instant start = Instant.now();
        while (!hasFound) {
            try {
                webDriver.findElement(elementToClick).click();
                hasFound = true;
            } catch (ElementNotInteractableException | NoSuchElementException notFound) {
                checkTimeout(start);
            }
        }
    }

    public void selectOptionByVisibleText(By elementToSelect, String textToSelect) throws TimeoutException {
        boolean hasFound = false;
        Instant start = Instant.now();
        WebElement foundElement = null;
        while (!hasFound) {
            try {
                foundElement = webDriver.findElement(elementToSelect);
                hasFound = true;
            } catch (ElementNotInteractableException notFound) {
                checkTimeout(start);
            }
        }
        new Select(foundElement).selectByVisibleText(textToSelect);
    }

    private static void timeout() throws TimeoutException {
        throw new TimeoutException("Timed out when running command");
    }

    private static long getTimeElapsed(Instant start) {
        return Duration.between(start, Instant.now()).getSeconds();
    }

    public Optional<WebElement> getElement(By elementToGet) {
        Instant start = Instant.now();
        while (true) {
            try {
                return Optional.ofNullable(webDriver.findElement(elementToGet));
            } catch (ElementNotInteractableException notFound) {
                try {
                    checkTimeout(start);
                } catch (TimeoutException timeout) {
                    return Optional.empty();
                }
            }
        }
    }

    public void waitForCondition(By elementToCheck,
                                 Predicate<WebElement> webElementPredicate) throws TimeoutException {
        waitForConditionInternal(elementToCheck, webElementPredicate, true);
    }

    private void waitForConditionInternal(By elementToCheck,
                                          Predicate<WebElement> webElementPredicate,
                                          boolean failOnMissingElement) throws TimeoutException {
        Instant start = Instant.now();
        boolean isAwaitingCondition = true;
        while (isAwaitingCondition) {
            try {
                WebElement foundElement = webDriver.findElement(elementToCheck);
                isAwaitingCondition = !webElementPredicate.test(foundElement);
            } catch (ElementNotInteractableException notFound) {
                checkTimeout(start);
            } catch (NoSuchElementException nonExisting) {
                if (failOnMissingElement) {
                    checkTimeout(start);
                } else {
                    isAwaitingCondition = false;
                }
            }
        }
    }

    public void initializeAtUrl(String urlToOpen) {
        openUrl(urlToOpen);
        webDriver.manage().window().setSize(new Dimension(1920, 1080));
    }

    public void sendKeysTo(By fieldToSendKeys, String keysToSend) {
        webDriver.findElement(fieldToSendKeys).sendKeys(keysToSend);
    }

    public void quit() {
        webDriver.quit();
    }

    public void openUrl(String url) {
        webDriver.get(url);
    }
}
