package com.sap.cloudfoundry.client.facade.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Some helper utilities used by the Cloud Foundry Java client.
 *
 */
public class CloudUtil {

    private static final Double DEFAULT_DOUBLE = 0.0;
    private static final Integer DEFAULT_INTEGER = 0;
    private static final Long DEFAULT_LONG = 0L;

    private CloudUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T parse(Class<T> clazz, Object object) {
        T defaultValue = null;
        try {
            if (clazz == Date.class) {
                String stringValue = parse(String.class, object);
                return clazz.cast(new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.US).parse(stringValue));
            }

            if (clazz == Integer.class) {
                defaultValue = (T) DEFAULT_INTEGER;
            } else if (clazz == Long.class) {
                defaultValue = (T) DEFAULT_LONG;
            } else if (clazz == Double.class) {
                defaultValue = (T) DEFAULT_DOUBLE;
            }

            if (object == null) {
                return defaultValue;
            }

            // special handling for int and long since smaller numbers become ints
            // but may be requested as long and vice versa
            if (clazz == Integer.class) {
                if (object instanceof Number) {
                    return clazz.cast(((Number) object).intValue());
                } else if (object instanceof String) {
                    return clazz.cast(Integer.valueOf(((String) object)));
                }
            }
            if (clazz == Long.class) {
                if (object instanceof Number) {
                    return clazz.cast(((Number) object).longValue());
                } else if (object instanceof String) {
                    return clazz.cast(Long.valueOf(((String) object)));
                }
            }
            if (clazz == Double.class) {
                if (object instanceof Number) {
                    return clazz.cast(((Number) object).doubleValue());
                } else if (object instanceof String) {
                    return clazz.cast(Double.valueOf(((String) object)));
                }
            }

            return clazz.cast(object);
        } catch (ClassCastException | ParseException e) {
            // ignore
        }
        return defaultValue;
    }

}
