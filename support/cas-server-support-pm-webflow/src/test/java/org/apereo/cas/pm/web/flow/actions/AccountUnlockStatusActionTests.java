package org.apereo.cas.pm.web.flow.actions;

import org.apereo.cas.authentication.Credential;
import org.apereo.cas.services.RegisteredServiceTestUtils;
import org.apereo.cas.web.flow.CasWebflowConstants;

import lombok.val;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.binding.message.MessageContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.Action;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.test.MockParameterMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This is {@link AccountUnlockStatusActionTests}.
 *
 * @author Misagh Moayyed
 * @since 6.6.0
 */
@Tag("WebflowActions")
public class AccountUnlockStatusActionTests extends BasePasswordManagementActionTests {
    @Autowired
    @Qualifier(CasWebflowConstants.ACTION_ID_UNLOCK_ACCOUNT_STATUS)
    protected Action action;

    @Test
    public void verifyBadCaptcha() throws Exception {
        val context = getRequestContext("good", "bad");
        val result = action.execute(context);
        assertEquals(CasWebflowConstants.TRANSITION_ID_ERROR, result.getId());
    }

    @Test
    public void verifyAccountUnlock() throws Exception {
        val context = getRequestContext("good", "good");
        val result = action.execute(context);
        assertEquals(CasWebflowConstants.TRANSITION_ID_SUCCESS, result.getId());
    }

    private static RequestContext getRequestContext(final String captcha, final String givenCaptcha) {
        val context = mock(RequestContext.class);
        when(context.getMessageContext()).thenReturn(mock(MessageContext.class));
        when(context.getFlowScope()).thenReturn(new LocalAttributeMap<>());
        final MockParameterMap requestParameters = new MockParameterMap();
        requestParameters.put("captchaValue", givenCaptcha);
        when(context.getRequestParameters()).thenReturn(requestParameters);
        when(context.getConversationScope()).thenReturn(new LocalAttributeMap<>());
        when(context.getExternalContext()).thenReturn(new ServletExternalContext(new MockServletContext(),
            new MockHttpServletRequest(), new MockHttpServletResponse()));
        val credential = RegisteredServiceTestUtils.getCredentialsWithSameUsernameAndPassword("casuser");
        context.getConversationScope().put(Credential.class.getName(), credential);
        context.getConversationScope().put("captchaValue", captcha);
        return context;
    }
}
