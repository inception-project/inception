/*******************************************************************************
 * Copyright 2015
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.diag;

import java.util.Set;
import java.util.TreeSet;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelCAS;

public class CasDoctorUtils
{
    public static Set<FeatureStructure> collectIndexed(CAS aCas)
    {
        LowLevelCAS llcas = aCas.getLowLevelCAS();
        Set<FeatureStructure> fses = new TreeSet<>((fs1, fs2) -> llcas.ll_getFSRef(fs1)
                - llcas.ll_getFSRef(fs2));

        FSIterator<FeatureStructure> i = aCas.getIndexRepository().getAllIndexedFS(
                aCas.getTypeSystem().getTopType());

        i.forEachRemaining(fs -> fses.add(fs));

        return fses;
    }

    public static Set<FeatureStructure> collectReachable(CAS aCas)
    {
        LowLevelCAS llcas = aCas.getLowLevelCAS();
        Set<FeatureStructure> fses = new TreeSet<>((fs1, fs2) -> llcas.ll_getFSRef(fs1)
                - llcas.ll_getFSRef(fs2));

        FSIterator<FeatureStructure> i = aCas.getIndexRepository().getAllIndexedFS(
                aCas.getTypeSystem().getTopType());

        i.forEachRemaining(fs -> collect(fses, fs));

        return fses;
    }

    public static void collect(Set<FeatureStructure> aFSes, FeatureStructure aFS)
    {
        if (aFS != null && !aFSes.contains(aFS)) {
            aFSes.add(aFS);

            for (Feature f : aFS.getType().getFeatures()) {
                if (!f.getRange().isPrimitive()) {
                    collect(aFSes, aFS.getFeatureValue(f));
                }
            }
        }
    }
    
    public static Set<FeatureStructure> getNonIndexedFSes(CAS aCas)
    {
        TypeSystem ts = aCas.getTypeSystem();

        Set<FeatureStructure> allReachableFS = collectReachable(aCas);
        Set<FeatureStructure> allIndexedFS = collectIndexed(aCas);

        // Remove all that are indexed
        allReachableFS.removeAll(allIndexedFS);

        // Remove all that are not annotations
        allReachableFS.removeIf(fs -> !ts.subsumes(aCas.getAnnotationType(), fs.getType()));

        // All that is left are non-index annotations
        return allReachableFS;
    }
}
