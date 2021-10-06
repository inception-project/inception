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

import de.tudarmstadt.ukp.inception.search.ExecutionException;
import de.tudarmstadt.ukp.inception.search.Granularities;

import java.util.ArrayList;
import java.util.List;

public enum Formats
{
    CSV, TXT;

    public static Formats uiToInternal(String aFormat) throws ExecutionException
    {
        switch (aFormat) {
        case ".csv":
            return CSV;
        case ".txt":
            return TXT;
        default:
            throw new ExecutionException("The format " + aFormat + " is not supported");
        }
    }

    public static String internalToUi(Formats aFormat)
    {
        switch (aFormat) {
        case CSV:
            return ".csv";
        default:
            return ".txt";
        }
    }

    public static List<String> uiList()
    {
        List<String> formatList = new ArrayList<String>();
        for (Formats format : Formats.values()) {
            formatList.add(internalToUi(format));
        }
        return formatList;
    }

}
