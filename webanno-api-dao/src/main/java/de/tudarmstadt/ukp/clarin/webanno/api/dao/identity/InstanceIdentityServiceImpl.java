/*
 * Copyright 2019
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao.identity;

import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.identity.InstanceIdentityService;
import de.tudarmstadt.ukp.clarin.webanno.model.InstanceIdentity;

@Component
public class InstanceIdentityServiceImpl implements InstanceIdentityService
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
        log.info("Instance identity: {}", id.getId());
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
