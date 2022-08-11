package selenium;

import api.HttpCodes;
import api.records.MitigationResult;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import scaresults.LicenseFinding;
import scaresults.MitigationProposal;
import selenium.exceptions.LibraryNotFoundException;
import selenium.exceptions.WrongCredentialsException;
import util.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ScaSeleniumWriter {
    private static final String SAVE_MITIGATION_BUTTON_XPATH = "//button[@data-automation-id='scaApplicationLicenses-MitigationSuccessPopup-ButtonCancel']";
    private static final String LICENSE_MITIGATION_ACTIONS_ID = "licenseMitigationActionsId";
    private static final String SCA_LICENSE_MITIGATION_COMMENT_FIELD_ID = "scaLicenseMitigationCommentField";
    private static final String MITIGATIONS_APPLY_BUTTON_ID = "scaLicenseMitigationApplyBtn";
    private static final String MITIGATION_SAVE_BUTTON_ID = "save-button";
    private static final String SCA_LICENSES_FILTER_ID = "scaFilterTextLicenses";
    private static final int MAX_ATTEMPTS = 10;
    public static final String EMPTY_RESULTS_MESSAGE = "Your query did not produce any results. Try using fewer filters or changing your search criteria.";
    private WebDriverWrapper webDriverWrapper;
    private String veracodeUsername;
    private String veracodePassword;

    public Map<MitigationProposal, MitigationResult> applyMitigations(List<MitigationProposal> mitigationProposals,
                                                                      String veracodeUsername,
                                                                      String veracodePassword) throws WrongCredentialsException {

        this.webDriverWrapper = new WebDriverWrapper();
        this.veracodeUsername = veracodeUsername;
        this.veracodePassword = veracodePassword;
        if (WebDriverProvider.IS_HEADLESS) {
            System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE, "true");
            System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");
        }
        try {
            ScaSeleniumWrapper.loginToPlatform(veracodeUsername, veracodePassword, webDriverWrapper);
            return mitigateAllFindings(mitigationProposals);
        } catch (TimeoutException tme) {
            throw new RuntimeException(tme);
        } finally {
            webDriverWrapper.quit();
        }
    }

    private Map<MitigationProposal, MitigationResult> mitigateAllFindings(
            List<MitigationProposal> mitigationProposals) throws WrongCredentialsException {
        Map<MitigationProposal, MitigationResult> findingsMitigated = new HashMap<>();
        for (MitigationProposal mitigationProposal : mitigationProposals) {
            findingsMitigated.put(mitigationProposal, mitigateSingleFinding(mitigationProposal, 0));
        }
        return findingsMitigated;
    }

    private MitigationResult mitigateSingleFinding(MitigationProposal mitigationProposal, int attempt) throws WrongCredentialsException {
        try {
            if (!ScaSeleniumWrapper.isLoggedIn(webDriverWrapper)) {
                ScaSeleniumWrapper.loginToPlatform(veracodeUsername, veracodePassword, webDriverWrapper);
            }
            webDriverWrapper.openUrl(ScaSeleniumWrapper.APPLICATION_BASE_URL +
                    mitigationProposal.applicationProfile().applicationProfileUrl());
            if (ScaSeleniumWrapper.tryOpenScaPage(webDriverWrapper)) {
                ScaSeleniumWrapper.switchToLicensesPage(webDriverWrapper);
                findLicenseFinding(mitigationProposal.licenseFinding());
                boolean hasProposed = proposeMitigationIfNecessary(mitigationProposal);
                if (acceptMitigationIfNecessary(mitigationProposal) || hasProposed) {
                    return null;
                }
                return new MitigationResult(HttpCodes.HTTP_NO_CHANGES,
                        "Mitigation of the same type is already accepted for this finding");
            }
            return new MitigationResult(HttpCodes.HTTP_BAD_REQUEST, "Unable to open SCA page, ensure that the user being used has access to the application profile and is able to view SCA results");
        } catch (LibraryNotFoundException e) {
            return new MitigationResult(HttpCodes.HTTP_BAD_REQUEST, "Library not found: " + mitigationProposal);
        } catch (TimeoutException e) {
            if (attempt > MAX_ATTEMPTS) {
                return new MitigationResult(HttpCodes.INTERNAL_SERVER_ERROR, "Timeout: " + e.getMessage());
            } else {
                TimeUtils.sleepFor(1);
                return mitigateSingleFinding(mitigationProposal, attempt + 1);
            }
        }
    }

    private void findLicenseFinding(LicenseFinding licenseFinding) throws TimeoutException {
        filterLicense( "Component Filename", licenseFinding.componentFileName());
        filterLicense( "License", licenseFinding.licenseName());
    }

    private void filterLicense(String filterField, String filterValue) throws TimeoutException {
        ScaSeleniumWrapper.startLicenseFilter(webDriverWrapper, filterField);
        webDriverWrapper.getElement(By.id(SCA_LICENSES_FILTER_ID))
                .ifPresent(webElement -> webElement.sendKeys(filterValue));
        webDriverWrapper.clickElement(By.id(ScaSeleniumWrapper.SCA_FILTER_ACTIVATE_BUTTON_ID));
        ScaSeleniumWrapper.waitForLicenseResultsToLoad(webDriverWrapper);
    }

    private boolean proposeMitigationIfNecessary(MitigationProposal mitigationProposal) throws TimeoutException, LibraryNotFoundException {
        if (isEmptyQuery()) {
            throw new LibraryNotFoundException();
        }
        String mitigationTypeAsText = "Mitigate as " + mitigationProposal.mitigationType().getAsText();
        if (hasProposedThisTypeOfMitigation(mitigationTypeAsText)) {
            return false;
        }
        webDriverWrapper.getElement(By.id(ScaSeleniumWrapper.SCA_LICENSES_TABLE_ID))
                .map(licensesTable -> licensesTable.findElement(By.xpath(".//tr")))
                .map(licensesTable -> licensesTable.findElement(By.xpath(".//input")))
                .ifPresent(WebElement::click);

        webDriverWrapper.selectOptionByVisibleText(By.id(LICENSE_MITIGATION_ACTIONS_ID),
                mitigationTypeAsText);
        webDriverWrapper.clickElement(By.id(MITIGATIONS_APPLY_BUTTON_ID));
        webDriverWrapper.waitForElementPresent(By.id(SCA_LICENSE_MITIGATION_COMMENT_FIELD_ID));

        saveMitigation(mitigationProposal);
        return true;
    }

    private boolean isEmptyQuery() {
        return webDriverWrapper.getElement(By.id(ScaSeleniumWrapper.SCA_LICENSES_TABLE_ID))
                .map(table -> table.findElements(By.xpath(".//tr")))
                .map(tableRows -> tableRows.get(tableRows.size() - 1))
                .map(tableRow -> tableRow.findElements(By.xpath(".//td")))
                .map(tableColumn -> tableColumn.get(tableColumn.size() - 1))
                .map(WebElement::getText)
                .map(EMPTY_RESULTS_MESSAGE::equals)
                .orElse(true);
    }

    private boolean hasProposedThisTypeOfMitigation(String mitigationTypeAsText) {
        return webDriverWrapper.getElement(By.id(ScaSeleniumWrapper.SCA_LICENSES_TABLE_ID))
                .map(table -> table.findElements(By.xpath(".//tr")))
                .map(tableRows -> tableRows.get(tableRows.size() - 1))
                .map(tableRow -> tableRow.findElements(By.xpath(".//td")))
                .map(tableColumn -> tableColumn.get(tableColumn.size() - 1))
                .map(WebElement::getText)
                .map(mitigationStatus -> mitigationStatus.startsWith(mitigationTypeAsText))
                .orElse(false);
    }

    private boolean acceptMitigationIfNecessary(MitigationProposal mitigationProposal) throws TimeoutException {
        if (isAcceptedMitigation()) {
            return false;
        }
        webDriverWrapper.selectOptionByVisibleText(By.id(LICENSE_MITIGATION_ACTIONS_ID), "Approve Mitigation");
        webDriverWrapper.clickElement(By.id(MITIGATIONS_APPLY_BUTTON_ID));
        saveMitigation(mitigationProposal);
        return true;
    }

    private boolean isAcceptedMitigation() {
        return webDriverWrapper.getElement(By.id(ScaSeleniumWrapper.SCA_LICENSES_TABLE_ID))
                .map(table -> table.findElements(By.xpath(".//tr")))
                .map(tableRows -> tableRows.get(tableRows.size() - 1))
                .map(tableRow -> tableRow.findElements(By.xpath(".//td")))
                .map(tableColumn -> tableColumn.get(tableColumn.size() - 1))
                .map(WebElement::getText)
                .map(mitigationStatus -> !mitigationStatus.endsWith("(Proposed)"))
                .orElse(false);
    }

    private void saveMitigation( MitigationProposal mitigationProposal) throws TimeoutException {
        webDriverWrapper.getElement(By.id(SCA_LICENSE_MITIGATION_COMMENT_FIELD_ID))
                .ifPresent(webElement -> webElement.sendKeys(mitigationProposal.mitigationText()));
        webDriverWrapper.clickElement(By.id(MITIGATION_SAVE_BUTTON_ID));
        webDriverWrapper.waitForElementPresent(By.xpath(SAVE_MITIGATION_BUTTON_XPATH));
        webDriverWrapper.clickElement(By.xpath(SAVE_MITIGATION_BUTTON_XPATH));
        ScaSeleniumWrapper.waitForLicenseResultsToLoad(webDriverWrapper);
    }
}
