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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.identity;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.InstanceIdentity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class InstanceIdentityServiceImpl
    implements InstanceIdentityService
{
    private Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;

    @EventListener(ApplicationReadyEvent.class)
    @Order(-10)
    @Transactional
    public void onStart()
    {
        // Fetch the instance identity or generate it if we don't have one yet
        InstanceIdentity id = getInstanceIdentity();
        log.debug("Instance identity: {}", id.getId());
    }

    @Override
    @Transactional
    public InstanceIdentity getInstanceIdentity()
    {
        List<InstanceIdentity> instanceIDs = entityManager
                .createQuery("FROM InstanceIdentity", InstanceIdentity.class).getResultList();

        InstanceIdentity identity;
        if (instanceIDs.isEmpty()) {
            identity = new InstanceIdentity();
            identity.setId(UUID.randomUUID().toString());
            entityManager.persist(identity);
        }
        else {
            identity = instanceIDs.get(0);
        }

        return identity;
    }
}
