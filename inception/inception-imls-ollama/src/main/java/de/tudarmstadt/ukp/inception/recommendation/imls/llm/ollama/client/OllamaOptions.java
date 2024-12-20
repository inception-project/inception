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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.ollama.client;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;

import de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.traits.Option;

public final class OllamaOptions
{
    // See https://github.com/ggerganov/llama.cpp/blob/master/examples/main/README.md
    // See https://github.com/jmorganca/ollama/blob/main/docs/api.md
    public static final Option<Integer> NUM_KEEP = new Option<>(Integer.class, "num_keep");
    public static final Option<Integer> SEED = new Option<>(Integer.class, "seed");
    public static final Option<Integer> NUM_PREDICT = new Option<>(Integer.class, "num_predict");
    public static final Option<Integer> TOP_K = new Option<>(Integer.class, "top_k");
    public static final Option<Double> TOP_P = new Option<>(Double.class, "top_p");
    public static final Option<Double> TFS_Z = new Option<>(Double.class, "tfs_z");
    public static final Option<Double> TYPICAL_P = new Option<>(Double.class, "typical_p");
    public static final Option<Integer> REPEAT_LAST_N = new Option<>(Integer.class,
            "repeat_last_n");
    public static final Option<Double> TEMPERATURE = new Option<>(Double.class, "temperature");
    public static final Option<Double> REPEAT_PENALTY = new Option<>(Double.class,
            "repeat_penalty");
    public static final Option<Double> PRESENCE_PENALTY = new Option<>(Double.class,
            "presence_penalty");
    public static final Option<Double> FREQUENCY_PENALTY = new Option<>(Double.class,
            "frequency_penalty");
    public static final Option<Integer> MIROSTAT = new Option<>(Integer.class, "mirostat");
    public static final Option<Double> MIROSTAT_TAU = new Option<>(Double.class, "mirostat_tau");
    public static final Option<Double> MIROSTAT_ETA = new Option<>(Double.class, "mirostat_eta");
    public static final Option<Boolean> PENALIZE_NEWLINE = new Option<>(Boolean.class,
            "penalize_newline");
    public static final Option<String[]> STOP = new Option<>(String[].class, "stop");
    public static final Option<Boolean> NUMA = new Option<>(Boolean.class, "numa");
    public static final Option<Integer> NUM_CTX = new Option<>(Integer.class, "num_ctx");
    public static final Option<Integer> NUM_BATCH = new Option<>(Integer.class, "num_batch");
    public static final Option<Integer> NUM_GQA = new Option<>(Integer.class, "num_gqa");
    public static final Option<Integer> NUM_GPU = new Option<>(Integer.class, "num_gpu");
    public static final Option<Integer> MAIN_GPU = new Option<>(Integer.class, "main_gpu");
    public static final Option<Boolean> LOW_VRAM = new Option<>(Boolean.class, "low_vram");
    public static final Option<Boolean> F16_KV = new Option<>(Boolean.class, "f16_kv");
    public static final Option<Boolean> LOGITS_ALL = new Option<>(Boolean.class, "logits_all");
    public static final Option<Boolean> VOCAB_ONLY = new Option<>(Boolean.class, "vocab_only");
    public static final Option<Boolean> USE_MMAP = new Option<>(Boolean.class, "use_mmap");
    public static final Option<Boolean> USE_MLOCK = new Option<>(Boolean.class, "use_mlock");
    public static final Option<Boolean> EMBEDDING_ONLY = new Option<>(Boolean.class,
            "embedding_only");
    public static final Option<Double> ROPE_FREQUENCY_BASE = new Option<>(Double.class,
            "rope_frequency_base");
    public static final Option<Double> ROPE_FREQUENCY_SCALE = new Option<>(Double.class,
            "rope_frequency_scale");
    public static final Option<Integer> NUM_THREAD = new Option<>(Integer.class, "num_thread");

    public static List<Option<?>> getAllOptions()
    {
        return asList(NUM_KEEP, SEED, NUM_PREDICT, TOP_K, TOP_P, TFS_Z, TYPICAL_P, REPEAT_LAST_N,
                TEMPERATURE, REPEAT_PENALTY, PRESENCE_PENALTY, FREQUENCY_PENALTY, MIROSTAT,
                MIROSTAT_TAU, MIROSTAT_ETA, PENALIZE_NEWLINE, STOP, NUMA, NUM_CTX, NUM_BATCH,
                NUM_GQA, NUM_GPU, MAIN_GPU, LOW_VRAM, F16_KV, LOGITS_ALL, VOCAB_ONLY, USE_MMAP,
                USE_MLOCK, EMBEDDING_ONLY, ROPE_FREQUENCY_BASE, ROPE_FREQUENCY_SCALE, NUM_THREAD);
    }

    public static Optional<Option<?>> getOption(String aOption)
    {
        return getAllOptions().stream().filter(o -> o.getName().equals(aOption)).findFirst();
    }

    private OllamaOptions()
    {
        // No instances
    }
}
