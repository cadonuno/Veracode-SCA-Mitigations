package scaresults.proposal;

import util.ApplicationProfile;

public abstract class MitigationProposalBase {
    protected final ApplicationProfile applicationProfile;
    protected final String mitigationText;

    protected final String acceptanceText;

    public MitigationProposalBase(ApplicationProfile applicationProfile, String mitigationText, String acceptanceText) {
        this.applicationProfile = applicationProfile;
        this.mitigationText = mitigationText;
        this.acceptanceText = acceptanceText;
    }

    public ApplicationProfile applicationProfile() {
        return applicationProfile;
    }

    public String mitigationText() {
        return mitigationText;
    }
    public String acceptanceText() {
        return acceptanceText;
    }
}
