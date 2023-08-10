package selenium;

import api.HttpCodes;
import api.records.MitigationResult;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import scaresults.finding.LicenseFinding;
import scaresults.finding.VulnerabilityFinding;
import scaresults.proposal.LicenseMitigationProposal;
import scaresults.proposal.MitigationProposalBase;
import scaresults.proposal.VulnerabilityMitigationProposal;
import selenium.exceptions.LibraryNotFoundException;
import selenium.exceptions.UnableToProposeMitigationException;
import selenium.exceptions.WrongCredentialsException;
import util.EmptyUtils;
import util.TimeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ScaSeleniumWriter {
    private static final String SAVE_LICENSE_MITIGATION_BUTTON_XPATH = "//button[@data-automation-id='scaApplicationLicenses-MitigationSuccessPopup-ButtonCancel']";
    private static final String SAVE_VULNERABILITIES_MITIGATION_BUTTON_XPATH = "//div[@id='scaMitigationSuccessPopup']/div/div/div/button";
    private static final String LICENSE_MITIGATION_ACTIONS_ID = "licenseMitigationActionsId";
    private static final String VULNERABILITIES_MITIGATION_ACTIONS_ID = "mitigationActionsId";
    private static final String SCA_LICENSE_MITIGATION_COMMENT_FIELD_ID = "scaLicenseMitigationCommentField";
    private static final String SCA_VULNERABILITIES_MITIGATION_COMMENT_FIELD_ID = "scaMitigationCommentField";
    private static final String LICENSE_MITIGATIONS_APPLY_BUTTON_ID = "scaLicenseMitigationApplyBtn";
    private static final String VULNERABILITIES_APPLY_BUTTON_ID = "scaMitigationApplyBtn";
    private static final String LICENSE_MITIGATION_SAVE_BUTTON_ID = "save-button";
    private static final String VULNERABILITY_MITIGATION_SAVE_BUTTON_ID = "save";
    private static final String SCA_LICENSES_FILTER_ID = "scaFilterTextLicenses";
    private static final String SCA_VULNERABILITY_FILTER_ID = "scaFilterTextVulnerabilities";
    private static final int MAX_ATTEMPTS = 10;
    public static final String EMPTY_RESULTS_MESSAGE = "Your query did not produce any results. Try using fewer filters or changing your search criteria.";
    private static final String MITIGATION_TECHNIQUE_ID = "mitigationTechniqueId";
    private static final String MITIGATION_SPECIFICS_ID = "scaMitigationSpecifiesField";
    private static final String MITIGATION_REMAINING_RISKS_ID = "scaMitigationRemainingRiskField";
    private static final String MITIGATION_VERIFICATION_ID = "scaMitigationVerificationField";


    private WebDriverWrapper webDriverWrapper;
    private String veracodeUsername;
    private String veracodePassword;

    public Map<MitigationProposalBase, MitigationResult> applyMitigations(
            List<MitigationProposalBase> mitigationsToApply,
            String veracodeUsername, String veracodePassword) throws WrongCredentialsException {

        this.webDriverWrapper = new WebDriverWrapper();
        this.veracodeUsername = veracodeUsername;
        this.veracodePassword = veracodePassword;
        if (WebDriverProvider.IS_HEADLESS) {
            System.setProperty(FirefoxDriver.SystemProperty.DRIVER_USE_MARIONETTE, "true");
            System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");
        }
        try {
            ScaSeleniumWrapper.loginToPlatform(veracodeUsername, veracodePassword, webDriverWrapper);
            return mitigateAllFindings(mitigationsToApply);
        } catch (TimeoutException tme) {
            throw new RuntimeException(tme);
        } finally {
            webDriverWrapper.quit();
        }
    }

    private Map<MitigationProposalBase, MitigationResult> mitigateAllFindings(
            List<MitigationProposalBase> mitigationsToApply) throws WrongCredentialsException {
        Map<MitigationProposalBase, MitigationResult> findingsMitigated = new HashMap<>();
        for (MitigationProposalBase mitigationProposal : mitigationsToApply) {
            findingsMitigated.put(mitigationProposal, mitigateSingleFinding(mitigationProposal, 0));
        }
        return findingsMitigated;
    }

    private MitigationResult mitigateSingleFinding(MitigationProposalBase mitigationProposal, int attempt) throws WrongCredentialsException {
        try {
            if (!ScaSeleniumWrapper.isLoggedIn(webDriverWrapper)) {
                ScaSeleniumWrapper.loginToPlatform(veracodeUsername, veracodePassword, webDriverWrapper);
            }
            webDriverWrapper.openUrl(ScaSeleniumWrapper.APPLICATION_BASE_URL +
                    mitigationProposal.applicationProfile().applicationProfileUrl());
            if (ScaSeleniumWrapper.tryOpenScaPage(webDriverWrapper)) {
                if (mitigationProposal instanceof LicenseMitigationProposal) {
                    ScaSeleniumWrapper.switchToLicensesPage(webDriverWrapper);
                    findLicenseFinding(((LicenseMitigationProposal) mitigationProposal).licenseFinding());
                    boolean hasProposed = proposeLicenseMitigationIfNecessary(mitigationProposal);
                    if (acceptLicenseMitigationIfNecessary(mitigationProposal) || hasProposed) {
                        return null;
                    }
                } else {
                    ScaSeleniumWrapper.switchToVulnerabilitiesPage(webDriverWrapper);
                    findVulnerabilityFinding(((VulnerabilityMitigationProposal) mitigationProposal).getVulnerabilityFinding());
                    boolean hasProposed = proposeVulnerabilityMitigationIfNecessary((VulnerabilityMitigationProposal) mitigationProposal);
                    if (acceptVulnerabilityMitigationIfNecessary(mitigationProposal) || hasProposed) {
                        return null;
                    }
                }
                return new MitigationResult(HttpCodes.HTTP_NO_CHANGES,
                        "Mitigation of the same type is already " +
                                (EmptyUtils.isNullOrEmpty(mitigationProposal.acceptanceText()) ? "proposed" : "accepted") +
                                " for this finding");
            }
            return new MitigationResult(HttpCodes.HTTP_BAD_REQUEST, "Unable to open SCA page, ensure that the user being used has access to the application profile and is able to view SCA results");
        } catch (LibraryNotFoundException e) {
            return new MitigationResult(HttpCodes.HTTP_BAD_REQUEST, "Library not found: " + mitigationProposal);
        } catch (UnableToProposeMitigationException e) {
            return new MitigationResult(HttpCodes.HTTP_BAD_REQUEST, "Unable to propose mitigation for: " + mitigationProposal);
        } catch (TimeoutException e) {
            if (attempt > MAX_ATTEMPTS) {
                return new MitigationResult(HttpCodes.INTERNAL_SERVER_ERROR, "Timeout: " + e.getMessage());
            } else {
                TimeUtils.sleepFor(1);
                return mitigateSingleFinding(mitigationProposal, attempt + 1);
            }
        }
    }

    private void findVulnerabilityFinding(VulnerabilityFinding vulnerabilityFinding) throws TimeoutException {
        filterVulnerability("Vulnerability", vulnerabilityFinding.getVulnerabilityId());
        filterVulnerability("Component Filename", vulnerabilityFinding.getComponentFileName());
    }

    private void filterVulnerability(String filterField, String filterValue) throws TimeoutException {
        ScaSeleniumWrapper.startVulnerabilityFilter(webDriverWrapper, filterField);
        webDriverWrapper.getElement(By.id(SCA_VULNERABILITY_FILTER_ID))
                .ifPresent(webElement -> webElement.sendKeys(filterValue));
        webDriverWrapper.clickElement(By.id(ScaSeleniumWrapper.SCA_VULNERABILITY_FILTER_ACTIVATE_BUTTON_ID));
        ScaSeleniumWrapper.waitForVulnerabilitiesResultsToLoad(webDriverWrapper);
    }

    private void findLicenseFinding(LicenseFinding licenseFinding) throws TimeoutException {
        filterLicense("Component Filename", licenseFinding.componentFileName());
        filterLicense("License", licenseFinding.licenseName());
    }

    private void filterLicense(String filterField, String filterValue) throws TimeoutException {
        ScaSeleniumWrapper.startLicenseFilter(webDriverWrapper, filterField);
        webDriverWrapper.getElement(By.id(SCA_LICENSES_FILTER_ID))
                .ifPresent(webElement -> webElement.sendKeys(filterValue));
        webDriverWrapper.clickElement(By.id(ScaSeleniumWrapper.SCA_LICENSE_FILTER_ACTIVATE_BUTTON_ID));
        ScaSeleniumWrapper.waitForLicenseResultsToLoad(webDriverWrapper);
    }

    private boolean proposeLicenseMitigationIfNecessary(MitigationProposalBase mitigationProposal) throws TimeoutException, LibraryNotFoundException {
        if (isEmptyQuery(ScaSeleniumWrapper.SCA_LICENSES_TABLE_ID)) {
            throw new LibraryNotFoundException();
        }
        String mitigationTypeAsText = getMitigationTypeAsText(mitigationProposal);
        if (hasProposedThisTypeOfMitigation(mitigationTypeAsText, ScaSeleniumWrapper.SCA_LICENSES_TABLE_ID)) {
            return false;
        }
        selectFirstElementFromTableIfNecessary(ScaSeleniumWrapper.SCA_LICENSES_TABLE_ID);

        webDriverWrapper.selectOptionByVisibleText(By.id(LICENSE_MITIGATION_ACTIONS_ID),
                mitigationTypeAsText);
        webDriverWrapper.clickElement(By.id(LICENSE_MITIGATIONS_APPLY_BUTTON_ID));
        webDriverWrapper.waitForElementPresent(By.id(SCA_LICENSE_MITIGATION_COMMENT_FIELD_ID));

        setRegularMitigationText(mitigationProposal.mitigationText(), SCA_LICENSE_MITIGATION_COMMENT_FIELD_ID);
        saveMitigation(SAVE_LICENSE_MITIGATION_BUTTON_XPATH, LICENSE_MITIGATION_SAVE_BUTTON_ID);
        ScaSeleniumWrapper.waitForLicenseResultsToLoad(webDriverWrapper);
        return true;
    }

    private boolean proposeVulnerabilityMitigationIfNecessary(VulnerabilityMitigationProposal mitigationProposal) throws TimeoutException, LibraryNotFoundException, UnableToProposeMitigationException {
        if (isEmptyQuery(ScaSeleniumWrapper.SCA_VULNERABILITIES_TABLE_ID)) {
            throw new LibraryNotFoundException();
        }
        String mitigationTypeAsText = getMitigationTypeAsText(mitigationProposal);
        if (hasProposedThisTypeOfMitigation(mitigationTypeAsText, ScaSeleniumWrapper.SCA_VULNERABILITIES_TABLE_ID)) {
            return false;
        }
        selectFirstElementFromTableIfNecessary(ScaSeleniumWrapper.SCA_VULNERABILITIES_TABLE_ID);
        int attemptCount = 0;
        while (attemptCount < 10) {
            proposeVulnerabilityMitigation(mitigationProposal, mitigationTypeAsText);
            if (hasProposedThisTypeOfMitigation(mitigationTypeAsText, ScaSeleniumWrapper.SCA_VULNERABILITIES_TABLE_ID)) {
                return true;
            }
            attemptCount++;
            TimeUtils.sleepFor(1);
        }
        throw new UnableToProposeMitigationException();
    }

    private void selectFirstElementFromTableIfNecessary(String scaResultsTableId) {
        webDriverWrapper.getElement(By.id(scaResultsTableId))
                .map(vulnerabilitiesTable -> vulnerabilitiesTable.findElement(By.xpath(".//tr")))
                .map(vulnerabilitiesTable -> vulnerabilitiesTable.findElement(By.xpath(".//input")))
                .filter(element -> !element.isSelected())
                .ifPresent(WebElement::click);
    }

    private void proposeVulnerabilityMitigation(VulnerabilityMitigationProposal mitigationProposal, String mitigationTypeAsText) throws TimeoutException {
        webDriverWrapper.selectOptionByVisibleText(By.id(VULNERABILITIES_MITIGATION_ACTIONS_ID),
                mitigationTypeAsText);
        webDriverWrapper.clickElement(By.id(VULNERABILITIES_APPLY_BUTTON_ID));

        if (mitigationProposal.getTSRV() != null) {
            webDriverWrapper.waitForElementPresent(By.id(MITIGATION_TECHNIQUE_ID));
            setMitigationTextTSRV(mitigationProposal);
        } else {
            webDriverWrapper.waitForElementPresent(By.id(SCA_VULNERABILITIES_MITIGATION_COMMENT_FIELD_ID));
            setRegularMitigationText(mitigationProposal.mitigationText(), SCA_VULNERABILITIES_MITIGATION_COMMENT_FIELD_ID);
        }
        saveMitigation(SAVE_VULNERABILITIES_MITIGATION_BUTTON_XPATH, VULNERABILITY_MITIGATION_SAVE_BUTTON_ID);
        ScaSeleniumWrapper.waitForVulnerabilitiesResultsToLoad(webDriverWrapper);
    }

    private void setMitigationTextTSRV(MitigationProposalBase mitigationProposal) throws TimeoutException {
        if (!(mitigationProposal instanceof VulnerabilityMitigationProposal)) {
            throw new IllegalStateException("Expecting Vulnerability Mitigation but found License Mitigation");
        }
        VulnerabilityMitigationProposal vulnerabilityMitigation = (VulnerabilityMitigationProposal) mitigationProposal;
        if (vulnerabilityMitigation.getTSRV() == null) {
            throw new IllegalStateException("TSRV is enabled and regular mitigation text was provided");
        }
        webDriverWrapper.selectOptionByIndex(By.id(MITIGATION_TECHNIQUE_ID),
                vulnerabilityMitigation.getTSRV().technique().getSelectorIndex());
        webDriverWrapper.getElement(By.id(MITIGATION_SPECIFICS_ID))
                .ifPresent(webElement -> webElement.sendKeys(vulnerabilityMitigation.getTSRV().specifics()));
        webDriverWrapper.getElement(By.id(MITIGATION_REMAINING_RISKS_ID))
                .ifPresent(webElement -> webElement.sendKeys(vulnerabilityMitigation.getTSRV().remainingRisk()));
        webDriverWrapper.getElement(By.id(MITIGATION_VERIFICATION_ID))
                .ifPresent(webElement -> webElement.sendKeys(vulnerabilityMitigation.getTSRV().verification()));
    }

    private void setRegularMitigationText(String mitigationText, String commentsFieldId) {
        if (mitigationText == null) {
            throw new IllegalStateException("Mitigation text was expected but TSRV was provided");
        }
        webDriverWrapper.getElement(By.id(commentsFieldId))
                .ifPresent(webElement -> webElement.sendKeys(mitigationText));
    }

    private String getMitigationTypeAsText(MitigationProposalBase mitigationProposal) {
        return mitigationProposal instanceof VulnerabilityMitigationProposal
                ? ((VulnerabilityMitigationProposal) mitigationProposal).getMitigationType().getAsText()
                : ((LicenseMitigationProposal) mitigationProposal).mitigationType().getAsText();
    }

    private boolean isEmptyQuery(String tableToCheck) {
        return webDriverWrapper.getElement(By.id(tableToCheck))
                .map(table -> table.findElements(By.xpath(".//tr")))
                .map(tableRows -> tableRows.get(tableRows.size() - 1))
                .map(tableRow -> tableRow.findElements(By.xpath(".//td")))
                .map(tableColumn -> tableColumn.get(tableColumn.size() - 1))
                .map(WebElement::getText)
                .map(EMPTY_RESULTS_MESSAGE::equals)
                .orElse(true);
    }

    private boolean hasProposedThisTypeOfMitigation(String mitigationTypeAsText, String tableToCheck) {
        return webDriverWrapper.getElement(By.id(tableToCheck))
                .map(table -> table.findElements(By.xpath(".//tr")))
                .map(tableRows -> tableRows.get(tableRows.size() - 1))
                .map(tableRow -> tableRow.findElements(By.xpath(".//td")))
                .map(tableColumn -> tableColumn.get(tableColumn.size() - 1))
                .map(WebElement::getText)
                .map(mitigationStatus -> mitigationStatus.startsWith(mitigationTypeAsText))
                .orElse(false);
    }

    private boolean acceptLicenseMitigationIfNecessary(MitigationProposalBase mitigationProposal) throws TimeoutException {
        if (EmptyUtils.isNullOrEmpty(mitigationProposal.acceptanceText()) || isAcceptedMitigation(ScaSeleniumWrapper.SCA_LICENSES_TABLE_ID)) {
            return false;
        }
        selectFirstElementFromTableIfNecessary(ScaSeleniumWrapper.SCA_LICENSES_TABLE_ID);
        webDriverWrapper.selectOptionByVisibleText(By.id(LICENSE_MITIGATION_ACTIONS_ID), "Approve Mitigation");
        webDriverWrapper.clickElement(By.id(LICENSE_MITIGATIONS_APPLY_BUTTON_ID));
        setRegularMitigationText(mitigationProposal.acceptanceText(), SCA_LICENSE_MITIGATION_COMMENT_FIELD_ID);
        saveMitigation(SAVE_LICENSE_MITIGATION_BUTTON_XPATH, LICENSE_MITIGATION_SAVE_BUTTON_ID);
        ScaSeleniumWrapper.waitForLicenseResultsToLoad(webDriverWrapper);
        return true;
    }

    private boolean acceptVulnerabilityMitigationIfNecessary(MitigationProposalBase mitigationProposal) throws TimeoutException {
        if (EmptyUtils.isNullOrEmpty(mitigationProposal.acceptanceText()) || isAcceptedMitigation(ScaSeleniumWrapper.SCA_VULNERABILITIES_TABLE_ID)) {
            return false;
        }
        selectFirstElementFromTableIfNecessary(ScaSeleniumWrapper.SCA_VULNERABILITIES_TABLE_ID);
        webDriverWrapper.selectOptionByVisibleText(By.id(VULNERABILITIES_MITIGATION_ACTIONS_ID), "Approve Mitigation");
        webDriverWrapper.clickElement(By.id(VULNERABILITIES_APPLY_BUTTON_ID));
        setRegularMitigationText(mitigationProposal.acceptanceText(), SCA_VULNERABILITIES_MITIGATION_COMMENT_FIELD_ID);
        saveMitigation(SAVE_VULNERABILITIES_MITIGATION_BUTTON_XPATH, VULNERABILITY_MITIGATION_SAVE_BUTTON_ID);
        ScaSeleniumWrapper.waitForVulnerabilitiesResultsToLoad(webDriverWrapper);
        return true;
    }

    private boolean isAcceptedMitigation(String tableId) {
        return webDriverWrapper.getElement(By.id(tableId))
                .map(table -> table.findElements(By.xpath(".//tr")))
                .map(tableRows -> tableRows.get(tableRows.size() - 1))
                .map(tableRow -> tableRow.findElements(By.xpath(".//td")))
                .map(tableColumn -> tableColumn.get(tableColumn.size() - 1))
                .map(WebElement::getText)
                .map(mitigationStatus -> !mitigationStatus.endsWith("(Proposed)"))
                .orElse(false);
    }

    private void saveMitigation(String saveMitigationButtonXpath, String mitigationSaveButtonId) throws TimeoutException {
        webDriverWrapper.clickElement(By.id(mitigationSaveButtonId));
        webDriverWrapper.waitForElementPresent(By.xpath(saveMitigationButtonXpath));
        webDriverWrapper.clickElement(By.xpath(saveMitigationButtonXpath));
    }
}
