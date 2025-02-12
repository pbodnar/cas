package org.apereo.cas;

import org.apereo.cas.configuration.model.support.ldap.AbstractLdapAuthenticationProperties;
import org.apereo.cas.configuration.model.support.ldap.AbstractLdapProperties;
import org.apereo.cas.configuration.model.support.ldap.LdapSearchEntryHandlersProperties;
import org.apereo.cas.util.LdapConnectionFactory;
import org.apereo.cas.util.LdapUtils;
import org.apereo.cas.util.junit.EnabledIfListeningOnPort;
import org.apereo.cas.util.scripting.GroovyScriptResourceCacheManager;
import org.apereo.cas.util.scripting.ScriptResourceCacheManager;
import org.apereo.cas.util.spring.ApplicationContextProvider;

import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.DerefAliases;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.handler.CaseChangeEntryHandler;
import org.ldaptive.sasl.Mechanism;
import org.ldaptive.sasl.QualityOfProtection;
import org.ldaptive.sasl.SecurityStrength;
import org.springframework.context.support.StaticApplicationContext;

import java.io.File;
import java.io.Serial;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link LdapUtilsTests}.
 *
 * @author Misagh Moayyed
 * @since 6.2.0
 */
@Tag("Ldap")
@EnabledIfListeningOnPort(port = 10389)
class LdapUtilsTests {

    @Test
    void verifyGetBoolean() {
        val entry = new LdapEntry();
        entry.addAttributes(new LdapAttribute("attr1", "true"));
        entry.addAttributes(new LdapAttribute("attr2", StringUtils.EMPTY));
        var input = LdapUtils.getBoolean(entry, "attr1", Boolean.TRUE);
        assertTrue(input);

        input = LdapUtils.getBoolean(entry, "attr2", Boolean.TRUE);
        assertTrue(input);
    }


    @Test
    void verifyGetLong() {
        val entry = new LdapEntry();
        entry.addAttributes(new LdapAttribute("attr1", "100"));
        val input = LdapUtils.getLong(entry, "attr1", 0L);
        assertEquals(100, input);
    }

    @Test
    void verifyGetBinary() {
        val entry = new LdapEntry();
        val attr = new LdapAttribute("attr1", "100".getBytes(StandardCharsets.UTF_8));
        attr.setBinary(true);
        entry.addAttributes(attr);

        val input = LdapUtils.getString(entry, "attr1");
        assertEquals("100", input);
    }

    @Test
    void verifyEntry() throws Exception {
        assertFalse(LdapUtils.isLdapConnectionUrl(new URL("https://github.com")));
        assertFalse(LdapUtils.containsResultEntry(null));
    }

    @Test
    void verifyFailsOp() throws Exception {
        val factory = mock(ConnectionFactory.class);
        val wrapper = new LdapConnectionFactory(factory);
        when(factory.getConnectionConfig()).thenThrow(new IllegalArgumentException("fails"));
        when(factory.getConnection()).thenThrow(new IllegalArgumentException("fails"));
        assertFalse(wrapper.executePasswordModifyOperation(null, null, null, AbstractLdapProperties.LdapType.GENERIC));
        assertFalse(wrapper.executeModifyOperation(null, Map.of()));
        assertFalse(wrapper.executeAddOperation(new LdapEntry()));
        assertFalse(wrapper.executeDeleteOperation(new LdapEntry()));
        wrapper.close();
    }

    @Test
    void verifyScriptedFilter() {
        val appCtx = new StaticApplicationContext();
        appCtx.refresh();
        ApplicationContextProvider.holdApplicationContext(appCtx);
        assertThrows(RuntimeException.class,
            () -> LdapUtils.newLdaptiveSearchFilter("classpath:LdapFilterQuery.groovy",
                List.of("p1", "p2"), List.of("v1", "v2")));

        val cacheMgr = new GroovyScriptResourceCacheManager();
        ApplicationContextProvider.registerBeanIntoApplicationContext(appCtx, cacheMgr, ScriptResourceCacheManager.BEAN_NAME);
        var filter = LdapUtils.newLdaptiveSearchFilter("classpath:LdapFilterQuery.groovy",
            List.of("p1", "p2"), List.of("v1", "v2"));
        assertNotNull(filter);
        assertNotNull(filter.getFilter());
        filter = LdapUtils.newLdaptiveSearchFilter("classpath:LdapFilterQuery.groovy",
            List.of("p1", "p2"), List.of("v1", "v2"));
        assertNotNull(filter);
        assertNotNull(filter.getFilter());
        filter = LdapUtils.newLdaptiveSearchFilter("classpath:UnknownLdapFilterQuery.groovy",
            List.of("p1", "p2"), List.of("v1", "v2"));
        assertNotNull(filter);
    }

