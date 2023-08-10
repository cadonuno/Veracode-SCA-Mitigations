package api;

import api.records.*;
import api.records.tomitigate.LicenseToMitigate;
import api.records.tomitigate.ToMitigationBase;
import api.records.tomitigate.VulnerabilityToMitigate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Strings;
import scaresults.finding.LicenseFinding;
import scaresults.finding.VulnerabilityFinding;
import scaresults.proposal.LicenseMitigationProposal;
import scaresults.proposal.MitigationProposalBase;
import scaresults.proposal.VulnerabilityMitigationProposal;
import scaresults.types.LicenseMitigationTypeEnum;
import scaresults.types.TSRVTechniqueEnum;
import scaresults.types.VulnerabilityMitigationTypeEnum;
import selenium.ScaSeleniumWriter;
import selenium.exceptions.WrongCredentialsException;
import util.ApiCredentials;
import util.EmptyUtils;
import util.apihandlers.ApiCaller;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONValue;

public class ScaMitigationServlet extends HttpServlet {

    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid Credentials";
    private static final String ERROR_EMPTY_LISTS = "At least one License or Vulnerability to mitigate needs to be provided";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"application/json".equals(request.getContentType())) {
            response.setStatus(HttpCodes.HTTP_BAD_REQUEST);
            return;
        }
        ScaMitigationRequest scaMitigationRequest = readParametersFromInput(request);
        ParameterValidation parameterValidation = validateParameters(scaMitigationRequest);
        if (parameterValidation.hasErrors()) {
            writeErrorsToOutputStream(response, parameterValidation.getErrors());
            return;
        }
        performMitigationFromRequest(scaMitigationRequest, response);
    }

    private ScaMitigationRequest readParametersFromInput(HttpServletRequest request) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        return mapper.readValue(request.getInputStream(), ScaMitigationRequest.class);
    }

    private void performMitigationFromRequest(ScaMitigationRequest scaMitigationRequest,
                                              HttpServletResponse response) throws IOException {
        try {
            handleExecutionResults(response,
                    new ScaSeleniumWriter()
                            .applyMitigations(
                                    Stream.concat(buildVulnerabilityMitigationProposals(scaMitigationRequest).stream(),
                                                    buildLicenseMitigationProposals(scaMitigationRequest).stream())
                                            .collect(Collectors.toList()),
                                    scaMitigationRequest.getVeracodeUsername(),
                                    scaMitigationRequest.getVeracodePassword()));
        } catch (WrongCredentialsException e) {
            writeErrorsToOutputStream(response, new HashMap<String, String>() {{
                put(RequestParameters.VERACODE_USERNAME, INVALID_CREDENTIALS_MESSAGE);
                put(RequestParameters.VERACODE_PASSWORD, INVALID_CREDENTIALS_MESSAGE);
            }});
        }
    }

    private void handleExecutionResults(HttpServletResponse response, Map<MitigationProposalBase, MitigationResult> mitigationResults) throws IOException {
        if (hasOneSuccess(mitigationResults)) {
            outputSuccess(response, mitigationResults);
        } else if (hasOneIssueOfType(mitigationResults, HttpCodes.INTERNAL_SERVER_ERROR)) {
            outputFailure(response, mitigationResults);
        } else if (hasOneIssueOfType(mitigationResults, HttpCodes.HTTP_BAD_REQUEST)) {
            outputBadRequest(response, mitigationResults);
        } else {
            outputNothingToChange(response, mitigationResults);
        }
    }

    private boolean hasOneIssueOfType(Map<MitigationProposalBase, MitigationResult> mitigationResults, int httpBadRequest) {
        return mitigationResults.values().stream().anyMatch(
                mitigationResult -> mitigationResult.httpCode() == httpBadRequest);
    }

    private void outputBadRequest(HttpServletResponse response, Map<MitigationProposalBase, MitigationResult> mitigationResults) throws IOException {
        response.setStatus(HttpCodes.HTTP_BAD_REQUEST);
        outputMitigationResults(response, "Unable to find some libraries", mitigationResults);
    }

    private boolean hasOneSuccess(Map<MitigationProposalBase, MitigationResult> mitigationResults) {
        return mitigationResults.values().stream().anyMatch(Objects::isNull);
    }

    private void outputFailure(HttpServletResponse response,
                               Map<MitigationProposalBase, MitigationResult> mitigationResults) throws IOException {
        response.setStatus(HttpCodes.INTERNAL_SERVER_ERROR);
        outputMitigationResults(response, "Issue detected", mitigationResults);
    }

    private void outputSuccess(HttpServletResponse response,
                               Map<MitigationProposalBase, MitigationResult> mitigationResults) throws IOException {
        response.setStatus(HttpCodes.HTTP_SUCCESS);
        outputMitigationResults(response, "success", mitigationResults);
    }

    private void outputNothingToChange(HttpServletResponse response,
                                       Map<MitigationProposalBase, MitigationResult> mitigationResults) throws IOException {
        response.setStatus(HttpCodes.HTTP_SUCCESS);
        outputMitigationResults(response, "Nothing to Change", mitigationResults);
    }

    private void outputMitigationResults(
            HttpServletResponse response, String status,
            Map<MitigationProposalBase, MitigationResult> mitigationResults) throws IOException {
        StringBuilder stringBuilder = new StringBuilder("{");
        appendAttributeToBuilder(stringBuilder, "status", status, 2, false);
        appendBuilderLine(stringBuilder, addQuotesToJsonValue("elements") + ": [", 2);
        boolean isFirst = true;
        for (Map.Entry<MitigationProposalBase, MitigationResult> entry : mitigationResults.entrySet()) {
            MitigationProposalBase mitigationProposal = entry.getKey();
            MitigationResult mitigationResult = entry.getValue();
            if (!isFirst) {
                stringBuilder.append(",");
            }
            appendBuilderLine(stringBuilder, "{", 2);
            appendAttributeToBuilder(stringBuilder, "ApplicationId",
                    mitigationProposal.applicationProfile().applicationId(), 4, false);
            appendAttributeToBuilder(stringBuilder, "ApplicationName",
                    mitigationProposal.applicationProfile().applicationName(), 4, false);
            if (mitigationProposal instanceof LicenseMitigationProposal) {
                outputMitigationResultsForLicense(stringBuilder, mitigationProposal);
            } else {
                outputMitigationResultsForVulnerability(stringBuilder, mitigationProposal);
            }
            appendAttributeToBuilder(stringBuilder, "Status",
                    (mitigationResult == null
                            ? "Mitigation successfully " +  (mitigationProposal.acceptanceText() == null ? "proposed" : "approved")
                            : mitigationResult.message()), 4, true);
            appendBuilderLine(stringBuilder, "}", 2);
            isFirst = false;
        }
        stringBuilder.append("]");
        appendBuilderLine(stringBuilder, "}", 0);
        response.getOutputStream().print(stringBuilder.toString());
    }

    private void outputMitigationResultsForVulnerability(StringBuilder stringBuilder, MitigationProposalBase mitigationProposal) {
        VulnerabilityMitigationProposal vulnerabilityMitigationProposal = (VulnerabilityMitigationProposal) mitigationProposal;
        appendAttributeToBuilder(stringBuilder, "ComponentFileName",
                vulnerabilityMitigationProposal.getVulnerabilityFinding().getComponentFileName(), 4, false);
        appendAttributeToBuilder(stringBuilder, "VulnerabilityId",
                vulnerabilityMitigationProposal.getVulnerabilityFinding().getVulnerabilityId(), 4, false);
        appendAttributeToBuilder(stringBuilder, "MitigationType",
                vulnerabilityMitigationProposal.getMitigationType().getAsText(), 4, false);
        if (vulnerabilityMitigationProposal.getTSRV() == null) {
            appendAttributeToBuilder(stringBuilder, "MitigationText",
                    mitigationProposal.mitigationText(), 4, false);
        } else {
            appendBuilderLine(stringBuilder, "\"TSRV\": {", 4);
            appendAttributeToBuilder(stringBuilder, "Technique",
                    vulnerabilityMitigationProposal.getTSRV().technique().name(), 6, false);
            appendAttributeToBuilder(stringBuilder, "Specifics",
                    vulnerabilityMitigationProposal.getTSRV().specifics(), 6, false);
            appendAttributeToBuilder(stringBuilder, "RemainingRisk",
                    vulnerabilityMitigationProposal.getTSRV().remainingRisk(), 6, false);
            appendAttributeToBuilder(stringBuilder, "Verification",
                    vulnerabilityMitigationProposal.getTSRV().verification(), 6, true);
            appendBuilderLine(stringBuilder, "},", 4);
            appendAttributeToBuilder(stringBuilder, "FindingType",
                    "Vulnerability", 4, false);
        }
    }

    private void outputMitigationResultsForLicense(StringBuilder stringBuilder, MitigationProposalBase mitigationProposal) {
        LicenseMitigationProposal licenseMitigationProposal = (LicenseMitigationProposal) mitigationProposal;
        appendAttributeToBuilder(stringBuilder, "LicenseName",
                licenseMitigationProposal.licenseFinding().licenseName(), 4, false);
        appendAttributeToBuilder(stringBuilder, "ComponentFileName",
                licenseMitigationProposal.licenseFinding().componentFileName(), 4, false);
        appendAttributeToBuilder(stringBuilder, "MitigationType",
                licenseMitigationProposal.mitigationType().getAsText(), 4, false);
        appendAttributeToBuilder(stringBuilder, "MitigationText",
                mitigationProposal.mitigationText(), 4, false);
        appendAttributeToBuilder(stringBuilder, "FindingType",
                "License Risk", 4, false);
    }

    private void appendAttributeToBuilder(StringBuilder stringBuilder, String attributeName,
                                          String attributeValue, int level, boolean isLast) {
        appendBuilderLine(stringBuilder, addQuotesToJsonValue(attributeName) + ": " +
                addQuotesToJsonValue(JSONValue.escape(attributeValue)) + (isLast ? "" : ","), level);
    }

    private String addQuotesToJsonValue(String aValue) {
        return "\"" + aValue + "\"";
    }

    private void appendBuilderLine(StringBuilder stringBuilder, String stringToAppend, int level) {
        stringBuilder.append("\n").append(Strings.repeat(" ", level)).append(stringToAppend);
    }

    private void writeErrorsToOutputStream(HttpServletResponse response, Map<String, String> parameterErrors) throws IOException {
        response.setStatus(HttpCodes.HTTP_BAD_REQUEST);
        ServletOutputStream outputStream = response.getOutputStream();
        StringBuilder stringBuilder = new StringBuilder();
        appendBuilderLine(stringBuilder, "{", 0);
        appendBuilderLine(stringBuilder, addQuotesToJsonValue("errorFields") + ": [", 2);
        List<Map.Entry<String, String>> entrySet = new ArrayList<>(parameterErrors.entrySet());
        int lastIndex = entrySet.size() - 1;
        for (int currentIndex = 0; currentIndex < entrySet.size(); currentIndex++) {
            appendBuilderLine(stringBuilder, "{", 2);
            Map.Entry<String, String> currentEntry = entrySet.get(currentIndex);
            appendBuilderLine(stringBuilder, addQuotesToJsonValue("field") + ": " +
                    addQuotesToJsonValue(JSONValue.escape(currentEntry.getKey())) + ",", 4);
            appendBuilderLine(stringBuilder, addQuotesToJsonValue("value") + ": " +
                    addQuotesToJsonValue(JSONValue.escape(currentEntry.getValue())), 4);
            appendBuilderLine(stringBuilder, "}" + (lastIndex == currentIndex ? "" : ","), 2);
        }

        appendBuilderLine(stringBuilder, "]", 2);
        appendBuilderLine(stringBuilder, "}", 0);
        outputStream.print(stringBuilder.toString());
    }

    private ParameterValidation validateParameters(ScaMitigationRequest scaMitigationRequest) {
        ParameterValidation parameterValidation = validateBaseParameters(scaMitigationRequest);
        if (!parameterValidation.hasErrors()) {
            ApiCredentials apiCredentials = new ApiCredentials(
                    scaMitigationRequest.getVeracodeApiId(),
                    scaMitigationRequest.getVeracodeApiKey());
            if (EmptyUtils.isNullOrEmptyList(scaMitigationRequest.getLicensesToMitigate())
                    && EmptyUtils.isNullOrEmptyList(scaMitigationRequest.getVulnerabilitiesToMitigate())) {
                parameterValidation.addError("LicensesToMitigate", ERROR_EMPTY_LISTS);
                parameterValidation.addError("VulnerabilitiesToMitigate", ERROR_EMPTY_LISTS);
            } else {
                validateLicensesToMitigate(parameterValidation,
                        apiCredentials,
                        scaMitigationRequest.getLicensesToMitigate());
                validateVulnerabilitiesToMitigate(parameterValidation,
                        apiCredentials,
                        scaMitigationRequest.getVulnerabilitiesToMitigate());
            }
        }
        return parameterValidation;
    }

    private ParameterValidation validateBaseParameters(ScaMitigationRequest scaMitigationRequest) {
        ParameterValidation parameterValidation = new ParameterValidation();
        checkForNullOrEmptyParameter(parameterValidation, scaMitigationRequest.getVeracodeApiId(),
                RequestParameters.API_ID);
        checkForNullOrEmptyParameter(parameterValidation, scaMitigationRequest.getVeracodeApiKey(),
                RequestParameters.API_KEY);
        checkForNullOrEmptyParameter(parameterValidation, scaMitigationRequest.getVeracodePassword(),
                RequestParameters.VERACODE_PASSWORD);
        checkForNullOrEmptyParameter(parameterValidation, scaMitigationRequest.getVeracodeUsername(),
                RequestParameters.VERACODE_USERNAME);
        return parameterValidation;
    }

    private void validateLicensesToMitigate(ParameterValidation parameterValidation,
                                            ApiCredentials apiCredentials,
                                            List<LicenseToMitigate> licensesToMitigate) {
        if (licensesToMitigate == null) {
            return;
        }
        licensesToMitigate.forEach(licenseToMitigate -> {
            checkForNullOrEmptyParameter(parameterValidation, licenseToMitigate.getComponentFileName(),
                    RequestParameters.COMPONENT_FILE_NAME);
            checkForNullOrEmptyParameter(parameterValidation, licenseToMitigate.getMitigationText(),
                    RequestParameters.MITIGATION_TEXT);
            checkForNullOrEmptyParameter(parameterValidation, licenseToMitigate.getApplicationId(),
                    RequestParameters.APPLICATION_ID);
            checkForNullOrEmptyParameter(parameterValidation, licenseToMitigate.getLicenseName(),
                    RequestParameters.LICENSE_NAME);
            validateApplicationId(licenseToMitigate, apiCredentials, parameterValidation);
            checkIfInvalidOrNullMitigationType(parameterValidation, licenseToMitigate.getMitigationType());
        });
    }

    private void validateVulnerabilitiesToMitigate(ParameterValidation parameterValidation,
                                                   ApiCredentials apiCredentials,
                                                   List<VulnerabilityToMitigate> vulnerabilitiesToMitigate) {
        if (vulnerabilitiesToMitigate == null) {
            return;
        }
        vulnerabilitiesToMitigate.forEach(vulnerabilityToMitigate -> {
            checkForNullOrEmptyParameter(parameterValidation, vulnerabilityToMitigate.getComponentFileName(),
                    RequestParameters.COMPONENT_FILE_NAME);
            if (vulnerabilityToMitigate.getTSRV() != null) {
                checkIfInvalidOrNullTSRVTechnique(parameterValidation, vulnerabilityToMitigate.getTSRV().technique());
                checkForNullOrEmptyParameter(parameterValidation, vulnerabilityToMitigate.getTSRV().specifics(),
                        "Specifics");
                checkForNullOrEmptyParameter(parameterValidation, vulnerabilityToMitigate.getTSRV().remainingRisk(),
                        "RemainingRisk");
                checkForNullOrEmptyParameter(parameterValidation, vulnerabilityToMitigate.getTSRV().verification(),
                        "Verification");
            } else if (vulnerabilityToMitigate.getAcceptanceText() == null
                    && vulnerabilityToMitigate.getMitigationText() == null) {
                parameterValidation.addError("TSRV, MitigationText, or AcceptanceText", "Must be set");
            }
            checkForNullOrEmptyParameter(parameterValidation, vulnerabilityToMitigate.getApplicationId(),
                    RequestParameters.APPLICATION_ID);
            checkForNullOrEmptyParameter(parameterValidation, vulnerabilityToMitigate.getVulnerabilityId(),
                    RequestParameters.VULNERABILITY_ID);
            validateApplicationId(vulnerabilityToMitigate, apiCredentials, parameterValidation);
            checkIfInvalidOrNullMitigationType(parameterValidation, vulnerabilityToMitigate.getMitigationType());
        });
    }

    private void checkIfInvalidOrNullMitigationType(ParameterValidation parameterValidation,
                                                    LicenseMitigationTypeEnum mitigationType) {
        if (mitigationType == null) {
            parameterValidation.addError(RequestParameters.MITIGATION_TYPE, "Must not be null");
        } else if (mitigationType == LicenseMitigationTypeEnum.Invalid) {
            parameterValidation.addError(RequestParameters.MITIGATION_TYPE, "Is invalid");
        }
    }

    private void checkIfInvalidOrNullMitigationType(ParameterValidation parameterValidation,
                                                    VulnerabilityMitigationTypeEnum mitigationType) {
        if (mitigationType == null) {
            parameterValidation.addError(RequestParameters.MITIGATION_TYPE, "Must not be null");
        } else if (mitigationType == VulnerabilityMitigationTypeEnum.Invalid) {
            parameterValidation.addError(RequestParameters.MITIGATION_TYPE, "Is invalid");
        }
    }

    private void checkIfInvalidOrNullTSRVTechnique(ParameterValidation parameterValidation,
                                                   TSRVTechniqueEnum tsrvTechniqueEnum) {
        if (tsrvTechniqueEnum == null) {
            parameterValidation.addError("Technique", "Must not be null");
        } else if (tsrvTechniqueEnum == TSRVTechniqueEnum.Invalid) {
            parameterValidation.addError("Technique", "Is invalid");
        }
    }

    private void checkForNullOrEmptyParameter(ParameterValidation parameterValidation,
                                              String parameterValue, String parameterName) {
        if (parameterValue == null
                || "".equals(parameterValue.trim())) {
            parameterValidation.addError(parameterName, "Must not be null");
        }
    }

    private void validateApplicationId(ToMitigationBase toMitigate,
                                       ApiCredentials apiCredentials,
                                       ParameterValidation parameterValidation) {
        toMitigate.setApplicationToMitigate(
                ApiCaller.getApplicationById(toMitigate.getApplicationId(), apiCredentials));
        if (toMitigate.getApplicationToMitigate() == null) {
            parameterValidation.addError(toMitigate.getApplicationId(), "Application not found");
        }
    }

    private List<LicenseMitigationProposal> buildLicenseMitigationProposals(
            ScaMitigationRequest scaMitigationRequest) {
        return scaMitigationRequest.getLicensesToMitigate()
                .stream()
                .map(licenseToMitigate -> new LicenseMitigationProposal(
                        buildLicenseFinding(licenseToMitigate),
                        licenseToMitigate.getMitigationType(),
                        licenseToMitigate.getApplicationToMitigate(),
                        licenseToMitigate.getMitigationText(),
                        licenseToMitigate.getAcceptanceText()))
                .collect(Collectors.toList());
    }

    private List<VulnerabilityMitigationProposal> buildVulnerabilityMitigationProposals(
            ScaMitigationRequest scaMitigationRequest) {
        return scaMitigationRequest.getVulnerabilitiesToMitigate()
                .stream()
                .map(vulnerabilityToMitigate -> new VulnerabilityMitigationProposal(
                        buildVulnerabilityFinding(vulnerabilityToMitigate),
                        vulnerabilityToMitigate.getMitigationType(),
                        vulnerabilityToMitigate.getApplicationToMitigate(),
                        vulnerabilityToMitigate.getMitigationText(),
                        vulnerabilityToMitigate.getAcceptanceText(),
                        vulnerabilityToMitigate.getTSRV()))
                .collect(Collectors.toList());
    }

    private VulnerabilityFinding buildVulnerabilityFinding(VulnerabilityToMitigate vulnerabilityToMitigate) {
        return new VulnerabilityFinding(vulnerabilityToMitigate.getComponentFileName(),
                vulnerabilityToMitigate.getVulnerabilityId());
    }

    private LicenseFinding buildLicenseFinding(LicenseToMitigate licenseToMitigate) {
        return new LicenseFinding(licenseToMitigate.getLicenseName(),
                null, licenseToMitigate.getComponentFileName());
    }
}
