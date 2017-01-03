/*
 * Copyright 2010
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
 */
package de.tudarmstadt.ukp.clarin.webanno.xmi;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.TypeSystemUtil;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionUtils;

/**
 * UIMA XMI format writer.
 */
@TypeCapability(
        inputs={
                "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData"})
public class XmiWriter
	extends JCasFileWriter_ImplBase
{
    public static final String PARAM_PRETTY_PRINT = "prettyPrint";
    @ConfigurationParameter(name = PARAM_PRETTY_PRINT, mandatory = true, defaultValue = "true")
    private boolean prettyPrint;
    
	/**
	 * Location to write the type system to. If this is not set, a file called typesystem.xml will
	 * be written to the XMI output path. If this is set, it is expected to be a file relative
	 * to the current work directory or an absolute file.
	 * <br>
	 * If this parameter is set, the {@link #PARAM_COMPRESSION} parameter has no effect on the
	 * type system. Instead, if the file name ends in ".gz", the file will be compressed,
	 * otherwise not.
	 */
	public static final String PARAM_TYPE_SYSTEM_FILE = "typeSystemFile";
	@ConfigurationParameter(name=PARAM_TYPE_SYSTEM_FILE, mandatory=false)
	private File typeSystemFile;

	private boolean typeSystemWritten;

	@Override
	public void initialize(UimaContext aContext)
		throws ResourceInitializationException
	{
		super.initialize(aContext);

		typeSystemWritten = false;
	}

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        try (OutputStream docOS = getOutputStream(aJCas, ".xmi")) {
            XmiCasSerializer.serialize(aJCas.getCas(), null, docOS, prettyPrint, null);

            if (!typeSystemWritten || typeSystemFile == null) {
                writeTypeSystem(aJCas);
                typeSystemWritten = true;
            }
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void writeTypeSystem(JCas aJCas)
        throws IOException, CASRuntimeException, SAXException
    {
		@SuppressWarnings("resource")
        OutputStream typeOS = null;
		
        try {
    		if (typeSystemFile != null) {
    		    typeOS = CompressionUtils.getOutputStream(typeSystemFile);
    		}
    		else {
    		    typeOS = getOutputStream("typesystem", ".xml");
    		}

			TypeSystemUtil.typeSystem2TypeSystemDescription(aJCas.getTypeSystem()).toXML(typeOS);
		}
		finally {
			closeQuietly(typeOS);
		}
	}
}
