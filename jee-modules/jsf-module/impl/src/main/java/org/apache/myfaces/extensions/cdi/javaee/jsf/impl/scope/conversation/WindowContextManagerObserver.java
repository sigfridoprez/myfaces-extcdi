/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.extensions.cdi.javaee.jsf.impl.scope.conversation;

import org.apache.myfaces.extensions.cdi.javaee.jsf.api.listener.phase.AfterPhase;
import org.apache.myfaces.extensions.cdi.javaee.jsf.api.listener.phase.PhaseId;
import org.apache.myfaces.extensions.cdi.javaee.jsf.api.request.RequestTypeResolver;
import org.apache.myfaces.extensions.cdi.javaee.jsf.impl.util.RequestCache;
import org.apache.myfaces.extensions.cdi.javaee.jsf.impl.util.ConversationUtils;
import static org.apache.myfaces.extensions.cdi.javaee.jsf.impl.util.ConversationUtils.*;
import org.apache.myfaces.extensions.cdi.javaee.jsf.impl.scope.conversation.spi.WindowHandler;
import org.apache.myfaces.extensions.cdi.javaee.jsf.impl.scope.conversation.spi.JsfAwareWindowContextConfig;
import org.apache.myfaces.extensions.cdi.javaee.jsf.impl.scope.conversation.spi.EditableWindowContext;
import org.apache.myfaces.extensions.cdi.javaee.jsf.impl.scope.conversation.spi.EditableWindowContextManager;
import static org.apache.myfaces.extensions.cdi.core.impl.scope.conversation.spi.WindowContextManager
        .WINDOW_CONTEXT_ID_PARAMETER_KEY;
import org.apache.myfaces.extensions.cdi.core.impl.scope.conversation.spi.WindowContextManager;
import org.apache.myfaces.extensions.cdi.core.impl.scope.conversation.spi.EditableConversation;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.WindowContext;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.WindowContextConfig;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.Conversation;

import javax.enterprise.event.Observes;
import javax.faces.event.PhaseEvent;
import javax.faces.context.FacesContext;
import java.io.IOException;

/**
 * @author Gerhard Petracek
 */
@SuppressWarnings({"UnusedDeclaration"})
final class WindowContextManagerObserver
{
    //don't change/optimize this observer!!!
    protected void cleanup(@Observes @AfterPhase(PhaseId.RESTORE_VIEW) PhaseEvent phaseEvent,
                           RequestTypeResolver requestTypeResolver,
                           WindowContextManager windowContextManager)
    {
        WindowContext windowContext = windowContextManager.getCurrentWindowContext();
        if (!requestTypeResolver.isPostRequest() && !requestTypeResolver.isPartialRequest())
        {
            boolean continueRequest = processGetRequest(phaseEvent.getFacesContext(), windowContext.getConfig());
            if (!continueRequest)
            {
                return;
            }
        }

        //don't refactor it to a lazy restore
        storeCurrentViewIdAsNewViewId(phaseEvent.getFacesContext(), windowContext);

        //for performance reasons + cleanup at the beginning of the request (check timeout,...)
        //+ we aren't allowed to cleanup in case of redirects
        //we would transfer the restored view-id into the conversation
        //don't ignore partial requests - in case of ajax-navigation we wouldn't check for expiration
        if (!requestTypeResolver.isPostRequest())
        {
            return;
        }

        cleanupInactiveConversations(windowContext);
    }

    protected void cleanupAndRecordCurrentViewAsOldViewId(
            @Observes @AfterPhase(PhaseId.RENDER_RESPONSE) PhaseEvent phaseEvent,
            EditableWindowContextManager windowContextManager)
    {
        storeCurrentViewIdAsOldViewId(phaseEvent.getFacesContext());

        cleanupInactiveWindowContexts(windowContextManager);
        RequestCache.resetCache();
    }

    /**
     * an external app might call a page without url parameter.
     * to support such an use-case it's possible to
     *  - deactivate the url parameter support (requires a special WindowHandler see e.g.
     *    ServerSideWindowHandler for jsf2
     *  - disable the initial redirect
     *  - use windowId=automatedEntryPoint as url parameter to force a new window context
     * @param facesContext current facesContext
     * @param config window config
     * @return true if the current request should be continued
     */
    private boolean processGetRequest(FacesContext facesContext, WindowContextConfig config)
    {
        boolean isUrlParameterSupported = config.isUrlParameterSupported();
        boolean useWindowIdForFirstPage = true;

        if(config instanceof JsfAwareWindowContextConfig)
        {
            useWindowIdForFirstPage = !((JsfAwareWindowContextConfig)config).isInitialRedirectDisable();
        }

        if(!isUrlParameterSupported)
        {
            useWindowIdForFirstPage = false;
        }

        if(useWindowIdForFirstPage)
        {
            String windowId = facesContext.getExternalContext()
                    .getRequestParameterMap().get(WINDOW_CONTEXT_ID_PARAMETER_KEY);

            if("automatedEntryPoint".equalsIgnoreCase(windowId))
            {
                return true;
            }

            WindowHandler windowHandler = ConversationUtils.getWindowHandler();
            windowId = resolveWindowContextId(isUrlParameterSupported, windowHandler);

            if(windowId == null)
            {
                redirect(facesContext, windowHandler);
                return false;
            }
        }
        return true;
    }

    private void redirect(FacesContext facesContext, WindowHandler windowHandler)
    {
        try
        {
            String targetURL = facesContext.getApplication()
                    .getViewHandler().getActionURL(facesContext, facesContext.getViewRoot().getViewId());

            // add requst-parameters e.g. for f:viewParam handling
            windowHandler.sendRedirect(FacesContext.getCurrentInstance().getExternalContext(), targetURL, true);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    //don't cleanup all window contexts (it would cause a side-effect with the access-scope and multiple windows
    private void cleanupInactiveConversations(WindowContext windowContext)
    {
        for (Conversation conversation : ((EditableWindowContext)windowContext).getConversations().values())
        {
            if (!((EditableConversation)conversation).isActive())
            {
                conversation.end();
            }
        }

        ((EditableWindowContext)windowContext).removeInactiveConversations();
    }
}
