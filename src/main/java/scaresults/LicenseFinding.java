package scaresults;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class LicenseFinding {
    private final String licenseName;
    private final LicenseRiskEnum licenseRisk;
    private final String componentFileName;

    public LicenseFinding(String licenseName, LicenseRiskEnum licenseRisk,
                          String componentFileName) {
        this.licenseName = licenseName;
        this.licenseRisk = licenseRisk;
        this.componentFileName = componentFileName;
    }

    public static Optional<LicenseFinding> tryMake(List<WebElement> columns) {
        if (columns.size() < 5 || passesPolicy(columns.get(1))) {
            return Optional.empty();
        }

        String licenseName = getLicenseName(columns.get(2));
        LicenseRiskEnum licenseSeverity = getLicenseRisk(columns.get(3));
        String componentFileName = getComponentFileName(columns.get(4));
        if (licenseName != null && licenseSeverity != null && componentFileName != null) {
            return Optional.of(new LicenseFinding(licenseName,
                    licenseSeverity, componentFileName));
        }
        return Optional.empty();
    }

    private static boolean passesPolicy(WebElement policyStatusColumn) {
        return policyStatusColumn.findElement(By.xpath(".//span"))
                .getAttribute("data-original-title").equals("Pass");
    }

    private static String getLicenseName(WebElement licenseNameColumn) {
        return licenseNameColumn.findElement(By.xpath(".//a/span")).getAttribute("innerText");
    }

    private static LicenseRiskEnum getLicenseRisk(WebElement licenseRiskColumn) {
        return LicenseRiskEnum.getByName(licenseRiskColumn.findElement(By.xpath(".//span"))
                .getAttribute("innerText"));
    }

    private static String getComponentFileName(WebElement componentFileNameColumn) {
        return componentFileNameColumn.findElement(By.xpath(".//span")).getAttribute("innerText");
    }

    public String licenseName() {
        return licenseName;
    }

    public LicenseRiskEnum licenseRisk() {
        return licenseRisk;
    }

    public String componentFileName() {
        return componentFileName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        LicenseFinding that = (LicenseFinding) obj;
        return Objects.equals(this.licenseName, that.licenseName) &&
                Objects.equals(this.licenseRisk, that.licenseRisk) &&
                Objects.equals(this.componentFileName, that.componentFileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(licenseName, licenseRisk, componentFileName);
    }

    @Override
    public String toString() {
        return "LicenseFinding[" +
                "licenseName=" + licenseName + ", " +
                "licenseRisk=" + licenseRisk + ", " +
                "componentFileName=" + componentFileName + ']';
    }

}
