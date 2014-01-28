/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.support;

import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.SingularAttribute;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hibernate.proxy.HibernateProxyHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.DirectFieldAccessor;

import de.tudarmstadt.ukp.clarin.webanno.model.support.spring.ApplicationContextProvider;

/**
 * Wicket model implementation which makes sure that s persistent model object is always attached to
 * an entity manager. Requires Hibernate and requires that the {@link ApplicationContextProvider}
 * is present in the Spring context.
 *
 * @author Richard Eckart de Castilho
 */
public class EntityModel<T>
    extends LoadableDetachableModel<T>
{
    private static final long serialVersionUID = 1L;

    private Class<T> entityClass;
    private Number id;

    @PersistenceContext
    private transient EntityManager entityManager;

    public EntityModel(T aEntity)
    {
        analyze(aEntity);
    }

    @Override
    protected T load()
    {
        if (id == null || id.longValue() == 0) {
            return BeanUtils.instantiate(entityClass);
        }
        else {
            T entity = getEntityManager().find(entityClass, id);
            return entity;
        }
    }

    @Override
    public void detach()
    {
        if (isAttached()) {
            // The ID may have change, e.g. because the object has been persisted meanwhile. Thus
            // we need to analyze it again before it is detached (in the Wicket sense).
            analyze(getObject());
            super.detach();
        }
    }

    private void analyze(T aObject)
    {
        if (aObject != null) {
            entityClass = HibernateProxyHelper.getClassWithoutInitializingProxy(aObject);

            String idProperty = null;
            Metamodel metamodel = getEntityManager().getMetamodel();
            EntityType entity = metamodel.entity(entityClass);
            Set<SingularAttribute> singularAttributes = entity.getSingularAttributes();
            for (SingularAttribute singularAttribute : singularAttributes) {
                if (singularAttribute.isId()) {
                    idProperty = singularAttribute.getName();
                    break;
                }
            }
            if (idProperty == null) {
                throw new RuntimeException("id field not found");
            }

            DirectFieldAccessor accessor = new DirectFieldAccessor(aObject);
            id = (Number) accessor.getPropertyValue(idProperty);
        }
        else {
            entityClass = null;
            id = null;
        }
    }

    private EntityManager getEntityManager()
    {
        if (entityManager == null) {
            ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory()
                    .autowireBean(this);
        }
        return entityManager;
    }
}
