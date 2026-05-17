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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.client;

import tools.jackson.databind.JsonNode;

/**
 * Provider-neutral description of a tool the model may call. Carried in {@link ChatOptions#tools()}
 * on the request side. The execution-side counterpart (with the actual Java method to invoke) lives
 * in the assistant/tool-library layer.
 *
 * @param name
 *            function name as exposed to the model
 * @param description
 *            free-text description used by the model to decide when to call
 * @param parametersSchema
 *            JSON schema describing the function parameters
 */
public record ToolDescriptor( //
        String name, //
        String description, //
        JsonNode parametersSchema)
{}
