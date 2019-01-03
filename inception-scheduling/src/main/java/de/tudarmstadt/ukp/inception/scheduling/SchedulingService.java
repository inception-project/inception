/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.scheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SchedulingService
        implements DisposableBean
{
    private static final Logger log = LoggerFactory.getLogger(SchedulingService.class);
    private static final int NUMBER_OF_THREADS = 10;

    private final ApplicationContext applicationContext;
    private final ExecutorService executor;

    @Autowired
    public SchedulingService(ApplicationContext aApplicationContext)
    {
        applicationContext = aApplicationContext;

        executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    }

    public void enqueue(Runnable aRunnable)
    {
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(aRunnable);
        factory.initializeBean(aRunnable, "transientTask");

        executor.execute(aRunnable);
    }

    public void stopAllTasksForUser(String aUserName)
    {
        // TODO: Implement me
    }

    @Override
    public void destroy() throws Exception
    {
        log.info("Shutting down scheduling service!");
        executor.shutdownNow();
    }
}
