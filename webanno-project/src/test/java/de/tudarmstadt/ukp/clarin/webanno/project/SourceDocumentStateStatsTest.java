/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.project;

import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.NEW;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

public class SourceDocumentStateStatsTest
{
    @Test
    public void testGetProjectState()
    {
        assertEquals(NEW,
                new SourceDocumentStateStats(1l, 1l, 0l, 0l, 0l, 0l).getProjectState());
        assertEquals(ANNOTATION_IN_PROGRESS,
                new SourceDocumentStateStats(1l, 0l, 1l, 0l, 0l, 0l).getProjectState());
        assertEquals(ANNOTATION_IN_PROGRESS,
                new SourceDocumentStateStats(2l, 0l, 1l, 1l, 0l, 0l).getProjectState());
        assertEquals(ANNOTATION_IN_PROGRESS,
                new SourceDocumentStateStats(3l, 1l, 1l, 1l, 0l, 0l).getProjectState());
        assertEquals(ANNOTATION_FINISHED,
                new SourceDocumentStateStats(1l, 0l, 0l, 1l, 0l, 0l).getProjectState());
        assertEquals(CURATION_IN_PROGRESS,
                new SourceDocumentStateStats(1l, 0l, 0l, 0l, 1l, 0l).getProjectState());
        assertEquals(CURATION_IN_PROGRESS,
                new SourceDocumentStateStats(2l, 1l, 0l, 0l, 1l, 0l).getProjectState());
        assertEquals(CURATION_IN_PROGRESS,
                new SourceDocumentStateStats(3l, 1l, 1l, 0l, 1l, 0l).getProjectState());
        assertEquals(CURATION_IN_PROGRESS,
                new SourceDocumentStateStats(4l, 1l, 1l, 1l, 1l, 0l).getProjectState());
        assertEquals(CURATION_FINISHED,
                new SourceDocumentStateStats(1l, 0l, 0l, 0l, 0l, 1l).getProjectState());
    }
    
    @Test
    public void testGetProjectStateIsDefinedSweep()
    {
        for (long an = 0; an < 3; an++) {
            for (long aip = 0; aip < 3; aip++) {
                for (long af = 0; af < 3; af++) {
                    for (long cip = 0; cip < 3; cip++) {
                        for (long cf = 0; cf < 3; cf++) {
                            long total = an + aip + af + cip + cf;
                            SourceDocumentStateStats stats = new SourceDocumentStateStats(
                                    total, an, aip, af, cip, cf);
                            System.out.println(stats);
                            stats.getProjectState();
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testGetProjectStateIsDefinedRandom()
    {
        int limit = 100;
        
        Random rnd = new Random();

        for (int i = 0; i < 10000; i++) {
            long an = rnd.nextInt(limit);
            long aip = rnd.nextInt(limit);
            long af = rnd.nextInt(limit);
            long cip = rnd.nextInt(limit);
            long cf = rnd.nextInt(limit);
            long total = an + aip + af + cip + cf;

            SourceDocumentStateStats stats = new SourceDocumentStateStats(
                    total, an, aip, af, cip, cf);
            System.out.println(stats);
            stats.getProjectState();
        }
    }
}
