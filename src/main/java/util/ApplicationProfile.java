package util;

import java.util.Objects;

public final class ApplicationProfile {
    private final String applicationId;
    private final String applicationName;
    private final String applicationProfileUrl;

    public ApplicationProfile(String applicationId, String applicationName, String applicationProfileUrl) {
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.applicationProfileUrl = applicationProfileUrl;
    }

    public String applicationId() {
        return applicationId;
    }

    public String applicationName() {
        return applicationName;
    }

    public String applicationProfileUrl() {
        return applicationProfileUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        ApplicationProfile that = (ApplicationProfile) obj;
        return Objects.equals(this.applicationId, that.applicationId) &&
                Objects.equals(this.applicationName, that.applicationName) &&
                Objects.equals(this.applicationProfileUrl, that.applicationProfileUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicationId, applicationName, applicationProfileUrl);
    }

    @Override
    public String toString() {
        return "ApplicationProfile[" +
                "applicationId=" + applicationId + ", " +
                "applicationName=" + applicationName + ", " +
                "applicationProfileUrl=" + applicationProfileUrl + ']';
    }

}
