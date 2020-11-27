open module com.sap.cloudfoundry.client.facade {

    requires cloudfoundry.client;
    requires cloudfoundry.client.reactor;
    requires cloudfoundry.util;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.io;
    requires org.slf4j;
    requires spring.core;
    requires spring.security.oauth2;
    requires spring.web;
    requires spring.webflux;
    requires reactor.core;
    requires io.netty.handler;
    requires reactor.netty;
    requires org.apache.commons.logging;
    requires org.reactivestreams;

    requires static java.compiler;
    requires static org.immutables.value;

    exports com.sap.cloudfoundry.client.facade;
    exports com.sap.cloudfoundry.client.facade.rest;
    exports com.sap.cloudfoundry.client.facade.oauth2;
    exports com.sap.cloudfoundry.client.facade.domain;
    exports com.sap.cloudfoundry.client.facade.adapters;
    exports com.sap.cloudfoundry.client.facade.util;

}