package api.records.tomitigate;

import util.ApplicationProfile;

public abstract class ToMitigationBase {
    private final String applicationId;
    private final String mitigationText;
    private final String componentFileName;
    private final String acceptanceText;

    private ApplicationProfile applicationToMitigate;

    protected ToMitigationBase(String applicationId, String mitigationText, String componentFileName, String acceptanceText) {
        this.applicationId = applicationId;
        this.mitigationText = mitigationText;
        this.componentFileName = componentFileName;
        this.acceptanceText = acceptanceText;
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

    public String getComponentFileName() {
        return componentFileName;
    }

    public void setApplicationToMitigate(ApplicationProfile applicationToMitigate) {
        this.applicationToMitigate = applicationToMitigate;
    }

    public String getAcceptanceText() {
        return acceptanceText;
    }
}
