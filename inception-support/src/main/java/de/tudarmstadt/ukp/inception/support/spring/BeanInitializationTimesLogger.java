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
package de.tudarmstadt.ukp.inception.support.spring;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.identityHashCode;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.leftPad;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(prefix = "profiling", name = "bean-initialization-timing", //
        havingValue = "true", matchIfMissing = false)
@Component
public class BeanInitializationTimesLogger
    implements BeanPostProcessor
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Map<Object, Long> starts = new IdentityHashMap<>();

    private List<Pair<String, Long>> timings = new ArrayList<>();

    @Override
    public Object postProcessBeforeInitialization(Object aBean, String aBeanName)
        throws BeansException
    {
        if (starts != null) {
            starts.put(aBean, currentTimeMillis());
        }

        return BeanPostProcessor.super.postProcessBeforeInitialization(aBean, aBeanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        if (starts != null) {
            Long start = starts.get(bean);

            int id = identityHashCode(bean);

            if (start != null) {
                long now = currentTimeMillis();
                log.trace("Initialized bean: [{}] (id: {}) in {}ms", beanName, id, now - start);
                timings.add(Pair.of(beanName + " (id: " + id + ")", now - start));
            }
            else {
                log.trace("Initialized bean: [{}]({}) (unknown start) ", beanName, id);
            }
        }

        return bean;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event)
    {
        if (starts != null && timings != null && log.isDebugEnabled()) {
            Comparator<Pair<String, Long>> cmp = Comparator.comparingLong(Pair::getValue);
            cmp = cmp.reversed();

            int topN = 25;
            log.debug("Bean initialization times ({} slowest beans)", topN);
            int i = 1;
            for (Pair<String, Long> e : timings.stream().sorted(cmp).limit(topN)
                    .collect(toList())) {
                log.debug("{}) {}ms   {}", leftPad(String.valueOf(i), 2),
                        leftPad(String.valueOf(e.getValue()), 8), e.getKey());
                i++;
            }
        }

        // Once the application has started up, we do not want to track the times anymore. The
        // application may add beans or perform dependency injection on beans while its is running.
        // We must not track these / keep references to these because that would be a memory leak.
        starts = null;
        timings = null;
    }
}
