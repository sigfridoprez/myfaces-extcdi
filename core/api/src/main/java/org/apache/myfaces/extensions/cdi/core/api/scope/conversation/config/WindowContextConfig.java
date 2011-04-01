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
package org.apache.myfaces.extensions.cdi.core.api.scope.conversation.config;

import org.apache.myfaces.extensions.cdi.core.api.config.ConfigEntry;
import org.apache.myfaces.extensions.cdi.core.api.config.AbstractAttributeAware;
import org.apache.myfaces.extensions.cdi.core.api.config.CodiConfig;

import javax.enterprise.context.ApplicationScoped;

/**
 * Configuration for the {@link org.apache.myfaces.extensions.cdi.core.api.scope.conversation.WindowContext}
 * - it's customizable via the @Alternative or @Specializes mechanism of CDI.
 *
 * @author Gerhard Petracek
 */
@ApplicationScoped
public class WindowContextConfig extends AbstractAttributeAware implements CodiConfig
{
    private static final long serialVersionUID = 8159770064249255686L;

    protected WindowContextConfig()
    {
    }

    /**
     * Specifies if it is allowed to use URL params for forwarding the current window-id.
     * (deactivate it e.g. for higher security - in this case it's required to use a window id provided by a
     * component lib or a server-side window-handler)
     * 
     * @return true if it is allowed to add the window-id as URL parameter
     */
    @ConfigEntry
    public boolean isUrlParameterSupported()
    {
        return true;
    }

    /**
     * Allows to restrict window-ids.
     * With the default window handler (esp. for JSF 1.2), URLs have to contain the window-id.
     * If users bookmark these links, they could open 2-n tabs (with the bookmark) which have the same window-id.
     * It isn't possible to prevent it if the session is still active, but it's possible to prevent it as soon as the
     * session gets closed.
     *
     * @return true to allow window-ids which aren't generated by CODI, false otherwise
     */
    @ConfigEntry
    public boolean isUnknownWindowIdsAllowed()
    {
        return false;
    }

    /**
     * if set to <code>true</code> CODI will add a windowId=xxx parameter
     * while encoding each action URL.
     * @return true if the window-id should be added, false otherwise
     */
    @ConfigEntry
    public boolean isAddWindowIdToActionUrlsEnabled()
    {
        return false;
    }

    /**
     * Specifies the time for the timeout for a window. After a timeout is detected all beans which are only linked
     * to the window will be destroyed.
     *
     * @return the time for the timeout for a window
     */
    @ConfigEntry
    public int getWindowContextTimeoutInMinutes()
    {
        return 60;
    }

    /**
     * Restricts the number of active windows.
     *
     * @return limit for active windows
     */
    @ConfigEntry
    public int getMaxWindowContextCount()
    {
        return 64;
    }

    /**
     * Allows to activate the cleanup of empty window contexts to avoid cleanup e.g.
     * of the eldest window context instances if the max. count is reached.
     *
     * @return true for activating it, false otherwise
     */
    @ConfigEntry
    public boolean isCloseEmptyWindowContextsEnabled()
    {
        return false;
    }

    /**
     * Allows to restore the window-context before the component tree gets built.
     * 
     * @return true for activating it, false otherwise
     */
    @ConfigEntry
    public boolean isEagerWindowContextDetectionEnabled()
    {
        return true;
    }

    /*
     * event config
     */

    /**
     * Specifies if the
     * {@link org.apache.myfaces.extensions.cdi.core.api.scope.conversation.event.CreateWindowContextEvent}
     * will be fired.
     *
     * @return true if the event should be fired, false otherwise
     */
    @ConfigEntry
    public boolean isCreateWindowContextEventEnabled()
    {
        return false;
    }

    /**
     * Specifies if the
     * {@link org.apache.myfaces.extensions.cdi.core.api.scope.conversation.event.CloseWindowContextEvent}
     * will be fired.
     *
     * @return true if the event should be fired, false otherwise
     */
    @ConfigEntry
    public boolean isCloseWindowContextEventEnabled()
    {
        return false;
    }

    //boolean isResetWindowContextEventEnable();
}