    @Test
    void verifyFilterByIndex() throws Exception {
        val filter = LdapUtils.newLdaptiveSearchFilter("cn={0}", List.of("casuser"));
        assertTrue(filter.getParameters().containsKey("0"));
        assertTrue(filter.getParameters().containsValue("casuser"));
    }

    @Test
    void verifyLdapAuthnAnon() throws Exception {
        val ldap = new Ldap();
        ldap.setLdapUrl("ldap://localhost:10389");
        ldap.setBindDn("cn=Directory Manager");
        ldap.setBindCredential("password");
        ldap.setPrincipalAttributePassword("password");
        ldap.setDerefAliases(DerefAliases.FINDING.name());
        ldap.setFailFast(false);
        ldap.setType(AbstractLdapAuthenticationProperties.AuthenticationTypes.ANONYMOUS);
        assertThrows(IllegalArgumentException.class, () -> LdapUtils.newLdaptiveAuthenticator(ldap));
        ldap.setBaseDn("ou=people,dc=example,dc=org");
        assertThrows(IllegalArgumentException.class, () -> LdapUtils.newLdaptiveAuthenticator(ldap));
        ldap.setSearchFilter("cn=invalid-user");
        assertNotNull(LdapUtils.newLdaptiveAuthenticator(ldap));
    }

    @Test
    void verifyLdapAuthnDirect() {
        val ldap = new Ldap();
        ldap.setLdapUrl("ldap://localhost:10389");
        ldap.setBindDn("cn=Directory Manager");
        ldap.setBindCredential("password");
        ldap.setBaseDn("ou=people,dc=example,dc=org|ou=users,dc=example,dc=org");
        ldap.setSearchFilter("cn=invalid-user");
        ldap.setDerefAliases(DerefAliases.FINDING.name());
        ldap.setFailFast(false);
        ldap.setType(AbstractLdapAuthenticationProperties.AuthenticationTypes.DIRECT);
        assertThrows(IllegalArgumentException.class, () -> LdapUtils.newLdaptiveAuthenticator(ldap));
        ldap.setDnFormat("cn=%s,dc=example,dc=org");
        assertNotNull(LdapUtils.newLdaptiveAuthenticator(ldap));
    }

    @Test
    void verifyLdapAuthnActiveDirectory() throws Exception {
        val ldap = new Ldap();
        ldap.setLdapUrl("ldap://localhost:10389");
        ldap.setBindDn("cn=Directory Manager");
        ldap.setBindCredential("password");
        ldap.setBaseDn("ou=people,dc=example,dc=org");
        ldap.setSearchFilter("cn=invalid-user");
        ldap.setDerefAliases(DerefAliases.FINDING.name());
        ldap.setPrincipalAttributePassword("password");
        ldap.setFailFast(false);
        ldap.setType(AbstractLdapAuthenticationProperties.AuthenticationTypes.AD);
        assertThrows(IllegalArgumentException.class, () -> LdapUtils.newLdaptiveAuthenticator(ldap));
        ldap.setDnFormat("cn=%s,dc=example,dc=org");
        assertNotNull(LdapUtils.newLdaptiveAuthenticator(ldap));
    }

    @Test
    void verifyPagedSearch() throws Exception {
        val ldap = new Ldap();
        ldap.setBaseDn("ou=people,dc=example,dc=org");
        ldap.setLdapUrl("ldap://localhost:10389");
        ldap.setBindDn("cn=Directory Manager");
        ldap.setBindCredential("password");
        ldap.setSearchFilter("cn=invalid-user");

        var factory = new LdapConnectionFactory(LdapUtils.newLdaptiveConnectionFactory(ldap));
        var response = factory.executeSearchOperation(ldap.getBaseDn(),
            LdapUtils.newLdaptiveSearchFilter(ldap.getSearchFilter()), 10, "cn");
        assertNotNull(response);
        assertFalse(LdapUtils.containsResultEntry(response));
        factory.close();

        ldap.setDisablePooling(true);
        factory = new LdapConnectionFactory(LdapUtils.newLdaptiveConnectionFactory(ldap));
        response = factory.executeSearchOperation(ldap.getBaseDn(),
            LdapUtils.newLdaptiveSearchFilter(ldap.getSearchFilter()), 10, "cn");
        assertNotNull(response);
        assertFalse(LdapUtils.containsResultEntry(response));
        factory.close();
    }

