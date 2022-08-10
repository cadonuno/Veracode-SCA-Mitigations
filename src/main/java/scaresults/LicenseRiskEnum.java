package scaresults;

public enum LicenseRiskEnum {
    High,
    Medium,
    Low;

    public static LicenseRiskEnum getByName(String riskAsString) {
        switch (riskAsString) {
            case "High":
                return High;
            case "Medium":
                return Medium;
            case "Low":
                return Low;
            default:
                return null;
        }
    }
}
