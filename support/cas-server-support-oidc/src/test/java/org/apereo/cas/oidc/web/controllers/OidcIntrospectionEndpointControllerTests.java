package org.apereo.cas.oidc.web.controllers;

import org.apereo.cas.oidc.AbstractOidcTests;
import org.apereo.cas.oidc.OidcConstants;
import org.apereo.cas.oidc.web.controllers.introspection.OidcIntrospectionEndpointController;
import org.apereo.cas.support.oauth.OAuth20Constants;
import org.apereo.cas.support.oauth.web.response.introspection.OAuth20IntrospectionAccessTokenSuccessResponse;
import org.apereo.cas.util.EncodingUtils;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.pac4j.core.context.HttpConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is {@link OidcIntrospectionEndpointControllerTests}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Tag("OIDC")
class OidcIntrospectionEndpointControllerTests extends AbstractOidcTests {
    @Autowired
    @Qualifier("oidcIntrospectionEndpointController")
    protected OidcIntrospectionEndpointController oidcIntrospectionEndpointController;

    @Test
    void verifyOperationWithValidTicket() throws Exception {
        val request = getHttpRequestForEndpoint(OidcConstants.INTROSPECTION_URL);
        val response = new MockHttpServletResponse();

        val auth = "clientid:secret";
        val value = EncodingUtils.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
        request.addHeader(HttpConstants.AUTHORIZATION_HEADER, HttpConstants.BASIC_HEADER_PREFIX + value);

        val accessToken = getAccessToken();
        servicesManager.save(getOidcRegisteredService());
        this.ticketRegistry.addTicket(accessToken);
        request.addParameter(OAuth20Constants.TOKEN, accessToken.getId());
        val result = oidcIntrospectionEndpointController.handleRequest(request, response);
        val body = (OAuth20IntrospectionAccessTokenSuccessResponse) result.getBody();
        assertNotNull(body);
        assertTrue(Instant.ofEpochSecond(body.getExp()).isAfter(Instant.ofEpochSecond(body.getIat())));
        assertTrue(body.isActive());
        assertEquals(accessToken.getScopes(), Set.of(body.getScope().split(" ")));
    }

    @Test
    void verifyBadEndpointRequest() {
        val request = getHttpRequestForEndpoint("unknown/issuer");
        request.setRequestURI("unknown/issuer");
        val response = new MockHttpServletResponse();
        val mv = oidcIntrospectionEndpointController.handleRequest(request, response);
        assertEquals(HttpStatus.BAD_REQUEST, mv.getStatusCode());
    }

    @Test
    void verifyOperationWithInvalidTicket() throws Exception {
        val request = getHttpRequestForEndpoint(OidcConstants.INTROSPECTION_URL);
        val response = new MockHttpServletResponse();

        val auth = "clientid:secret";
        val value = EncodingUtils.encodeBase64(auth.getBytes(StandardCharsets.UTF_8));
        request.addHeader(HttpConstants.AUTHORIZATION_HEADER, HttpConstants.BASIC_HEADER_PREFIX + value);

        val accessToken = getAccessToken();
        servicesManager.save(getOidcRegisteredService());
        request.addParameter(OAuth20Constants.TOKEN, accessToken.getId());
        val result = oidcIntrospectionEndpointController.handleRequest(request, response);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        val body = (OAuth20IntrospectionAccessTokenSuccessResponse) result.getBody();
        assertNotNull(body);
        assertFalse(body.isActive());
    }
}