    @Test
    void verifyComparePooling() throws Exception {
        val ldap = new Ldap();
        ldap.setBaseDn("ou=people,dc=example,dc=org|ou=users,dc=example,dc=org");
        ldap.setLdapUrl("ldap://localhost:10389");
        ldap.setBindDn("cn=Directory Manager");
        ldap.setBindCredential("password");
        ldap.setSearchFilter("cn=invalid-user");
        ldap.getValidator().setType("compare");

        val factory = new LdapConnectionFactory(LdapUtils.newLdaptivePooledConnectionFactory(ldap));
        val response = factory.executeSearchOperation(ldap.getBaseDn(),
            LdapUtils.newLdaptiveSearchFilter(ldap.getSearchFilter()), 10, "cn");
        assertNotNull(response);
        assertFalse(LdapUtils.containsResultEntry(response));

        val config1 = LdapUtils.newLdaptiveConnectionConfig(ldap);
        assertNotNull(config1);
        Arrays.stream(LdapSearchEntryHandlersProperties.SearchEntryHandlerTypes.values())
            .forEach(type -> {
                val props = new LdapSearchEntryHandlersProperties();
                props.setType(type);
                props.getCaseChange().setAttributeNameCaseChange(CaseChangeEntryHandler.CaseChange.UPPER.name());
                props.getCaseChange().setDnCaseChange(CaseChangeEntryHandler.CaseChange.UPPER.name());
                props.getCaseChange().setAttributeValueCaseChange(CaseChangeEntryHandler.CaseChange.UPPER.name());
                ldap.getSearchEntryHandlers().add(props);
                val resolver = LdapUtils.newLdaptiveSearchEntryResolver(ldap, factory.getConnectionFactory());
                assertNotNull(resolver);
            });
        factory.close();
    }

    @Test
    void verifyConnectionConfig() throws Exception {
        val ldap = new Ldap();
        ldap.setBaseDn("ou=people,dc=example,dc=org|ou=users,dc=example,dc=org");
        ldap.setLdapUrl("ldap://localhost:10389");
        ldap.setBindDn("cn=Directory Manager");
        ldap.setBindCredential("password");
        ldap.setSearchFilter("cn=invalid-user");

        Arrays.stream(AbstractLdapProperties.LdapConnectionStrategy.values()).forEach(s -> {
            ldap.setConnectionStrategy(s.toString());
            val config = LdapUtils.newLdaptiveConnectionConfig(ldap);
            assertNotNull(config);
        });

        ldap.setDerefAliases(DerefAliases.SEARCHING.name());
        ldap.setKeystoreType(KeyStore.getDefaultType());
        ldap.setKeystorePassword("changeit");
        ldap.setKeystore(new File(System.getenv("JAVA_HOME"), "jre/lib/security/cacerts").getCanonicalPath());
        val config = LdapUtils.newLdaptiveConnectionConfig(ldap);
        assertNotNull(config);

        Arrays.stream(Mechanism.values()).forEach(m -> {
            ldap.setSaslMechanism(m.name());
            ldap.setSaslRealm("cas");
            ldap.setSaslMutualAuth(Boolean.FALSE);
            ldap.setSaslAuthorizationId("123456");
            ldap.setSaslQualityOfProtection(QualityOfProtection.AUTH.name());
            ldap.setSaslSecurityStrength(SecurityStrength.MEDIUM.name());

            val config1 = LdapUtils.newLdaptiveConnectionConfig(ldap);
            assertNotNull(config1);
        });
    }

    private static class Ldap extends AbstractLdapAuthenticationProperties {
        @Serial
        private static final long serialVersionUID = 7979417317490698363L;
    }
}
