package api.records.tomitigate;

import com.fasterxml.jackson.annotation.JsonProperty;
import scaresults.types.LicenseMitigationTypeEnum;

public class LicenseToMitigate extends ToMitigationBase {
    private final String licenseName;
    private final LicenseMitigationTypeEnum mitigationType;

    public LicenseToMitigate(@JsonProperty("ApplicationId") String applicationId,
                             @JsonProperty("MitigationText") String mitigationText,
                             @JsonProperty("LicenseName") String licenseName,
                             @JsonProperty("ComponentFileName") String componentFileName,
                             @JsonProperty("AcceptanceText") String acceptanceText,
                             @JsonProperty("MitigationType") String mitigationType) {
        super(applicationId, mitigationText, componentFileName, acceptanceText);
        this.licenseName = licenseName;
        this.mitigationType = LicenseMitigationTypeEnum.getByName(mitigationType);
    }

    public LicenseMitigationTypeEnum getMitigationType() {
        return mitigationType;
    }

    public String getLicenseName() {
        return licenseName;
    }
}
