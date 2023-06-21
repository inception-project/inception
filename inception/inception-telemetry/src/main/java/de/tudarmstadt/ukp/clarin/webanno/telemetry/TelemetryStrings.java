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
package de.tudarmstadt.ukp.clarin.webanno.telemetry;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.support.wicket.resource.Strings;

public class TelemetryStrings
    implements Serializable
{
    private final String productName;
    private final String save;

    public TelemetryStrings()
    {
        this(Strings.getString("product.name"), Strings.getString("save"));
    }

    public TelemetryStrings(String aProductName, String aSave)
    {
        super();
        productName = aProductName;
        save = aSave;
    }

    public String getProductName()
    {
        return productName;
    }

    public String getSave()
    {
        return save;
    }
}
