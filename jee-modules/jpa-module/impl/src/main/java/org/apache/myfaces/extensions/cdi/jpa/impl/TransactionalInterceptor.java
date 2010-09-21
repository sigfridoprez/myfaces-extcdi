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
package org.apache.myfaces.extensions.cdi.jpa.impl;

import org.apache.myfaces.extensions.cdi.jpa.api.Transactional;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interceptor for wrapping transactional database requests.
 *
 * @author Mark Struberg
 */
@Interceptor
@Transactional
public class TransactionalInterceptor implements Serializable
{

    private @Inject BeanManager bm;
    private static transient ThreadLocal<AtomicInteger> refCount = new ThreadLocal<AtomicInteger>();

    protected final Logger logger = Logger.getLogger(TransactionalInterceptor.class.getName());

    /** key=qualifier name, value= EntityManager */
    private static transient ThreadLocal<HashMap<String, EntityManager>> ems =
            new ThreadLocal<HashMap<String, EntityManager>>();

    public class AnyLiteral extends AnnotationLiteral<Any> implements Any
    {
    }

    /** 1 ms  in nanoTime ticks */
    final static long LONG_MILLISECOND = 1000000L;
    final static long LONG_RUNNING_THRESHOLD = 300L * LONG_MILLISECOND;

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception
    {
        Transactional t = context.getMethod().getAnnotation(Transactional.class);
        if (t == null)
        {
            t = context.getTarget().getClass().getAnnotation(Transactional.class);
        }
        
        Class q = Default.class;
        if (t != null)
        {
            q = t.qualifier();
        }
        
        Set<Bean<?>> emBeans = bm.getBeans(EntityManager.class, new AnyLiteral());
        if (emBeans == null)
        {
            throw new IllegalArgumentException("cannot find EntityManager");
        }
        Bean<EntityManager> bean = null;
        
        it:
        for (Bean<?> emBean : emBeans)
        {
            
            Set<Annotation> anns = emBean.getQualifiers();
            for (Annotation a : anns)
            {
                if (a.annotationType().equals(q))
                {
                    bean = (Bean<EntityManager>) emBean;
                    break it;
                }
            }
        }
        
        if (bean == null)
        {
            throw new IllegalArgumentException("cannot find EntityManager with Qualifier " + q.getName());
        }

        EntityManager entityManager = (EntityManager) bm.getReference(bean, EntityManager.class,
                                                                      bm.createCreationalContext(bean));

        if (ems.get() == null)
        {
            ems.set(new HashMap<String, EntityManager>());
        }
        ems.get().put(q.getName(), entityManager);
        // log.info("growing: " + ems.get().size());

        if (refCount.get() == null)
        {
            refCount.set(new AtomicInteger(0));
        }
         
        EntityTransaction transaction = entityManager.getTransaction();

        // used to store any exception we get from the services
        Exception firstException = null;

        try
        {
            if(!transaction.isActive())
            {
                transaction.begin();
            }
            refCount.get().incrementAndGet();
            
            return context.proceed();
            
        }
        catch(Exception e)
        {
            firstException = e;

            // we only cleanup and rollback all open transactions in the outermost interceptor!
            // this way, we allow inner functions to catch and handle exceptions properly.
            if (refCount.get().intValue() == 1)
            {
                for (EntityManager em: ems.get().values())
                {
                    transaction = em.getTransaction();
                    if (transaction != null && transaction.isActive())
                    {
                        try
                        {
                            transaction.rollback();
                        }
                        catch (Exception eRollback)
                        {
                            logger.log(Level.SEVERE, "Got additional Exception while subsequently " +
                                       "rolling back other SQL transactions", eRollback);
                        }

                    }
                }

                refCount.remove();

                // drop all EntityManagers from the ThreadLocal
                ems.remove();
            }

            // rethrow the exception
            throw e;

        }
        finally
        {
            if (refCount.get() != null)
            {
                refCount.get().decrementAndGet();


                // will get set if we got an Exception while committing
                // in this case, we rollback all later transactions too.
                boolean commitFailed = false;

                // commit all open transactions in the outermost interceptor!
                // this is a 'JTA for poor men' only, and will not guaranty
                // commit stability over various databases!
                if (refCount.get().intValue() == 0)
                {

                    // only commit all transactions if we didn't rollback
                    // them already
                    if (firstException == null)
                    {
                        for (EntityManager em: ems.get().values())
                        {
                            transaction = em.getTransaction();
                            if(transaction != null && transaction.isActive())
                            {
                                try
                                {
                                    if (!commitFailed)
                                    {
                                        transaction.commit();
                                    }
                                    else
                                    {
                                        transaction.rollback();
                                    }
                                }
                                catch (Exception e)
                                {
                                    firstException = e;
                                    commitFailed = true;
                                }
                            }
                        }
                    }

                    // finally remove all ThreadLocals
                    refCount.remove();
                    ems.remove();
                    if (commitFailed)
                    {
                        throw firstException;
                    }
                }

            }
        }

    }

}