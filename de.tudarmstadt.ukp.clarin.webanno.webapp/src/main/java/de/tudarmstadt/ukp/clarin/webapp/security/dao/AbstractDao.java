/*******************************************************************************
 * Copyright 2013
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webapp.security.dao;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractDao<T, K>
{
	@PersistenceContext
	protected EntityManager entityManager;

	@Transactional
	public void create(final T aModel)
	{
		entityManager.persist(aModel);
		entityManager.flush();
	}

	@Transactional
	public T update(final T aModel)
	{
		return entityManager.merge(aModel);
	}

	public abstract int delete(final K aId);

	@Transactional
	public <TT> List<TT> getUnique(Class<?> aType, String aProperty, Class<TT> aPropertyType)
	{
		return entityManager.createQuery(
				"SELECT DISTINCT o." + aProperty + " FROM " + aType.getName() + " o ORDER BY o."
						+ aProperty, aPropertyType).getResultList();
	}

	/**
	 * Finds all entities that have the same type as the given example and all fields are equal to
	 * non-null fields in the example.
	 */
	protected <TT> CriteriaQuery<TT> queryByExample(TT aExample, String aOrderBy, boolean aAscending)
	{
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		@SuppressWarnings("unchecked")
		CriteriaQuery<TT> query = cb.createQuery((Class<TT>) aExample.getClass());
		@SuppressWarnings("unchecked")
		Root<TT> root = query.from((Class<TT>) aExample.getClass());
		query.select(root);

		List<Predicate> predicates = new ArrayList<Predicate>();
		BeanWrapper a = PropertyAccessorFactory.forBeanPropertyAccess(aExample);

		// Iterate over all properties
		for (PropertyDescriptor d : a.getPropertyDescriptors()) {
			Object value = a.getPropertyValue(d.getName());

			// Only consider writeable properties. This filters out e.g. the "class" (getClass())
			// property.
			if (value != null && a.isWritableProperty(d.getName())) {
				predicates.add(cb.equal(root.get(d.getName()), value));
			}
		}

		if (!predicates.isEmpty()) {
			query.where(predicates.toArray(new Predicate[predicates.size()]));
		}

		if (aOrderBy != null) {
			if (aAscending) {
				query.orderBy(cb.asc(root.get(aOrderBy)));
			}
			else {
				query.orderBy(cb.desc(root.get(aOrderBy)));
			}
		}

		return query;
	}
}
