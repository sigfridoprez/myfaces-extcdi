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
package org.apache.myfaces.extensions.cdi.javaee.jsf.impl.scope.conversation.named;

import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.named.ConversationScoped;
import org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ConversationManager;
import org.apache.myfaces.extensions.cdi.core.api.tools.annotate.DefaultAnnotation;
import org.apache.myfaces.extensions.cdi.javaee.jsf.api.qualifier.Jsf;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.context.FacesContext;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * @author Gerhard Petracek
 */
public class NamedConversationContextAdapter implements Context
{
    private BeanManager beanManager;

    public NamedConversationContextAdapter(BeanManager beanManager)
    {
        this.beanManager = beanManager;
    }

    /**
     * @return annotation of the codi conversation scope
     */
    public Class<? extends Annotation> getScope()
    {
        return ConversationScoped.class;
    }

    /**
     * @param component descriptor of the bean
     * @param creationalContext context for creating a bean
     * @return a scoped bean-instance
     */
    public <T> T get(Contextual<T> component, CreationalContext<T> creationalContext)
    {
        if (component instanceof Bean)
        {
            ConversationManager conversationManager = resolveConversationManager();

            T beanInstance = component.create(creationalContext);
            scopeBeanInstance((Bean<T>)component, beanInstance, conversationManager);
            return beanInstance;
        }

        Class invalidComponentClass = component.create(creationalContext).getClass();
        throw new IllegalStateException(invalidComponentClass + " is no valid conversation scoped bean");
    }

    /**
     * @param component descriptor of the bean
     * @return an instance of the requested bean if it already exists in the current
     * {@link org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ConversationContext}
     * null otherwise
     */
    public <T> T get(Contextual<T> component)
    {
        if (component instanceof Bean)
        {
            Bean<T> foundBean = ((Bean<T>) component);
            ConversationManager conversationManager = resolveConversationManager();

            return resolveBeanInstance(foundBean, conversationManager);
        }
        return null;
    }

    /**
     * @return true as soon as JSF is active (the {@link org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ConversationContext}
     * will be created automatically
     */
    public boolean isActive()
    {
        return FacesContext.getCurrentInstance() != null;
    }

    /**
     * @return an instance of a custom (the default) {@link ConversationManager}
     */
    private ConversationManager resolveConversationManager()
    {
        Bean<ConversationManager> conversationManagerBean = resolveConversationManagerBean();
        CreationalContext<ConversationManager> conversationManagerCreationalContext = this.beanManager.createCreationalContext(conversationManagerBean);
        return conversationManagerBean.create(conversationManagerCreationalContext);
    }

    /**
     * @return the descriptor of a custom {@link ConversationManager} with the qualifier {@link Jsf} or
     * the descriptor of the default implementation provided by this module
     */
    private Bean<ConversationManager> resolveConversationManagerBean()
    {
        Set<?> conversationManagerBeans = this.beanManager.getBeans(ConversationManager.class, DefaultAnnotation.of(Jsf.class));

        if(conversationManagerBeans.isEmpty())
        {
            conversationManagerBeans = getDefaultConversationManager();
        }

        if(conversationManagerBeans.size() != 1)
        {
            throw new IllegalStateException(conversationManagerBeans.size() + " conversation-managers were found");
        }
        //noinspection unchecked
        return (Bean<ConversationManager>)conversationManagerBeans.iterator().next();
    }

    /**
     * @return the descriptor of the default {@link ConversationManager}
     */
    private Set<Bean<?>> getDefaultConversationManager()
    {
        return this.beanManager.getBeans(ConversationManager.class);
    }

    /**
     * @param beanDescriptor descriptor of the requested bean
     * @param conversationManager the current {@link ConversationManager}
     * @return the instance of the requested bean if it exists in the current {@link org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ConversationContext}
     * null otherwise
     */
    private <T> T resolveBeanInstance(Bean<T> beanDescriptor, ConversationManager conversationManager)
    {
        //TODO
        return null;
    }

    /**
     * Store the given bean in the {@link org.apache.myfaces.extensions.cdi.core.api.scope.conversation.ConversationContext}
     *
     * @param beanDescriptor descriptor of the current bean
     * @param beanInstance bean to save in the current conversation
     * @param conversationManager current {@link ConversationManager}
     */
    private <T> void scopeBeanInstance(Bean<T> beanDescriptor, T beanInstance, ConversationManager conversationManager)
    {
        //TODO
    }
}
