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
package org.apache.myfaces.extensions.cdi.jsf2.impl.navigation;

import org.apache.myfaces.extensions.cdi.jsf.api.config.view.Page;
import org.apache.myfaces.extensions.cdi.jsf.impl.config.view.ViewConfigCache;
import org.apache.myfaces.extensions.cdi.jsf.impl.config.view.spi.ViewConfigEntry;
import org.apache.myfaces.extensions.cdi.jsf.impl.util.JsfUtils;
import org.apache.myfaces.extensions.cdi.jsf.impl.util.RequestParameter;

import javax.faces.application.NavigationCase;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;

/**
 * Destructive operations aren't supported (compared to the SubKeyMap used in MyFaces).
 * Reason: It isn't allowed to remove navigation cases btw. View-Configs
 * 
 * @author Gerhard Petracek
 */
class NavigationCaseMapWrapper implements Map<String, Set<NavigationCase>>
{
    private Map<String, Set<NavigationCase>> wrappedNavigationCaseMap;
    private final Map<String, Set<NavigationCase>> viewConfigBasedNavigationCaseCache;

    public NavigationCaseMapWrapper(Map<String, Set<NavigationCase>> navigationCases)
    {
        this.wrappedNavigationCaseMap = navigationCases;
        this.viewConfigBasedNavigationCaseCache = createViewConfigBasedNavigationCases(false);
    }

    private Map<String, Set<NavigationCase>> createViewConfigBasedNavigationCases(boolean allowParameters)
    {
        Map<String, Set<NavigationCase>> result = new HashMap<String, Set<NavigationCase>>();

        Collection<ViewConfigEntry> viewConfigEntries = ViewConfigCache.getViewConfigEntries();

        if(!viewConfigEntries.isEmpty())
        {
            Set<NavigationCase> navigationCase = new HashSet<NavigationCase>();

            Map<String, List<String>> parameters = null;

            if(allowParameters)
            {
                parameters = resolveParameters();
            }

            boolean includeParameters;

            for(ViewConfigEntry entry : viewConfigEntries)
            {
                includeParameters = Page.ViewParameter.INCLUDE.equals(entry.getViewParameter());
                navigationCase.add(new NavigationCase("*",
                                                      null,
                                                      null,
                                                      null,
                                                      entry.getViewId(),
                                                      includeParameters ? parameters : null,
                                                      Page.NavigationMode.REDIRECT.equals(entry.getNavigationMode()),
                                                      includeParameters));

                result.put(entry.getViewId(), navigationCase);
            }
        }
        return result;
    }

    private Map<String, List<String>> resolveParameters()
    {
        Map<String, List<String>> parameters = new HashMap<String, List<String>>();

        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();

        for(RequestParameter parameter : JsfUtils.getRequestParameters(externalContext, true))
        {
            parameters.put(parameter.getKey(), parameter.getValueList());
        }

        return parameters;
    }

    public int size()
    {
        return this.wrappedNavigationCaseMap.size() +
                this.viewConfigBasedNavigationCaseCache.size();
    }

    public boolean isEmpty()
    {
        return this.wrappedNavigationCaseMap.isEmpty() &&
                this.viewConfigBasedNavigationCaseCache.isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return this.wrappedNavigationCaseMap.containsKey(key) ||
                this.viewConfigBasedNavigationCaseCache.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return this.wrappedNavigationCaseMap.containsValue(value) ||
                this.viewConfigBasedNavigationCaseCache.containsValue(value);
    }

    public Set<NavigationCase> get(Object key)
    {
        Set<NavigationCase> result = this.wrappedNavigationCaseMap.get(key);

        if(result == null)
        {
            return createViewConfigBasedNavigationCases(true).get(key);
        }
        return result;
    }

    public Set<NavigationCase> put(String key, Set<NavigationCase> value)
    {
        return this.wrappedNavigationCaseMap.put(key, value);
    }

    public Set<NavigationCase> remove(Object key)
    {
        return this.wrappedNavigationCaseMap.remove(key);
    }

    public void putAll(Map<? extends String, ? extends Set<NavigationCase>> m)
    {
        this.wrappedNavigationCaseMap.putAll(m);
    }

    public void clear()
    {
        this.wrappedNavigationCaseMap.clear();
    }

    public Set<String> keySet()
    {
        Set<String> result = new HashSet<String>();
        result.addAll(this.wrappedNavigationCaseMap.keySet());
        result.addAll(this.viewConfigBasedNavigationCaseCache.keySet());
        return result;
    }

    public Collection<Set<NavigationCase>> values()
    {
        Collection<Set<NavigationCase>> result = new HashSet<Set<NavigationCase>>();

        result.addAll(this.wrappedNavigationCaseMap.values());
        result.addAll(createViewConfigBasedNavigationCases(true).values());
        return result;
    }

    public Set<Entry<String, Set<NavigationCase>>> entrySet()
    {
        Set<Entry<String, Set<NavigationCase>>> result = new HashSet<Entry<String, Set<NavigationCase>>>();

        result.addAll(this.wrappedNavigationCaseMap.entrySet());
        result.addAll(createViewConfigBasedNavigationCases(true).entrySet());
        return result;
    }
}