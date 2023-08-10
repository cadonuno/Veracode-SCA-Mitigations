package scaresults.proposal;

import scaresults.types.LicenseMitigationTypeEnum;
import scaresults.finding.LicenseFinding;
import util.ApplicationProfile;

import java.util.Objects;

public final class LicenseMitigationProposal extends MitigationProposalBase {
    private final LicenseFinding licenseFinding;
    private final LicenseMitigationTypeEnum mitigationType;


    public LicenseMitigationProposal(LicenseFinding licenseFinding, LicenseMitigationTypeEnum mitigationType,
                                     ApplicationProfile applicationProfile, String mitigationText, String acceptanceText) {
        super(applicationProfile, mitigationText, acceptanceText);
        this.licenseFinding = licenseFinding;
        this.mitigationType = mitigationType;
    }

    public LicenseFinding licenseFinding() {
        return licenseFinding;
    }

    public LicenseMitigationTypeEnum mitigationType() {
        return mitigationType;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        LicenseMitigationProposal that = (LicenseMitigationProposal) obj;
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
