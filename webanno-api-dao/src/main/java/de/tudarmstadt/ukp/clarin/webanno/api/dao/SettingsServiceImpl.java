/*
# * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.SettingsService;

@Component(SettingsService.SERVICE_NAME)
public class SettingsServiceImpl
    implements SettingsService
{
    @Value(value = "${ui.brat.sentences.number}")
    private int numberOfSentences;

    @PersistenceContext
    private EntityManager entityManager;

    public SettingsServiceImpl()
    {
        // Nothing to do
    }

    @Override
    public String getDatabaseDriverName()
    {
        final StringBuilder sb = new StringBuilder();
        Session session = entityManager.unwrap(Session.class);
        session.doWork(aConnection -> sb.append(aConnection.getMetaData().getDriverName()));

        return sb.toString();
    }

    @Override
    public int getNumberOfSentences()
    {
        return numberOfSentences;
    }
}
