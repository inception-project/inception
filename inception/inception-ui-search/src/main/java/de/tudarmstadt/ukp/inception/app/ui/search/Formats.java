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
package de.tudarmstadt.ukp.inception.app.ui.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum Formats
{
    CSV(".csv"), TXT(".txt");

    private static final Map<String, Formats> UI_TO_INTERNAL = new HashMap<String, Formats>();
    private static final Map<Formats, String> INTERNAL_TO_UI = new HashMap<Formats, String>();

    static {
        for (Formats f : values()) {
            UI_TO_INTERNAL.put(f.uiName, f);
            INTERNAL_TO_UI.put(f, f.uiName);
        }
    }

    public final String uiName;

    private Formats(String aUiName)
    {
        uiName = aUiName;
    }

    public static Formats uiToInternal(String aFormat)
    {
        return UI_TO_INTERNAL.get(aFormat);
    }

    public static String internalToUi(Formats aFormat)
    {
        return INTERNAL_TO_UI.get(aFormat);
    }

    public static List<String> uiList()
    {
        List<String> sortedList = new ArrayList<String>(INTERNAL_TO_UI.values());
        Collections.sort(sortedList);
        return sortedList;
    }

}
