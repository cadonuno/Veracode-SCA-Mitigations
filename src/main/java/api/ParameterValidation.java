package api;

import util.ApplicationProfile;

import java.util.HashMap;
import java.util.Map;

public class ParameterValidation {
    private Map<String, String> errors = new HashMap<>();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }


    public void addError(String field, String value) {
        errors.put(field, value);
    }

    public Map<String, String> getErrors() {
        return new HashMap<>(this.errors);
    }
}
