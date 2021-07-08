/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.experimental.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;

@Component
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
public class AnnotationSystemAPIServiceImpl
    implements AnnotationSystemAPIService, Serializable
{
    private static final long serialVersionUID = -2755044373072834849L;
    private final EntityManager entityManager;
    private final SchedulingService schedulingService;

    @Autowired
    public AnnotationSystemAPIServiceImpl(EntityManager aEntityManager,
            SchedulingService aSchedulingService)
    {
        entityManager = aEntityManager;
        schedulingService = aSchedulingService;
    }

    @Override
    @Transactional
    public AnnotationLayer getAnnotationLayer(String aName)
    {
        try {
            return entityManager
                    .createQuery(
                            "SELECT al " + "FROM AnnotationLayer al " + "WHERE al.name = :name",
                            AnnotationLayer.class)
                    .setParameter("name", aName).getResultList().get(0);
        }
        catch (NoResultException e) {
            System.err.println("ANNOTATION LAYER NOT FOUND. " + Arrays.toString(e.getStackTrace()));
            return null;
        }
    }

    @Override
    @Transactional
    public List getAllAnnotationLayers()
    {
        return entityManager.createQuery("SELECT name " +
            "FROM AnnotationLayer " +
            "GROUP BY name").getResultList();
    }
}
