package api.records;

import api.records.tomitigate.LicenseToMitigate;
import api.records.tomitigate.VulnerabilityToMitigate;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class ScaMitigationRequest {

    private final String veracodeApiId;
    private final String veracodeApiKey;
    private final String veracodeUsername;
    private final String veracodePassword;

    private final List<LicenseToMitigate> licensesToMitigate;
    private final List<VulnerabilityToMitigate> vulnerabilitiesToMitigate;

    public ScaMitigationRequest(@JsonProperty("LicensesToMitigate") List<LicenseToMitigate> licensesToMitigate,
                                @JsonProperty("VulnerabilitiesToMitigate") List<VulnerabilityToMitigate> vulnerabilitiesToMitigate,
                                @JsonProperty("VeracodeApiId") String veracodeApiId,
                                @JsonProperty("VeracodeApiKey") String veracodeApiKey,
                                @JsonProperty("VeracodeUsername") String veracodeUsername,
                                @JsonProperty("VeracodePassword") String veracodePassword) {
        this.licensesToMitigate = licensesToMitigate;
        this.vulnerabilitiesToMitigate = vulnerabilitiesToMitigate;
        this.veracodeApiId = veracodeApiId;
        this.veracodeApiKey = veracodeApiKey;
        this.veracodeUsername = veracodeUsername;
        this.veracodePassword = veracodePassword;
    }

    public String getVeracodePassword() {
        return veracodePassword;
    }

    public String getVeracodeUsername() {
        return veracodeUsername;
    }

    public String getVeracodeApiKey() {
        return veracodeApiKey;
    }

    public String getVeracodeApiId() {
        return veracodeApiId;
    }

    public List<LicenseToMitigate> getLicensesToMitigate() {
        return licensesToMitigate == null ? Collections.emptyList() : licensesToMitigate;
    }

    public List<VulnerabilityToMitigate> getVulnerabilitiesToMitigate() {
        return vulnerabilitiesToMitigate == null ? Collections.emptyList() : vulnerabilitiesToMitigate;
    }
}
