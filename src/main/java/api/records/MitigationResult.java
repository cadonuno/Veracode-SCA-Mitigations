package api.records;

import java.util.Objects;

public final class MitigationResult {
    private final int httpCode;
    private final String message;

    public MitigationResult(int httpCode, String message) {
        this.httpCode = httpCode;
        this.message = message;
    }

    public int httpCode() {
        return httpCode;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        MitigationResult that = (MitigationResult) obj;
        return this.httpCode == that.httpCode &&
                Objects.equals(this.message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(httpCode, message);
    }

    @Override
    public String toString() {
        return "MitigationResult[" +
                "httpCode=" + httpCode + ", " +
                "message=" + message + ']';
    }


}
