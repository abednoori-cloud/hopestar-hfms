package com.hopestar.hfms.common.util;

import lombok.experimental.UtilityClass;

/**
 * Small string helpers shared across modules — most notably
 * {@link #padSequence} which the future
 * {@code SequenceGeneratorService} (Invoice/Student/Employee/Transaction
 * numbering, approved architecture §6) will use to zero-pad sequence
 * values to the configured width.
 */
@UtilityClass
public class StringUtil {

    public boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public String defaultIfBlank(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    public String padSequence(long value, int width) {
        return String.format("%0" + width + "d", value);
    }

    public String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
