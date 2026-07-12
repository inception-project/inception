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
package de.tudarmstadt.ukp.inception.support.test.llm;

import static java.time.Duration.ofMinutes;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * A Testcontainers container running
 * <a href="https://github.com/sinedied/ollamazure">ollamazure</a> — a local emulator of the Azure
 * OpenAI wire protocol that forwards to an Ollama backend. This lets the Azure OpenAI client be
 * exercised end-to-end (including its
 * {@code /openai/deployments/{deployment}/chat/completions?api-version=} route and its SSE
 * assembly) against a real local Ollama, without an Azure deployment or API key.
 * <p>
 * The image is built on the fly from the {@code Dockerfile} resource next to this class, so no
 * pre-published image is required. The container forwards to an Ollama reachable from
 * <em>inside</em> the container; by default that is the host's Ollama via
 * {@code host.docker.internal}. The caller is responsible for ensuring that Ollama is running and
 * that the requested model is pulled — this container only provides the Azure-protocol front.
 * <p>
 * Typical use, guarded so it skips when Docker or Ollama is absent:
 *
 * <pre>
 * &#64;Testcontainers(disabledWithoutDocker = true)
 * class MyTest
 * {
 *     &#64;Container
 *     static OllamazureContainer ollamazure = new OllamazureContainer("nemotron-3-nano:4b");
 *
 *     &#64;BeforeAll
 *     static void requireOllama()
 *     {
 *         assumeThat(HttpTestUtils.checkURL("http://localhost:11434")).isTrue();
 *     }
 *
 *     // point the Azure client at ollamazure.getAzureBaseUrl()
 * }
 * </pre>
 */
public class OllamazureContainer
    extends GenericContainer<OllamazureContainer>
{
    private static final int OLLAMAZURE_PORT = 4041;

    private static final String DOCKERFILE = "de/tudarmstadt/ukp/inception/support/test/llm/"
            + "ollamazure/Dockerfile";

    /**
     * Ollama URL as seen from inside the container. Defaults to the host's Ollama; overridable via
     * {@link #withOllamaUrl} (e.g. to target an Ollama running in a sibling container on a shared
     * network).
     */
    private String ollamaUrl = "http://host.docker.internal:11434";

    private final String model;

    /**
     * Embeddings model ollamazure is configured with. ollamazure touches this model at startup, so
     * it must be one the backend Ollama already has pulled — otherwise startup fails on the pull.
     * Defaults to the same model the Ollama integration tests use.
     */
    private String embeddingsModel = "granite-embedding:278m-fp16";

    /**
     * @param aModel
     *            the Ollama model that ollamazure should route completions to. It is also used as
     *            the Azure deployment name (ollamazure runs with {@code --use-deployment}), so the
     *            deployment segment of the URL selects this model.
     */
    public OllamazureContainer(String aModel)
    {
        super(new ImageFromDockerfile().withFileFromClasspath("Dockerfile", DOCKERFILE));
        model = aModel;

        withExposedPorts(OLLAMAZURE_PORT);
        // Make the host reachable as host.docker.internal on Linux (already resolvable on
        // Docker Desktop for macOS/Windows).
        withExtraHost("host.docker.internal", "host-gateway");
        waitingFor(Wait.forListeningPort().withStartupTimeout(ofMinutes(2)));
    }

    /**
     * Override the Ollama backend URL as seen from inside the container (default:
     * {@code http://host.docker.internal:11434}).
     */
    public OllamazureContainer withOllamaUrl(String aOllamaUrl)
    {
        ollamaUrl = aOllamaUrl;
        return this;
    }

    /**
     * Override the embeddings model (default: {@code granite-embedding:278m-fp16}). Must be pulled
     * in the backend Ollama, as ollamazure touches it at startup.
     */
    public OllamazureContainer withEmbeddingsModel(String aEmbeddingsModel)
    {
        embeddingsModel = aEmbeddingsModel;
        return this;
    }

    @Override
    protected void configure()
    {
        // -d/--use-deployment makes ollamazure treat the deployment name in the URL as the model
        // name, so a single container can serve the requested model under its own deployment path.
        setCommand("--use-deployment", "--ollama-url", ollamaUrl, "--model", model, "--embeddings",
                embeddingsModel);
    }

    /**
     * The deployment name served by this container (equal to the model name).
     */
    public String getDeployment()
    {
        return model;
    }

    /**
     * The base URL to configure the Azure OpenAI client with, for the default deployment (the model
     * passed to the constructor). See {@link #getAzureBaseUrl(String)}.
     */
    public String getAzureBaseUrl()
    {
        return getAzureBaseUrl(model);
    }

    /**
     * The base URL to configure the Azure OpenAI client with for a specific deployment. The client
     * appends {@code chat/completions?api-version=...}, yielding ollamazure's expected Azure route
     * {@code /openai/deployments/{deployment}/chat/completions}.
     * <p>
     * Because ollamazure runs with {@code --use-deployment}, the deployment segment selects the
     * Ollama model directly, so a single container can serve <em>any</em> pulled model — not only
     * the one passed to the constructor. The caller is responsible for the requested model being
     * pulled in the backend Ollama.
     */
    public String getAzureBaseUrl(String aDeployment)
    {
        return "http://" + getHost() + ":" + getMappedPort(OLLAMAZURE_PORT) + "/openai/deployments/"
                + aDeployment + "/";
    }
}
