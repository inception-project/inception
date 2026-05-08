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
package de.tudarmstadt.ukp.inception.export.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("document-import")
public class DocumentImportExportServicePropertiesImpl
    implements DocumentImportExportServiceProperties
{
    /**
     * Whether to run the CAS Doctor on every imported document. {@code AUTO} enables checks for
     * formats that give the user a lot of flexibility but also have great potential for importing
     * inconsistent data. {@code OFF} disables all checks on import; {@code ON} checks all formats,
     * even the more rigid ones.
     */
    private CasDoctorOnImportPolicy runCasDoctorOnImport = CasDoctorOnImportPolicy.AUTO;

    /** Token-count limit for imported documents. {@code 0} means no limit. */
    private int maxTokens = 2_000_000;

    /** Sentence-count limit for imported documents. {@code 0} means no limit. */
    private int maxSentences = 20_000;

    @Override
    public CasDoctorOnImportPolicy getRunCasDoctorOnImport()
    {
        return runCasDoctorOnImport;
    }

    public void setRunCasDoctorOnImport(CasDoctorOnImportPolicy aRunCasDoctorOnImport)
    {
        runCasDoctorOnImport = aRunCasDoctorOnImport;
    }

    @Override
    public int getMaxTokens()
    {
        return maxTokens;
    }

    public void setMaxTokens(int aMaxTokens)
    {
        maxTokens = aMaxTokens;
    }

    @Override
    public int getMaxSentences()
    {
        return maxSentences;
    }

    public void setMaxSentences(int aMaxSentences)
    {
        maxSentences = aMaxSentences;
    }
}
