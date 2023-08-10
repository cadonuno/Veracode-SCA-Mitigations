package scaresults.types;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public final class TSRV {
    private final TSRVTechniqueEnum technique;
    private final String specifics;
    private final String remainingRisk;
    private final String verification;

    TSRV(@JsonProperty("Technique")String technique,
         @JsonProperty("Specifics")String specifics,
         @JsonProperty("RemainingRisk")String remainingRisk,
         @JsonProperty("Verification")String verification) {
        this.technique = TSRVTechniqueEnum.getByName(technique);
        this.specifics = specifics;
        this.remainingRisk = remainingRisk;
        this.verification = verification;
    }

    public TSRVTechniqueEnum technique() {
        return technique;
    }

    public String specifics() {
        return specifics;
    }

    public String remainingRisk() {
        return remainingRisk;
    }

    public String verification() {
        return verification;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        TSRV that = (TSRV) obj;
        return Objects.equals(this.technique, that.technique) &&
                Objects.equals(this.specifics, that.specifics) &&
                Objects.equals(this.remainingRisk, that.remainingRisk) &&
                Objects.equals(this.verification, that.verification);
    }

    @Override
    public int hashCode() {
        return Objects.hash(technique, specifics, remainingRisk, verification);
    }

    @Override
    public String toString() {
        return "TSRV[" +
                "technique=" + technique + ", " +
                "specifics=" + specifics + ", " +
                "remainingRisk=" + remainingRisk + ", " +
                "verification=" + verification + ']';
    }


}
