/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.recommendation.imls.dl4j.pos;

import static org.nd4j.linalg.indexing.NDArrayIndex.all;
import static org.nd4j.linalg.indexing.NDArrayIndex.point;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dkpro.core.api.embeddings.binary.BinaryVectorizer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;

import de.tudarmstadt.ukp.inception.recommendation.api.model.AnnotationObject;

public class Vectorizer
{
    private Map<String, Integer> tagset = new HashMap<String, Integer>();

    // Special use embeddings
    private INDArray randUnk;

    public Vectorizer()
    {

    }

    public Vectorizer(List<String> tagset)
    {
        for (int i = 0; i < tagset.size(); i++) {
            this.tagset.put(tagset.get(i), i);
        }
    }

    public DataSet vectorize(List<List<AnnotationObject>> sentences, BinaryVectorizer wordVectors,
            int truncateLength, int maxTagsetSize, boolean includeLabels)
        throws IOException
    {
        int embeddingSize = wordVectors.dimensions(); 

        randUnk = Nd4j.rand(1, embeddingSize, Nd4j.getRandom()).subi(0.5).divi(embeddingSize);

        int maxSentLength = sentences.stream().mapToInt(tokens -> tokens.size()).max().getAsInt();
        // If longest sentence exceeds 'truncateLength': only take the first 'truncateLength' words
        if (maxSentLength > truncateLength) {
            maxSentLength = truncateLength;
        }
        // Create data for training
        // Here: we have sentences.size() examples of varying lengths
        INDArray features = Nd4j.create(sentences.size(), embeddingSize, maxSentLength);
        // Tags are using a 1-hot encoding
        INDArray labels = Nd4j.create(sentences.size(), maxTagsetSize, maxSentLength);
        // Sentences have variable length, so we we need to mask positions not used in short
        // sentences.
        INDArray featuresMask = Nd4j.zeros(sentences.size(), maxSentLength);
        INDArray labelsMask = Nd4j.zeros(sentences.size(), maxSentLength);
        for (int s = 0; s < sentences.size(); s++) {
            List<AnnotationObject> tokens = sentences.get(s);
            // Get word vectors for each word in review, and put them in the training data
            for (int t = 0; t < Math.min(tokens.size(), maxSentLength); t++) {
                AnnotationObject ao = tokens.get(t);
                String word = ao.getCoveredText();
                INDArray vector = Nd4j.create(wordVectors.vectorize(word));

                if (vector == null) {
                    vector = randUnk;
                }

                features.put(new INDArrayIndex[] { point(s), all(), point(t) }, vector);
                // Word is present (not padding) for this example + time step -> 1.0 in features
                // mask
                featuresMask.putScalar(new int[] { s, t }, 1.0);

                if (includeLabels) {
                    String pos = ao.getLabel();
                    if (!tagset.containsKey(pos)) {
                        tagset.put(pos, tagset.size());
                    }
                    labels.putScalar(s, tagset.get(pos), t, 1.0);
                }
                labelsMask.putScalar(new int[] { s, t }, 1.0);
            }
        }

        return new DataSet(features, labels, featuresMask, labelsMask);
    }

    public Collection<String> getTagset()
    {
        return tagset.keySet();
    }

}
