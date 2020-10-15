package com.sap.cloudfoundry.client.facade.domain.annotation;

import org.immutables.value.Value;

@Value.Style(depluralize = true, typeImmutable = "Immutable*", typeImmutableNested = "Immutable*", visibility = Value.Style.ImplementationVisibility.PUBLIC)
public @interface GenerationStyle {

}
