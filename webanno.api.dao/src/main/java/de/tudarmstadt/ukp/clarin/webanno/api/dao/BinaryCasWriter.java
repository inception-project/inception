/*******************************************************************************
 * Copyright 2013
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.cas.impl.Serialization.serializeCAS;
import static org.apache.uima.cas.impl.Serialization.serializeCASMgr;
import static org.apache.uima.cas.impl.Serialization.serializeWithCompression;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;



import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionUtils;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.SerializedCasReader;

public class BinaryCasWriter
    extends JCasFileWriter_ImplBase
{
    /**
     * Location to write the type system to. If this is not set, a file called typesystem.xml will
     * be written to the output path. If this is set, it is expected to be a file relative to the
     * current work directory or an absolute file. <br>
     * If this parameter is set, the {@link #PARAM_COMPRESS} parameter has no effect on the type
     * system. Instead, if the type system file should be compressed or not is detected from the
     * file name extension (e.g. ".gz"). <br>
     * If this parameter is set, the type system and index repository are no longer serialized into
     * the same file as the test of the CAS. The {@link SerializedCasReader} can currently not read
     * such files. Use this only if you really know what you are doing.
     */
    public static final String PARAM_TYPE_SYSTEM_FILE = "typeSystemFile";
    @ConfigurationParameter(name = PARAM_TYPE_SYSTEM_FILE, mandatory = false)
    private File typeSystemFile;

    public static final String PARAM_FORMAT = "format";
    @ConfigurationParameter(name = PARAM_FORMAT, mandatory = true, defaultValue = "6+")
    private String format;

    public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
    @ConfigurationParameter(name=PARAM_FILENAME_SUFFIX, mandatory=true, defaultValue=".ser")
    private String filenameSuffix;

    private boolean typeSystemWritten;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        OutputStream docOS = null;
        try {
            docOS = getOutputStream(aJCas, filenameSuffix);

            if ("0".equals(format)) {
                serializeCAS(aJCas.getCas(), docOS);
            }
            else if ("4".equals(format)) {
                serializeWithCompression(aJCas.getCas(), docOS);
            }
            else if (format.startsWith("6")) {
                if ("6+".equals(format)) {
                    writeHeader(docOS);
                    writeTypeSystem(aJCas, docOS);
                }
                serializeWithCompression(aJCas.getCas(), docOS, aJCas.getTypeSystem());
            }
            else {
                throw new IllegalArgumentException("Unknown format [" + format
                        + "]. Must be 0, 4,  6, or 6+");
            }

            if (!typeSystemWritten) {
                writeTypeSystem(aJCas);
                typeSystemWritten = true;
            }
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
        finally {
            closeQuietly(docOS);
        }
    }

    private void writeTypeSystem(JCas aJCas)
        throws IOException
    {
        File typeOSFile;
        if (typeSystemFile != null) {
            typeOSFile = typeSystemFile;
        }
        else {
            typeOSFile = getTargetPath("typesystem", ".ser");
        }

        OutputStream typeOS = null;
        try {
            typeOS = CompressionUtils.getOutputStream(typeOSFile);
            writeTypeSystem(aJCas, typeOS);
        }
        finally {
            closeQuietly(typeOS);
        }
    }

    private void writeHeader(OutputStream aOS)
        throws IOException
    {
        byte[] header = new byte[] { 'D', 'K', 'P', 'r', 'o', '1' };
        DataOutputStream dataOS = new DataOutputStream(aOS);
        dataOS.write(header);
        dataOS.flush();
    }

    private void writeTypeSystem(JCas aJCas, OutputStream aOS)
        throws IOException
    {
        ObjectOutputStream typeOS = new ObjectOutputStream(aOS);
        CASMgrSerializer casMgrSerializer = serializeCASMgr(aJCas.getCasImpl());
        typeOS.writeObject(casMgrSerializer);
        typeOS.flush();
    }
}
