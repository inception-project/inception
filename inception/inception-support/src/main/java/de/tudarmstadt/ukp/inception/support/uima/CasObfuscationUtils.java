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
package de.tudarmstadt.ukp.inception.support.uima;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.findAllFeatureStructures;
import static org.apache.uima.cas.CAS.FEATURE_BASE_NAME_SOFA;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCopier;
import org.apache.uima.util.CasCreationUtils;
import org.dkpro.core.api.xml.type.XmlAttribute;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlNode;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.inception.support.text.TextUtils;

public class CasObfuscationUtils
{
    public static void obfuscateCasInPlace(CAS aCas) throws UIMAException
    {
        var originalDocumentLanguage = aCas.getDocumentLanguage();

        // Obfuscate any primitive string features (and string arrays) on copied FSes.
        obfuscateStringFeatures(aCas);

        // Set obfuscated language after copying/obfuscating so it is not overwritten
        // by the CasCopier.
        aCas.setDocumentLanguage(TextUtils.obfuscate(originalDocumentLanguage));
    }

    public static CAS createObfuscatedClone(CAS aCas) throws UIMAException
    {
        if (aCas == null) {
            return null;
        }

        // Create a fresh CAS and initialize its CAS manager (type system, indexes,
        // type priorities) from the source CAS manager.
        var targetCas = createCasUsingTemplate(aCas);

        // Set obfuscated document text on the target first so offsets remain valid
        // while copying annotations.
        targetCas.setDocumentText(obfuscateDocumentText(aCas));

        // Copy all feature structures (annotations, metadata) into the target CAS.
        CasCopier.copyCas(aCas, targetCas, false);

        obfuscateCasInPlace(targetCas);

        return targetCas;
    }

    private static CAS createCasUsingTemplate(CAS aCas) throws ResourceInitializationException
    {
        var sourceReal = WebAnnoCasUtil.getRealCas(aCas);
        var sourceImpl = (CASImpl) sourceReal;
        var casMgr = Serialization.serializeCASMgr(sourceImpl);

        var targetCas = CasCreationUtils.createCas();
        targetCas.getJCasImpl().getCasImpl().getBinaryCasSerDes()
                .setupCasFromCasMgrSerializer(casMgr);
        return targetCas;
    }

    private static String obfuscateDocumentText(CAS aCas)
    {
        var originalText = aCas.getDocumentText();
        if (originalText == null) {
            return null;
        }

        var obfuscatedText = TextUtils.obfuscate(originalText);
        if (obfuscatedText.length() != originalText.length()) {
            throw new IllegalStateException("Obfuscated text length differs from original");
        }

        return obfuscatedText;
    }

    static void obfuscateStringFeatures(CAS aCas)
    {
        var reachable = findAllFeatureStructures(aCas);

        for (var fs : reachable) {
            if (fs instanceof SofaFS) {
                // Can't use standard set methods with SofaFS features.
                continue;
            }

            if (fs instanceof DocumentMetaData) {
                // Can't obfuscate DMD otherwise we run into problems on exporters that merge data
                // back into original documents or otherwise need to access e.g. the source document
                continue;
            }

            if (fs instanceof XmlNode || fs instanceof XmlDocument || fs instanceof XmlAttribute) {
                // We need to maintain this, otherwise document layout breaks e.g. for HTML files
                continue;
            }

            // Note: DocumentMetaData is obfuscated like other string features so that
            // exported copies do not leak identifying metadata.

            for (var f : fs.getType().getFeatures()) {
                // Skip sofa references and non-primitive features
                if (FEATURE_BASE_NAME_SOFA.equals(f.getShortName())) {
                    continue;
                }

                if (f.getRange().isPrimitive()) {
                    if (TYPE_NAME_STRING.equals(f.getRange().getName())) {
                        var val = fs.getFeatureValueAsString(f);
                        if (val != null) {
                            fs.setStringValue(f, TextUtils.obfuscate(val));
                        }
                    }
                }
                else {
                    // Handle string arrays
                    if (TYPE_NAME_STRING_ARRAY.equals(f.getRange().getName())) {
                        var arrFs = (StringArrayFS) fs.getFeatureValue(f);
                        if (arrFs != null) {
                            for (int i = 0; i < arrFs.size(); i++) {
                                var s = arrFs.get(i);
                                if (s != null) {
                                    arrFs.set(i, TextUtils.obfuscate(s));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
