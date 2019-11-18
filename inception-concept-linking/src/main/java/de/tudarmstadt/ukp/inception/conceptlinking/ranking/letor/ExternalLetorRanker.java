/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.conceptlinking.ranking.letor;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentenceCovering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.conceptlinking.ranking.Ranker;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class ExternalLetorRanker
    implements Ranker
{

    private static final String URL = "http://blinky:5000/rank";
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public List<KBHandle> rank(String aQuery, String aMention, Set<KBHandle> aCandidates,
                               CAS aCas, int aBeginOffset)
    {
        List<KBHandle> unsortedCandidates = new ArrayList<>(aCandidates);
        if (unsortedCandidates.size() == 0) {
            return unsortedCandidates;
        }

        String context = getContext(aCas, aBeginOffset);

        PredictionRequest request = new PredictionRequest(aMention, context, unsortedCandidates);
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();

        okhttp3.RequestBody body = RequestBody.create(JSON, request.toJson());
        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .url(URL)
                .post(body)
                .build();

        try (okhttp3.Response response = client.newCall(httpRequest).execute();
             ResponseBody responseBody = response.body()) {
            if (response.isSuccessful()) {
                ObjectMapper mapper = new ObjectMapper();
                Integer[] ranks = mapper.readValue(responseBody.string(), Integer[].class);
                return argsort(unsortedCandidates, ranks);
            } else {
                log.error("Reranking request was not successful: [{} - {}]", response.code(), responseBody.string());
            }
        } catch (IOException e) {
            log.error("Exception while re-ranking externally", e);
        }

        return new ArrayList<>(aCandidates);
    }

    private String getContext(CAS aCas, int aBeginOffset) {
        if (aCas == null) {
            return "";
        }

        AnnotationFS sentence = selectSentenceCovering(aCas, aBeginOffset);

        if (sentence == null) {
            return "";
        }

        return sentence.getCoveredText();
    }

    private List<KBHandle> argsort(List<KBHandle> aCandidates, Integer[] aRanks) {
        KBHandle[] result = new KBHandle[aCandidates.size()];
        for (int i = 0; i < aCandidates.size(); i++) {
            int rank = aRanks[i];
            result[rank] = aCandidates.get(i);
        }

        return Arrays.asList(result);
    }
}
