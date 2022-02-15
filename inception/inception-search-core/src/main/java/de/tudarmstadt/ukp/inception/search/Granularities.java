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
package de.tudarmstadt.ukp.inception.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Granularities
{
    PER_DOCUMENT("per Document"), PER_SENTENCE("per Sentence");

    private static final Map<String, Granularities> UI_TO_INTERNAL = new HashMap<String, Granularities>();
    private static final Map<Granularities, String> INTERNAL_TO_UI = new HashMap<Granularities, String>();

    static {
        for (Granularities g : values()) {
            UI_TO_INTERNAL.put(g.uiName, g);
            INTERNAL_TO_UI.put(g, g.uiName);
        }
    }

    public final String uiName;

    private Granularities(String aUiName)
    {
        uiName = aUiName;
    }

    public static String internalToUi(Granularities aGranularity)
    {
        return INTERNAL_TO_UI.get(aGranularity);
    }

    public static Granularities uiToInternal(String aGranularity)
    {
        return UI_TO_INTERNAL.get(aGranularity);
    }

    public static List<String> uiList()
    {
        List<String> sortedList = new ArrayList<String>(INTERNAL_TO_UI.values());
        Collections.sort(sortedList);
        return sortedList;
    }

}
