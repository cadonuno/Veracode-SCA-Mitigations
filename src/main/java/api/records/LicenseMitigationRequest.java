package api.records;

import org.apache.tinkerpop.shaded.jackson.annotation.JsonProperty;

import java.util.List;

public class LicenseMitigationRequest {

    private final List<LicenseToMitigate> licensesToMitigate;
    private final String veracodeApiId;
    private final String veracodeApiKey;
    private final String veracodeUsername;
    private final String veracodePassword;

    public LicenseMitigationRequest(@JsonProperty("licensesToMitigate") List<LicenseToMitigate> licensesToMitigate,
                                    @JsonProperty("veracodeApiId") String veracodeApiId,
                                    @JsonProperty("veracodeApiKey") String veracodeApiKey,
                                    @JsonProperty("veracodeUsername") String veracodeUsername,
                                    @JsonProperty("veracodePassword") String veracodePassword) {
        this.licensesToMitigate = licensesToMitigate;
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
        return licensesToMitigate;
    }
}
