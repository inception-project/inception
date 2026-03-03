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
package de.tudarmstadt.ukp.clarin.webanno.model;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import org.junit.jupiter.api.Test;

class AnnotationDocumentTest
{
    @Test
    void testUpdateState()
    {
        var sut = new AnnotationDocument();

        assertThat(sut.getState()).isEqualTo(NEW);
        assertThat(sut.getStateUpdated()).isNotNull();

        var date = sut.getStateUpdated();
        sut.updateState(NEW);
        assertThat(sut.getState()).isEqualTo(NEW);
        assertThat(sut.getStateUpdated()).isEqualTo(date);

        await().atMost(ofSeconds(1)).until(() -> currentTimeMillis() != date.getTime());

        sut.updateState(FINISHED);
        assertThat(sut.getState()).isEqualTo(FINISHED);
        assertThat(sut.getStateUpdated()).isNotNull().isNotEqualTo(date);
    }
}
