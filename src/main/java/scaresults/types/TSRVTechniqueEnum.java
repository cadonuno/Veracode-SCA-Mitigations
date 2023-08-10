package scaresults.types;

import java.util.Locale;

public enum TSRVTechniqueEnum {
    M1(1),
    M2(2),
    M3(3),
    M4(4),
    M5(5),

    GP1(6),
    GP2(7),
    GP3(8),
    GP4(9),

    Invalid(-1);

    private final int selectorIndex;

    TSRVTechniqueEnum(int selectorIndex) {
        this.selectorIndex = selectorIndex;
    }

    public int getSelectorIndex() {
        return selectorIndex;
    }

    public static TSRVTechniqueEnum getByName(String tsrvName) {
        if (tsrvName == null) {
            return null;
        }
        switch (tsrvName.trim().toLowerCase(Locale.ROOT)) {
            case "m1":
                return M1;
            case "m2":
                return M2;
            case "m3":
                return M3;
            case "m4":
                return M4;
            case "m5":
                return M5;
            case "gp1":
                return GP1;
            case "gp2":
                return GP2;
            case "gp3":
                return GP3;
            case "gp4":
                return GP4;
            case "":
                return null;
            default:
                return Invalid;
        }
    }
}
