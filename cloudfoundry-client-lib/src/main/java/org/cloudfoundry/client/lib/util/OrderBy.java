package org.cloudfoundry.client.lib.util;

import org.immutables.value.Value;

@Value.Immutable
public abstract class OrderBy {

    public static final OrderBy NO_ORDER = new OrderBy() {

        @Override
        public String getValue() {
            return null;
        }

        @Override
        public Direction getDirection() {
            return null;
        }

        @Override
        public String getOrderQuery() {
            return null;
        }

    };
    public static final String DESCENDING_ORDER_DIRECTION_QUERY = "-";
    public static final String ASCENDING_ORDER_DIRECTION_QUERY = "";

    /**
     * Use {@link Values} class in order to obtain valid orderBy value
     * 
     * @return String representing orderBy value
     */
    public abstract String getValue();

    @Value.Default
    public Direction getDirection() {
        return Direction.ASCENDING;
    }

    @Value.Derived
    public String getOrderQuery() {
        String orderDirectionQuery = getOrderDirectionQuery();
        return orderDirectionQuery + getValue();
    }

    private String getOrderDirectionQuery() {
        return getDirection() == Direction.ASCENDING ? ASCENDING_ORDER_DIRECTION_QUERY : DESCENDING_ORDER_DIRECTION_QUERY;
    }

    public enum Direction {
        ASCENDING, DESCENDING
    }

    public static abstract class Values {

        public static final String CREATED_AT = "created_at";
        public static final String UPDATED_AT = "updated_at";
        
        public static abstract class Applications extends Values {
            public static String NAME = "name";
        }

        public static abstract class Buildpacks extends Values {
            public static String POSITION = "position";
        }
        
        public static abstract class Organizations extends Values {
            public static String NAME = "name";
        }
        
        public static abstract class Spaces extends Values {
            public static String NAME = "name";
        }
    }

}
