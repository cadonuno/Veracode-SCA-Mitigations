package api;

import api.records.LicenseMitigationRequest;
import api.records.LicenseToMitigate;
import api.records.MitigationResult;
import com.google.common.base.Strings;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import scaresults.LicenseFinding;
import scaresults.MitigationProposal;
import scaresults.MitigationTypeEnum;
import selenium.ScaSeleniumWriter;
import selenium.exceptions.WrongCredentialsException;
import util.ApiCredentials;
import util.apihandlers.ApiCaller;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.json.simple.JSONValue;

public class LicenseMitigationServlet extends HttpServlet {

    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid Credentials";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LicenseMitigationRequest licenseMitigationRequest = readParametersFromInput(request);
        ParameterValidation parameterValidation = validateParameters(licenseMitigationRequest);
        if (parameterValidation.hasErrors()) {
            writeErrorsToOutputStream(response, parameterValidation.getErrors());
            return;
        }
        performMitigationFromRequest(licenseMitigationRequest, response);
    }

    private LicenseMitigationRequest readParametersFromInput(HttpServletRequest request) throws IOException {
        return new ObjectMapper().readValue(request.getInputStream(), LicenseMitigationRequest.class);
    }

    private void performMitigationFromRequest(LicenseMitigationRequest licenseMitigationRequest,
                                              HttpServletResponse response) throws IOException {
        List<MitigationProposal> mitigationsToApply = buildMitigationProposals(licenseMitigationRequest);
        try {
            handleExecutionResults(response, new ScaSeleniumWriter().applyMitigations(mitigationsToApply,
                    licenseMitigationRequest.getVeracodeUsername(),
                    licenseMitigationRequest.getVeracodePassword()));
        } catch (WrongCredentialsException e) {
            writeErrorsToOutputStream(response, new HashMap<String, String>() {{
                put(RequestParameters.VERACODE_USERNAME, INVALID_CREDENTIALS_MESSAGE);
                put(RequestParameters.VERACODE_PASSWORD, INVALID_CREDENTIALS_MESSAGE);
            }});
        }
    }

    private void handleExecutionResults(HttpServletResponse response, Map<MitigationProposal, MitigationResult> mitigationResults) throws IOException {
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

    private boolean hasOneIssueOfType(Map<MitigationProposal, MitigationResult> mitigationResults, int httpBadRequest) {
        return mitigationResults.values().stream().anyMatch(
                mitigationResult -> mitigationResult.httpCode() == httpBadRequest);
    }

    private void outputBadRequest(HttpServletResponse response, Map<MitigationProposal, MitigationResult> mitigationResults) throws IOException {
        response.setStatus(HttpCodes.HTTP_BAD_REQUEST);
        outputMitigationResults(response, "Unable to find some libraries", mitigationResults);
    }

    private boolean hasOneSuccess(Map<MitigationProposal, MitigationResult> mitigationResults) {
        return mitigationResults.values().stream().anyMatch(Objects::isNull);
    }

    private void outputFailure(HttpServletResponse response,
                               Map<MitigationProposal, MitigationResult> mitigationResults) throws IOException {
        response.setStatus(HttpCodes.INTERNAL_SERVER_ERROR);
        outputMitigationResults(response, "Issue detected", mitigationResults);
    }

    private void outputSuccess(HttpServletResponse response,
                               Map<MitigationProposal, MitigationResult> mitigationResults) throws IOException {
        response.setStatus(HttpCodes.HTTP_SUCCESS);
        outputMitigationResults(response, "success", mitigationResults);
    }

    private void outputNothingToChange(HttpServletResponse response,
                                       Map<MitigationProposal, MitigationResult> mitigationResults) throws IOException {
        response.setStatus(HttpCodes.HTTP_SUCCESS);
        outputMitigationResults(response, "Nothing to Change", mitigationResults);
    }

    private void outputMitigationResults(
            HttpServletResponse response, String status,
            Map<MitigationProposal, MitigationResult> mitigationResults) throws IOException {
        StringBuilder stringBuilder = new StringBuilder("{");
        appendAttributeToBuilder(stringBuilder, "status", status, 2, false);
        appendBuilderLine(stringBuilder, addQuotesToJsonValue("licenses") + ": [", 2);
        boolean isFirst = true;
        for (Map.Entry<MitigationProposal, MitigationResult> entry : mitigationResults.entrySet()) {
            MitigationProposal mitigationProposal = entry.getKey();
            MitigationResult mitigationResult = entry.getValue();
            if (!isFirst) {
                stringBuilder.append(",");
            }
            appendBuilderLine(stringBuilder, "{", 2);
            appendAttributeToBuilder(stringBuilder, "applicationId",
                    mitigationProposal.applicationProfile().applicationId(), 4, false);
            appendAttributeToBuilder(stringBuilder, "applicationName",
                    mitigationProposal.applicationProfile().applicationName(), 4, false);
            appendAttributeToBuilder(stringBuilder, "licenseName",
                    mitigationProposal.licenseFinding().licenseName(), 4, false);
            appendAttributeToBuilder(stringBuilder, "componentFileName",
                    mitigationProposal.licenseFinding().componentFileName(), 4, false);
            appendAttributeToBuilder(stringBuilder, "mitigationType",
                    mitigationProposal.mitigationType().getAsText(), 4, false);
            appendAttributeToBuilder(stringBuilder, "mitigationText",
                    mitigationProposal.mitigationText(), 4, false);
            appendAttributeToBuilder(stringBuilder, "status",
                    (mitigationResult == null ? "Mitigation successfully approved" : mitigationResult.message()), 4, true);
            appendBuilderLine(stringBuilder, "}", 2);
            isFirst = false;
        }
        stringBuilder.append("]");
        appendBuilderLine(stringBuilder, "}", 0);
        response.getOutputStream().print(stringBuilder.toString());
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
            appendAttributeToBuilder(stringBuilder, entrySet.get(currentIndex).getKey(),
                    entrySet.get(currentIndex).getValue(), 2, lastIndex == currentIndex);
        }

        parameterErrors.forEach((key, value) -> writeParameter(stringBuilder, key, value));
        appendBuilderLine(stringBuilder, "]", 2);
        appendBuilderLine(stringBuilder, "}", 0);
        outputStream.print(stringBuilder.toString());
    }

    private void writeParameter(StringBuilder stringBuilder, String key, String value) {
        appendBuilderLine(stringBuilder, addQuotesToJsonValue(key) + ": " + addQuotesToJsonValue(value), 4);
    }

    private ParameterValidation validateParameters(LicenseMitigationRequest licenseMitigationRequest) {
        ParameterValidation parameterValidation = validateBaseParameters(licenseMitigationRequest);
        if (!parameterValidation.hasErrors()) {
            validateLicensesToMitigate(parameterValidation,
                    new ApiCredentials(
                            licenseMitigationRequest.getVeracodeApiId(),
                            licenseMitigationRequest.getVeracodeApiKey()),
                    licenseMitigationRequest.getLicensesToMitigate());
        }
        return parameterValidation;
    }

    private ParameterValidation validateBaseParameters(LicenseMitigationRequest licenseMitigationRequest) {
        ParameterValidation parameterValidation = new ParameterValidation();
        checkForNullOrEmptyParameter(parameterValidation, licenseMitigationRequest.getVeracodeApiId(),
                RequestParameters.API_ID);
        checkForNullOrEmptyParameter(parameterValidation, licenseMitigationRequest.getVeracodeApiKey(),
                RequestParameters.API_KEY);
        checkForNullOrEmptyParameter(parameterValidation, licenseMitigationRequest.getVeracodePassword(),
                RequestParameters.VERACODE_PASSWORD);
        checkForNullOrEmptyParameter(parameterValidation, licenseMitigationRequest.getVeracodeUsername(),
                RequestParameters.VERACODE_USERNAME);
        return parameterValidation;
    }

    private void validateLicensesToMitigate(ParameterValidation parameterValidation,
                                            ApiCredentials apiCredentials, List<LicenseToMitigate> licensesToMitigate) {
        licensesToMitigate.forEach(licenseToMitigate -> {
            checkForNullOrEmptyParameter(parameterValidation, licenseToMitigate.getComponentFileName(),
                    RequestParameters.COMPONENT_FILE_NAME);
            checkForNullOrEmptyParameter(parameterValidation, licenseToMitigate.getMitigationText(),
                    RequestParameters.MITIGATION_TEXT);
            checkForNullOrEmptyParameter(parameterValidation, licenseToMitigate.getApplicationId(),
                    RequestParameters.APPLICATION_ID);
            checkForNullOrEmptyParameter(parameterValidation, licenseToMitigate.getLicenseName(),
                    RequestParameters.LICENSE_NAME);
            if (!parameterValidation.hasErrors()) {
                validateApplicationId(licenseToMitigate, apiCredentials, parameterValidation);
            }
            checkIfInvalidOrNullMitigationType(parameterValidation, licenseToMitigate.getMitigationType());
        });
    }

    private void checkIfInvalidOrNullMitigationType(ParameterValidation parameterValidation,
                                                    MitigationTypeEnum mitigationType) {
        if (mitigationType == null) {
            parameterValidation.addError(RequestParameters.MITIGATION_TYPE, "Must not be null");
        } else if (mitigationType == MitigationTypeEnum.Invalid) {
            parameterValidation.addError(RequestParameters.MITIGATION_TYPE, "Is invalid");
        }
    }

    private void checkForNullOrEmptyParameter(ParameterValidation parameterValidation,
                                              String parameterValue, String parameterName) {
        if (parameterValue == null
                || "".equals(parameterValue.trim())) {
            parameterValidation.addError(parameterName, "Must not be null");
        }
    }

    private void validateApplicationId(LicenseToMitigate licenseToMitigate,
                                       ApiCredentials apiCredentials,
                                       ParameterValidation parameterValidation) {
        licenseToMitigate.setApplicationToMitigate(
                ApiCaller.getApplicationById(licenseToMitigate.getApplicationId(), apiCredentials));
        if (licenseToMitigate.getApplicationToMitigate() == null) {
            parameterValidation.addError(RequestParameters.APPLICATION_ID, "Application not found");
        }
    }

    private List<MitigationProposal> buildMitigationProposals(LicenseMitigationRequest licenseMitigationRequest) {
        return licenseMitigationRequest.getLicensesToMitigate()
                .stream()
                .map(licenseToMitigate -> new MitigationProposal(
                        buildLicenseFinding(licenseToMitigate),
                        licenseToMitigate.getMitigationType(),
                        licenseToMitigate.getApplicationToMitigate(),
                        licenseToMitigate.getMitigationText()))
                .collect(Collectors.toList());
    }

    private LicenseFinding buildLicenseFinding(LicenseToMitigate licenseToMitigate) {
        return new LicenseFinding(licenseToMitigate.getLicenseName(),
                null, licenseToMitigate.getComponentFileName());
    }
}
