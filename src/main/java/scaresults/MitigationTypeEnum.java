package scaresults;

import java.util.Locale;

public enum MitigationTypeEnum {
    ApprovedByLegal("Approved by Legal"),
    CommerciallyLicensed("Commercially Licensed"),
    Experimental("Experimental"),
    InternalUse("Internal Use"),
    Invalid("Invalid");

    private final String typeAsText;

    MitigationTypeEnum(String typeAsText) {
        this.typeAsText = typeAsText;
    }

    public String getAsText() {
        return typeAsText;
    }

    public static MitigationTypeEnum getByName(String mitigationName) {
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
