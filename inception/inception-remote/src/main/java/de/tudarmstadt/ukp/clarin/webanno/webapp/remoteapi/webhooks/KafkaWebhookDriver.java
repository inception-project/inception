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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class KafkaWebhookDriver
    implements WebhookDriver
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String ID = "kafka";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public void sendNotification(String aTopic, Object aMessage, Webhook aHook)
        throws IOException, InterruptedException
    {
        var producerProbs = new Properties();

        // These settings can be overridden by the user in the properties of the webhook
        producerProbs.put("bootstrap.servers", aHook.getUrl());
        producerProbs.put("acks", "1");

        // Apply the user overrides
        if (aHook.getProperties() != null) {
            producerProbs.putAll(aHook.getProperties());
        }

        // Our serializer and deserializer settings need to always be in effect
        producerProbs.put("key.serializer", StringSerializer.class.getName());
        producerProbs.put("key.deserializer", StringDeserializer.class.getName());
        producerProbs.put("value.serializer", StringSerializer.class.getName());
        producerProbs.put("value.deserializer", StringDeserializer.class.getName());

        var json = JSONUtil.toJsonString(aMessage);

        try (var producer = new KafkaProducer<Object, String>(producerProbs)) {
            var record = new ProducerRecord<>(aTopic, json);
            var metadata = producer.send(record).get(); // synchronous send
            LOG.debug("Sent to partition [{}]  with offset [{}]", metadata.offset(),
                    metadata.partition());
        }
        catch (ExecutionException e) {
            throw new IOException(e);
        }
    }
}
