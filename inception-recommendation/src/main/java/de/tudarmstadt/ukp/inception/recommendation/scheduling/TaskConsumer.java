/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.scheduling;

import static org.apache.commons.lang3.Validate.notNull;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.Task;

public class TaskConsumer
    implements Runnable
{
    private Logger log = LoggerFactory.getLogger(getClass());
    
    private ApplicationContext applicationContext;
    private BlockingQueue<Task> queue;
 
    public TaskConsumer(ApplicationContext aApplicationContext, BlockingQueue<Task> aQueue)
    {
        notNull(aQueue);
        notNull(aApplicationContext);
        
        queue = aQueue;
        applicationContext = aApplicationContext;
    }

    @Override
    public void run()
    {
        try {
            while (!Thread.interrupted()) {
                log.info("Waiting for new task...");
                
                Runnable task = queue.take();
                
                try {
                    AutowireCapableBeanFactory factory = applicationContext
                            .getAutowireCapableBeanFactory();
                    factory.autowireBean(task);
                    factory.initializeBean(task, "transientTask");
                    
                    task.run();
                }
                catch (Throwable e) {
                    log.error("{} failed.", task, e);
                }

                log.info("{} completed successfully.", task);
            }
        }
        catch (InterruptedException ie) {
            log.info("Thread interrupted: ", ie);
        }
    }

}
