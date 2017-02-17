/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public interface ImportExportService
{
    // --------------------------------------------------------------------------------------------
    // Methods related to import/export data formats
    // --------------------------------------------------------------------------------------------

    /**
     * Returns the labels on the UI for the format of the {@link SourceDocument} to be read from a
     * properties File
     *
     * @return labels of readable formats.
     */
    List<String> getReadableFormatLabels();

    /**
     * Returns the Id of the format for the {@link SourceDocument} to be read from a properties File
     *
     * @param label
     *            the label.
     *
     * @return the ID.
     */
    String getReadableFormatId(String label);

    /**
     * Returns formats of the {@link SourceDocument} to be read from a properties File
     *
     * @return the formats.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    @SuppressWarnings("rawtypes")
    Map<String, Class> getReadableFormats()
        throws IOException, ClassNotFoundException;

    /**
     * Returns the labels on the UI for the format of {@link AnnotationDocument} while exporting
     *
     * @return the labels.
     */
    List<String> getWritableFormatLabels();

    /**
     * Returns the Id of the format for {@link AnnotationDocument} while exporting
     *
     * @param label
     *            the label.
     * @return the ID.
     */
    String getWritableFormatId(String label);

    /**
     * Returns formats of {@link AnnotationDocument} while exporting
     *
     * @return the formats.
     * @throws IOException
     *             if an I/O error occurs.
     * @throws ClassNotFoundException
     *             if a DKPro Core reader/writer cannot be loaded.
     */
    @SuppressWarnings("rawtypes")
    Map<String, Class> getWritableFormats()
        throws IOException, ClassNotFoundException;
}
