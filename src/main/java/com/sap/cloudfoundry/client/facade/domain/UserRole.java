package com.sap.cloudfoundry.client.facade.domain;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableUserRole.class)
@JsonDeserialize(as = ImmutableUserRole.class)
public abstract class UserRole extends CloudEntity {

    public abstract boolean isActive();

    public abstract List<SpaceRole> getSpaceRoles();

    public enum SpaceRole {
        SPACE_AUDITOR, SPACE_DEVELOPER, SPACE_MANAGER;

        private static final Map<String, SpaceRole> NAMES_TO_VALUES = Arrays.stream(SpaceRole.values())
                                                                            .collect(Collectors.toMap(role -> role.toString()
                                                                                                                  .toLowerCase(),
                                                                                                      Function.identity()));

        public static SpaceRole fromString(String spaceRoleName) {
            SpaceRole spaceRole = NAMES_TO_VALUES.get(spaceRoleName);
            if (spaceRole == null) {
                throw new IllegalStateException(MessageFormat.format("Space role not found: \"{0}\"", spaceRoleName));
            }
            return spaceRole;
        }
    }

    public boolean hasSpaceRole(SpaceRole spaceRole) {
        return getSpaceRoles().contains(spaceRole);
    }
}
