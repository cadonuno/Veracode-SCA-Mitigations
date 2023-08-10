package selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import selenium.exceptions.WrongCredentialsException;

import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class ScaSeleniumWrapper {
    public static final String APPLICATION_BASE_URL = "https://analysiscenter.veracode.com/auth/index.jsp#";
    private static final String LOGIN_URL = "https://web.analysiscenter.veracode.com/login/";
    private static final String USERNAME_FIELD_ID = "okta-signin-username";
    private static final String PASSWORD_FIELD_ID = "okta-signin-password";
    private static final String LOGIN_BUTTON_ID = "okta-signin-submit";
    private static final String USER_NAME_ICON_ID = "icon_user";
    private static final String SCA_BUTTON_XPATH = "//span[text() = 'Software Composition Analysis']";
    private static final String LICENSES_TAB_LINK_XPATH = "//li[@class='ng-scope inactive']/a[text() = 'Licenses']";
    private static final String VULNERABILITIES_TAB_LINK_XPATH = "//li[@class='ng-scope inactive']/a[text() = 'Vulnerabilities']";
    private static final String SCA_ISSUES_TABLE_LOADING = "scaApplicationComponentsTable_processing";
    private static final String SCA_LICENSES_TABLE_LOADING = "scaLicensesTableWithCheckbox_processing";
    private static final String SCA_VULNERABILITIES_TABLE_LOADING = "scaVulnerabilitiesTableWithCheckbox_processing";
    public static final String SCA_LICENSES_FILTER_ID = "scaFilterSelectLicenses";

    private static final String SCA_VULNERABILITIES_FILTER_ID = "scaFilterSelectVulnerabilities";
    public static final String SCA_LICENSE_FILTER_ACTIVATE_BUTTON_ID = "scaFilterBtnLicenses";
    public static final String SCA_VULNERABILITY_FILTER_ACTIVATE_BUTTON_ID = "scaFilterBtnVulnerabilities";
    public static final String SCA_LICENSES_TABLE_ID = "scaLicensesTableWithCheckbox";
    public static final String SCA_VULNERABILITIES_TABLE_ID = "scaVulnerabilitiesTableWithCheckbox";

    public static final Predicate<WebElement> CHECK_FOR_ELEMENT_INVISIBLE_PREDICATE = (element) -> element.getAttribute("style").equals("display: none;");


    public static void switchToLicensesPage(WebDriverWrapper webDriver) throws TimeoutException {
        webDriver.waitForElementPresent(By.xpath(LICENSES_TAB_LINK_XPATH));
        webDriver.waitForCondition(By.id(SCA_ISSUES_TABLE_LOADING),
                CHECK_FOR_ELEMENT_INVISIBLE_PREDICATE);
        webDriver.clickElement(By.xpath(LICENSES_TAB_LINK_XPATH));
        webDriver.waitForElementPresent(By.id(SCA_LICENSES_FILTER_ID));
        waitForLicenseResultsToLoad(webDriver);
    }

    public static void switchToVulnerabilitiesPage(WebDriverWrapper webDriver) throws TimeoutException {
        webDriver.waitForElementPresent(By.xpath(VULNERABILITIES_TAB_LINK_XPATH));
        webDriver.waitForCondition(By.id(SCA_ISSUES_TABLE_LOADING),
                CHECK_FOR_ELEMENT_INVISIBLE_PREDICATE);
        webDriver.clickElement(By.xpath(VULNERABILITIES_TAB_LINK_XPATH));
        webDriver.waitForElementPresent(By.id(SCA_VULNERABILITIES_FILTER_ID));
        waitForVulnerabilitiesResultsToLoad(webDriver);
    }

    public static void waitForVulnerabilitiesResultsToLoad(WebDriverWrapper webDriver) throws TimeoutException {
        waitForLoad(webDriver, By.id(SCA_VULNERABILITIES_TABLE_LOADING));
    }

    public static boolean tryOpenScaPage(WebDriverWrapper webDriver) throws TimeoutException {
        By scaButtonXpathSelector = By.xpath(SCA_BUTTON_XPATH);
        boolean hasFoundScaResults = webDriver.hasElement(scaButtonXpathSelector);
        if (!hasFoundScaResults) {
            return false;
        }
        webDriver.clickElement(scaButtonXpathSelector);
        return true;
    }

    public static void loginToPlatform(String username, String password, WebDriverWrapper webDriverWrapper) throws TimeoutException, WrongCredentialsException {
        webDriverWrapper.initializeAtUrl(LOGIN_URL);
        webDriverWrapper.waitForElementPresent(By.id(LOGIN_BUTTON_ID));
        webDriverWrapper.sendKeysTo(By.id(USERNAME_FIELD_ID), username);
        webDriverWrapper.sendKeysTo(By.id(PASSWORD_FIELD_ID), password);
        webDriverWrapper.clickElement(By.id(LOGIN_BUTTON_ID));
        try {
            webDriverWrapper.waitForElementPresent(By.id(USER_NAME_ICON_ID));
        } catch (TimeoutException e) {
            throw new WrongCredentialsException();
        }
    }

    public static void waitForLicenseResultsToLoad(WebDriverWrapper webDriver) throws TimeoutException {
        waitForLoad(webDriver, By.id(SCA_LICENSES_TABLE_LOADING));
    }

    public static void waitForLoad(WebDriverWrapper webDriver, By elementToCheck) throws TimeoutException {
        webDriver.waitForCondition(elementToCheck,
                ScaSeleniumWrapper.CHECK_FOR_ELEMENT_INVISIBLE_PREDICATE);
    }

    public static void startLicenseFilter(WebDriverWrapper webDriver, String filterText) throws TimeoutException {
        webDriver.clickElement(By.id(ScaSeleniumWrapper.SCA_LICENSES_FILTER_ID));
        webDriver.selectOptionByVisibleText(By.id(ScaSeleniumWrapper.SCA_LICENSES_FILTER_ID), filterText);
    }

    public static void startVulnerabilityFilter(WebDriverWrapper webDriver, String filterText) throws TimeoutException {
        webDriver.clickElement(By.id(ScaSeleniumWrapper.SCA_VULNERABILITIES_FILTER_ID));
        webDriver.selectOptionByVisibleText(By.id(ScaSeleniumWrapper.SCA_VULNERABILITIES_FILTER_ID), filterText);
    }

    public static boolean isLoggedIn(WebDriverWrapper webDriverWrapper) {
        webDriverWrapper.openUrl("https://web.analysiscenter.veracode.com/");
        return webDriverWrapper.hasElement(By.id(USER_NAME_ICON_ID));
    }
}
