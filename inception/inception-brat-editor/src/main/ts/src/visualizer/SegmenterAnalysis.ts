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

/**
 * Segmenter/ICU analysis shape used for grapheme cluster and bidi helpers.
 */
export interface SegmenterAnalysis {
    // DOM character indices of grapheme cluster starts
    clusterStarts: number[];

    // Optional visual reordering mapping (adapter-specific)
    visualOrder?: number[];

    // Adapter-specific extras are intentionally not permitted here to preserve
    // strict typing; add explicit fields if needed in future.
}

/** Adapter interface for grapheme-cluster / bidi analysis providers. */
export interface SegmenterAdapter {
    analyzeText(text: string): SegmenterAnalysis;
}
