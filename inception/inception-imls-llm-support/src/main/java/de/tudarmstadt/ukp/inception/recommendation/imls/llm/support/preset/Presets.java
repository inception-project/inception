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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.preset;

import static java.util.Collections.emptyList;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.yaml.YamlUtil;

public final class Presets
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final WatchedResourceFile<ArrayList<Preset>> PRESETS;

    static {
        var presetsResource = MethodHandles.lookup().lookupClass().getResource("presets.yaml");
        PRESETS = new WatchedResourceFile<>(presetsResource, is -> YamlUtil.getObjectMapper()
                .readValue(is, new TypeReference<ArrayList<Preset>>()
                {
                }));
    }

    private Presets()
    {
        // No instances;
    }

    public static List<Preset> getPresets()
    {
        try {
            if (PRESETS.get().isPresent()) {
                return PRESETS.get().get();
            }

            return emptyList();
        }
        catch (Exception e) {
            LOG.error("Unable to load presets", e);
            return Collections.emptyList();
        }
    }
}
