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
package de.tudarmstadt.ukp.clarin.webanno.telemetry;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.telemetry.event.TelemetrySettingsSavedEvent;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;

@Component
public class TelemetryServiceImpl
    implements TelemetryService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
    private EntityManager entityManager;
    
    private final List<TelemetrySupport> telemetrySupportsProxy;
    private final ApplicationEventPublisher eventPublisher;
    
    private List<TelemetrySupport> telemetrySupports;
    
    public TelemetryServiceImpl(
            @Lazy @Autowired(required = false) List<TelemetrySupport> aTelemetrySupports,
            ApplicationEventPublisher aEventPublisher)
    {
        telemetrySupportsProxy = aTelemetrySupports;
        eventPublisher = aEventPublisher;
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    public void init()
    {
        List<TelemetrySupport> tsp = new ArrayList<>();

        if (telemetrySupportsProxy != null) {
            tsp.addAll(telemetrySupportsProxy);
            AnnotationAwareOrderComparator.sort(tsp);
        
            for (TelemetrySupport ts : tsp) {
                log.info("Found telemetry support: {}", ts.getId());
            }
        }
        
        telemetrySupports = Collections.unmodifiableList(tsp);
    }
    
    @Override
    public List<TelemetrySupport> getTelemetrySupports()
    {
        return telemetrySupports;
    }
    
    @Override
    public Optional<TelemetrySupport> getTelemetrySuppport(String aSupport)
    {
        return telemetrySupports.stream()
                .filter(ts -> ts.getId().equals(aSupport))
                .findFirst();
    }
    
    @Override
    @Transactional
    public List<TelemetrySettings> listSettings()
    {
        String query = "FROM TelemetrySettings";
        
        return entityManager.createQuery(query, TelemetrySettings.class)
            .getResultList();
    }
    
    @Override
    @Transactional
    public <T> Optional<TelemetrySettings> readSettings(TelemetrySupport<T> aSupport)
    {
        String query = 
                "FROM TelemetrySettings " + 
                "WHERE support = :support";
        
        List<TelemetrySettings> results = entityManager.createQuery(query, TelemetrySettings.class)
            .setParameter("support", aSupport.getId())
            .getResultList();
        
        if (results.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(results.get(0));
    }    
    
    @Override
    @Transactional
    public <T> TelemetrySettings readOrCreateSettings(TelemetrySupport<T> aSupport)
    {
        String query = 
                "FROM TelemetrySettings " + 
                "WHERE support = :support";
        
        List<TelemetrySettings> results = entityManager.createQuery(query, TelemetrySettings.class)
            .setParameter("support", aSupport.getId())
            .getResultList();
        
        if (!results.isEmpty()) {
            return results.get(0);
        }
        else {
            return new TelemetrySettings(aSupport);
        }
    }
    
    @Transactional
    private void writeSettings(TelemetrySettings aSettings)
    {
        if (isNull(aSettings.getId())) {
            entityManager.persist(aSettings);
        }
        else {
            entityManager.merge(aSettings);
        }
    }
    
    @Override
    @Transactional
    public void writeAllSettings(List<TelemetrySettings> aSettings)
    {
        for (TelemetrySettings settings : aSettings) {
            writeSettings(settings);
        }
        
        eventPublisher.publishEvent(new TelemetrySettingsSavedEvent(this));
    }
}
