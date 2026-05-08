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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Testcontainers(disabledWithoutDocker = true)
class KafkaWebhookDriverTest
{
    @Container
    static KafkaContainer kafka = new KafkaContainer("apache/kafka")
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    private KafkaWebhookDriver sut;

    @BeforeEach
    void setup()
    {
        sut = new KafkaWebhookDriver();
    }

    @Test
    void testKafkaAvailable()
    {
        assertThat(kafka.isRunning()).isTrue();
    }

    @Test
    void testSendAndReceive() throws Exception
    {
        var webhook = new Webhook();
        webhook.setEnabled(true);
        webhook.setUrl(kafka.getBootstrapServers());

        var topic = "notifications";

        var consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", kafka.getBootstrapServers());
        consumerProps.put("group.id", "test-group");
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("key.serializer", StringSerializer.class.getName());
        consumerProps.put("key.deserializer", StringDeserializer.class.getName());
        consumerProps.put("value.serializer", StringSerializer.class.getName());
        consumerProps.put("value.deserializer", StringDeserializer.class.getName());

        try (var consumer = new KafkaConsumer<String, String>(consumerProps)) {
            consumer.subscribe(asList(topic));

            sut.sendNotification(topic, "Hello Kafka!", webhook);

            var records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isGreaterThan(0);

            var record = records.iterator().next();
            assertThat(record.topic()).isEqualTo("notifications");
            assertThat(JSONUtil.fromJsonString(String.class, record.value()))
                    .isEqualTo("Hello Kafka!");
        }
    }
}
