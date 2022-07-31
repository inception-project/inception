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

import java.util.List;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;

/**
 * @param <T>
 *            the traits type. If no traits are supported, this should be {@link Void}.
 */
public interface TelemetrySupport<T>
{
    String getId();

    String getName();

    /**
     * @return if the support is valid. The support is valid if the user has made all necessary
     *         choices. While a support does not have valid settings, no telemetry must be
     *         submitted.
     */
    boolean hasValidSettings();

    /**
     * @return the version of the support. When the version of a telemetry support changes, the user
     *         should be presented with the settings again.
     */
    int getVersion();

    /**
     * Returns a Wicket component to configure the telemetry support.
     * 
     * @param aId
     *            a markup ID.
     * @param aTelemetryModel
     *            a model holding the annotation feature for which the traits editor should be
     *            created.
     * @return the editor component .
     */
    default Panel createTraitsEditor(String aId, IModel<TelemetrySettings> aTelemetryModel)
    {
        return new EmptyPanel(aId);
    }

    /**
     * Read the traits for this {@link TelemetrySupport}. If traits are supported, then this method
     * must be overwritten. A typical implementation would read the traits from a JSON string stored
     * in {@link TelemetrySettings#getTraits}, but it would also possible to load the traits from a
     * database table.
     * 
     * @param aSettings
     *            the settings whose traits should be obtained.
     * @return the traits.
     */
    default T readTraits(TelemetrySettings aSettings)
    {
        return null;
    }

    /**
     * Write the traits for this {@link TelemetrySupport}. If traits are supported, then this method
     * must be overwritten. A typical implementation would write the traits from to JSON string
     * stored in {@link TelemetrySettings#setTraits}, but it would also possible to store the traits
     * from a database table.
     * 
     * @param aSettings
     *            the settings whose traits should be written.
     * @param aTraits
     *            the traits.
     */
    default void writeTraits(TelemetrySettings aSettings, T aTraits)
    {
        aSettings.setTraits(null);
    }

    /**
     * @return a list of details containing the data which the telemetry support sends home.
     * 
     *         <b>Note:</b> Make sure this map actually contains all the data that is being sent!
     *         This is used to display the details of what is being sent to the user - it is not
     *         used to pick up the actual data which is sent home! So make sure the method which
     *         sents the data and this method are in sync.
     */
    List<TelemetryDetail> getDetails();

    void acceptAll(T aTraits);

    void rejectAll(T aTraits);
}
