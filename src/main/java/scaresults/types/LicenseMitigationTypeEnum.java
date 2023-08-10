package scaresults.types;

import java.util.Locale;

public enum LicenseMitigationTypeEnum {
    ApprovedByLegal("Mitigate as Approved by Legal"),
    CommerciallyLicensed("Mitigate as Commercially Licensed"),
    Experimental("Mitigate as Experimental"),
    InternalUse("Mitigate as Internal Use"),
    Invalid("Invalid");

    private final String typeAsText;

    LicenseMitigationTypeEnum(String typeAsText) {
        this.typeAsText = typeAsText;
    }

    public String getAsText() {
        return typeAsText;
    }

    public static LicenseMitigationTypeEnum getByName(String mitigationName) {
        if (mitigationName == null) {
            return null;
        }
        switch (mitigationName.trim().toLowerCase(Locale.ROOT)) {
            case "approved by legal":
            case "approvedbylegal":
                return ApprovedByLegal;
            case "commercially licensed":
            case "commerciallylicensed":
                return CommerciallyLicensed;
            case "experimental":
                return Experimental;
            case "internal use":
            case "internaluser":
                return InternalUse;
            case "":
                return null;
            default:
                return Invalid;
        }
    }
}
