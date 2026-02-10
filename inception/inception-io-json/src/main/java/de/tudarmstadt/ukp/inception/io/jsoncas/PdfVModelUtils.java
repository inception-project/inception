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
package de.tudarmstadt.ukp.inception.io.jsoncas;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.util.CasCopier;

@Deprecated
class PdfVModelUtils
{
    private static final String PDF_CHUNK_TYPE = "org.dkpro.core.api.pdf.type.PdfChunk";
    private static final String PDF_PAGE_TYPE = "org.dkpro.core.api.pdf.type.PdfPage";

    private PdfVModelUtils()
    {
        // No instances
    }

    @Deprecated
    public static boolean containsPdfDocumentStructure(CAS aCas)
    {
        var ts = aCas.getTypeSystem();
        if (ts.getType(PDF_PAGE_TYPE) == null) {
            return false;
        }

        return !aCas.select(PDF_PAGE_TYPE).isEmpty();
    }

    @Deprecated
    public static int transferPdfDocumentStructure(CAS aTarget, CAS aSource)
    {
        var sourceTS = aSource.getTypeSystem();
        var targetTS = aTarget.getTypeSystem();
        if (sourceTS.getType(PDF_PAGE_TYPE) == null || sourceTS.getType(PDF_CHUNK_TYPE) == null
                || targetTS.getType(PDF_PAGE_TYPE) == null
                || targetTS.getType(PDF_CHUNK_TYPE) == null) {
            return 0;
        }

        var copied = new AtomicInteger();
        var casCopier = new CasCopier(aSource, aTarget);
        Consumer<FeatureStructure> copyFunc = fs -> {
            var copy = casCopier.copyFs(fs);
            if (copy != null) {
                copied.incrementAndGet();
                aTarget.addFsToIndexes(copy);
            }
        };

        aSource.select(PDF_PAGE_TYPE).forEach(copyFunc);
        aSource.select(PDF_CHUNK_TYPE).forEach(copyFunc);

        return copied.get();
    }

}
