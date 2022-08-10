package selenium;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public class WebDriverProvider {
    public static final boolean IS_HEADLESS = true;
    private static final String CHROME_DRIVER = "webdriver.chrome.driver";
    private static final String FIREFOX_DRIVER = "webdriver.gecko.driver";

    public static WebDriver getDriver(String driverName) {
        switch (driverName) {
            case CHROME_DRIVER:
                return getChromeDriver();
            case FIREFOX_DRIVER:
                return getFirefoxDriver();
        }
        throw new IllegalArgumentException("Invalid driver: " + driverName);
    }

    private static WebDriver getFirefoxDriver() {
        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.setHeadless(IS_HEADLESS);
        return new FirefoxDriver(firefoxOptions);
    }

    private static WebDriver getChromeDriver() {
        ChromeOptions chromeOptions = new ChromeOptions();
        if (IS_HEADLESS) {
            chromeOptions.addArguments("--headless");
        }
        return new ChromeDriver(chromeOptions);
    }
}
