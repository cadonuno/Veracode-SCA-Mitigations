package selenium;

import api.HttpCodes;
import api.records.MitigationResult;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import scaresults.LicenseFinding;
import scaresults.MitigationProposal;
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
    private WebDriverWrapper webDriverWrapper;

    public Map<MitigationProposal, MitigationResult> applyMitigations(List<MitigationProposal> mitigationProposals,
                                                                      String veracodeUsername,
                                                                      String veracodePassword) {

        this.webDriverWrapper = new WebDriverWrapper();
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

    private HashMap<MitigationProposal, MitigationResult> mitigateAllFindings(
            List<MitigationProposal> mitigationProposals) {
        return mitigationProposals.stream()
                .collect(HashMap::new, (hashMap, mitigationProposal) ->
                                hashMap.put(mitigationProposal, mitigateSingleFinding(mitigationProposal, 0)),
                        HashMap::putAll);
    }

    private MitigationResult mitigateSingleFinding(MitigationProposal mitigationProposal, int attempt) {
        try {
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
            return new MitigationResult(HttpCodes.HTTP_BAD_REQUEST, "Mitigation not found");
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

    private boolean proposeMitigationIfNecessary(MitigationProposal mitigationProposal) throws TimeoutException {
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
