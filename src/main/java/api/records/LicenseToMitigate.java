package api.records;

import org.apache.tinkerpop.shaded.jackson.annotation.JsonProperty;
import scaresults.MitigationTypeEnum;
import util.ApplicationProfile;

public class LicenseToMitigate {

    private final String applicationId;
    private final String mitigationText;
    private final String licenseName;
    private final String componentFileName;
    private final MitigationTypeEnum mitigationType;
    private ApplicationProfile applicationToMitigate;

    public LicenseToMitigate(@JsonProperty("applicationId") String applicationId,
                             @JsonProperty("mitigationText") String mitigationText,
                             @JsonProperty("licenseName") String licenseName,
                             @JsonProperty("componentFileName") String componentFileName,
                             @JsonProperty("mitigationType") String mitigationType) {
        this.mitigationText = mitigationText;
        this.licenseName = licenseName;
        this.componentFileName = componentFileName;
        this.mitigationType = MitigationTypeEnum.getByName(mitigationType);
        this.applicationId = applicationId;
    }

    public MitigationTypeEnum getMitigationType() {
        return mitigationType;
    }

    public String getComponentFileName() {
        return componentFileName;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public String getMitigationText() {
        return mitigationText;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public ApplicationProfile getApplicationToMitigate() {
        return applicationToMitigate;
    }

    public void setApplicationToMitigate(ApplicationProfile applicationToMitigate) {
        this.applicationToMitigate = applicationToMitigate;
    }
}
