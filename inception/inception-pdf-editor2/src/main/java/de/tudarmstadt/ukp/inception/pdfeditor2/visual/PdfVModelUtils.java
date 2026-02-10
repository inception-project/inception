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
package de.tudarmstadt.ukp.inception.pdfeditor2.visual;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.util.CasCopier;
import org.dkpro.core.api.pdf.type.PdfChunk;
import org.dkpro.core.api.pdf.type.PdfPage;

public class PdfVModelUtils
{
    private PdfVModelUtils()
    {
        // No instances
    }

    public static boolean containsPdfDocumentStructure(CAS aCas)
    {
        var ts = aCas.getTypeSystem();
        if (ts.getType(PdfPage._TypeName) == null) {
            return false;
        }

        return !aCas.select(PdfPage.class).isEmpty();
    }

    public static int transferPdfDocumentStructure(CAS aTarget, CAS aSource)
    {
        var sourceTS = aSource.getTypeSystem();
        var targetTS = aTarget.getTypeSystem();
        if (sourceTS.getType(PdfPage._TypeName) == null
                || sourceTS.getType(PdfChunk._TypeName) == null
                || targetTS.getType(PdfPage._TypeName) == null
                || targetTS.getType(PdfChunk._TypeName) == null) {
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

        aSource.select(PdfPage.class).forEach(copyFunc);
        aSource.select(PdfChunk.class).forEach(copyFunc);

        return copied.get();
    }

}
