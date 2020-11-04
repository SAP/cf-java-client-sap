package com.sap.cloudfoundry.client.facade.adapters;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v2.spaces.UserSpaceRoleEntity;
import org.cloudfoundry.client.v2.spaces.UserSpaceRoleResource;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.ImmutableUserRole;
import com.sap.cloudfoundry.client.facade.domain.UserRole;

@Value.Immutable
public abstract class RawUserRole extends RawCloudEntity<UserRole> {

    @Value.Parameter
    public abstract UserSpaceRoleResource getResource();

    @Override
    public UserRole derive() {
        UserSpaceRoleResource resource = getResource();
        UserSpaceRoleEntity entity = resource.getEntity();
        return ImmutableUserRole.builder()
                                .metadata(parseResourceMetadata(resource))
                                .spaceRoles(parseSpaceRoles(entity))
                                .build();
    }

    private List<UserRole.SpaceRole> parseSpaceRoles(UserSpaceRoleEntity entity) {
        return entity.getSpaceRoles()
                     .stream()
                     .map(UserRole.SpaceRole::fromString)
                     .collect(Collectors.toList());
    }
}
