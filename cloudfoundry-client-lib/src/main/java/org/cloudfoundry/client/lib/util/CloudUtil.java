/*
 * Copyright 2009-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.client.lib.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Some helper utilities used by the Cloud Foundry Java client.
 *
 * @author Ramnivas Laddad
 * @author A.B.Srinivasan
 */
public class CloudUtil {

    public static final int BUFFER_SIZE = 16 * 1024;
    private static final Double DEFAULT_DOUBLE = 0.0;
    private static final Integer DEFAULT_INTEGER = 0;
    private static final Long DEFAULT_LONG = 0L;

    private CloudUtil() {
    }

    public static String parse(Object object) {
        return parse(String.class, object);
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
