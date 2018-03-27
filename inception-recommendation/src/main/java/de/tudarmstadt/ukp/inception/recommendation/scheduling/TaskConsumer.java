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

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.scheduling.tasks.Task;

public class TaskConsumer
    implements Runnable
{
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private BlockingQueue<Task> queue;
 
    public TaskConsumer(BlockingQueue<Task> aQueue)
    {
        this.queue = aQueue;
    }

    @Override
    public void run()
    {
        try {
            while (!Thread.interrupted()) {
                logger.info("Waiting for new task...");
                
                Task t = queue.take();
                
                try {
                    t.run();
                }
                catch (Exception e) {
                    logger.error("{} failed.", t, e);
                }

                logger.info("{} successfully.", t);
            }
        }
        catch (InterruptedException ie) {
            logger.info("Thread interrupted: ", ie);
        }
    }

}
