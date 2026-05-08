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
package de.tudarmstadt.ukp.inception.diam.model.websocket;

import static java.util.Collections.newSetFromMap;
import static java.util.Collections.unmodifiableSet;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tools.jackson.databind.JsonNode;

public class ViewportState
{
    public record Subscription(String sessionId, String subscriptionId) {}

    private final ViewportDefinition vpd;

    private final Set<Subscription> subscriptions = newSetFromMap(new ConcurrentHashMap<>());

    private JsonNode json;

    public ViewportState(ViewportDefinition aVpd)
    {
        vpd = aVpd;
    }

    public ViewportDefinition getViewportDefinition()
    {
        return vpd;
    }

    public synchronized void setJson(JsonNode aJson)
    {
        json = aJson;
    }

    public synchronized JsonNode getJson()
    {
        return json;
    }

    public void removeSubscriptionsBySession(String aSessionId)
    {
        subscriptions.removeIf(p -> p.sessionId().equals(aSessionId));
    }

    public void addSubscription(String aSubscriberId, String aSubscriptionId)
    {
        subscriptions.add(new Subscription(aSubscriberId, aSubscriptionId));
    }

    public void removeSubscription(String aSessionId, String aSubscriptionId)
    {
        subscriptions.removeIf(p -> p.sessionId().equals(aSessionId)
                && p.subscriptionId().equals(aSubscriptionId));
    }

    public boolean hasSubscriptions()
    {
        return !subscriptions.isEmpty();
    }

    public Set<Subscription> getSubscriptions()
    {
        return unmodifiableSet(subscriptions);
    }
}
