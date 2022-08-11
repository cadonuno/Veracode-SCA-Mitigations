package scaresults;

import util.ApplicationProfile;

import java.util.Objects;

public final class MitigationProposal {
    private final LicenseFinding licenseFinding;
    private final MitigationTypeEnum mitigationType;
    private final ApplicationProfile applicationProfile;
    private final String mitigationText;

    public MitigationProposal(LicenseFinding licenseFinding, MitigationTypeEnum mitigationType,
                              ApplicationProfile applicationProfile, String mitigationText) {
        this.licenseFinding = licenseFinding;
        this.mitigationType = mitigationType;
        this.applicationProfile = applicationProfile;
        this.mitigationText = mitigationText;
    }

    public LicenseFinding licenseFinding() {
        return licenseFinding;
    }

    public MitigationTypeEnum mitigationType() {
        return mitigationType;
    }

    public ApplicationProfile applicationProfile() {
        return applicationProfile;
    }

    public String mitigationText() {
        return mitigationText;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        MitigationProposal that = (MitigationProposal) obj;
        return Objects.equals(this.licenseFinding, that.licenseFinding) &&
                Objects.equals(this.mitigationType, that.mitigationType) &&
                Objects.equals(this.applicationProfile, that.applicationProfile) &&
                Objects.equals(this.mitigationText, that.mitigationText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licenseFinding, mitigationType, applicationProfile, mitigationText);
    }

    @Override
    public String toString() {
        return "Application Profile: { " + applicationProfile.applicationName() +
                " - " + applicationProfile.applicationId() + " }, " +
                "License: { " + licenseFinding.componentFileName() +
                " - " + licenseFinding.licenseName() + " }";
    }
}
