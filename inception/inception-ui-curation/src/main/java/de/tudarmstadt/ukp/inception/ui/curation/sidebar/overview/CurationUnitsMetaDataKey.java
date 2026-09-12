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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar.overview;

import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;

/**
 * Holds the computed curation units on the page rather than in the sidebar.
 */
public class CurationUnitsMetaDataKey
    extends MetaDataKey<CurationUnits>
{
    private static final long serialVersionUID = 6002172627318143995L;

    public final static CurationUnitsMetaDataKey INSTANCE = new CurationUnitsMetaDataKey();

    public static CurationUnits get(Component aOwner)
    {
        var units = aOwner.getMetaData(INSTANCE);
        if (units == null) {
            units = new CurationUnits();
            aOwner.setMetaData(INSTANCE, units);
        }
        return units;
    }
}
