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
package de.tudarmstadt.ukp.inception.assistant.userguide;

import static de.tudarmstadt.ukp.inception.scheduling.ProgressScope.SCOPE_UNITS;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.collections4.ListUtils.partition;

import java.lang.invoke.MethodHandles;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.scheduling.Task;

public class UserGuideIndexingTask
    extends Task
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "UserGuideIndexingTask";

    private final URL htmlUrl;

    private @Autowired UserGuideQueryServiceImpl documentationIndexingService;

    public UserGuideIndexingTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE)
                .withProject(Project.builder().withName("GLOBAL").withId(-1l).build()));
        htmlUrl = aBuilder.htmlUrl;
    }

    @Override
    public String getTitle()
    {
        return "Building user guide index...";
    }

    @Override
    public void execute() throws Exception
    {
        try (var is = htmlUrl.openStream()) {
            var userGuide = Jsoup.parse(is, UTF_8.name(), "");

            try (var iw = documentationIndexingService.getIndexWriter()) {
                LOG.info(
                        "Started building user guide index in the background... this may take a moment");
                var startTime = currentTimeMillis();
                var blocks = userGuide.select(".i7n-assistant").stream() //
                        .map(Element::text) //
                        .toList();
                var blockChunks = partition(blocks, 100);

                try (var progress = getMonitor().openScope(SCOPE_UNITS, blocks.size())) {
                    for (var blockChunk : blockChunks) {
                        progress.update(up -> up.increment(blockChunk.size()));

                        documentationIndexingService.indexBlocks(iw,
                                blockChunk.toArray(String[]::new));
                    }

                    var endTime = currentTimeMillis();
                    LOG.info("User guide index complete in {}ms ({} blocks)", endTime - startTime,
                            progress.getProgress());
                }
            }
        }

        documentationIndexingService.markIndexUpToDate();
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends Task.Builder<T>
    {
        protected URL htmlUrl;

        protected Builder()
        {
        }

        @SuppressWarnings("unchecked")
        public T withHtml(URL aHtmlUrl)
        {
            this.htmlUrl = aHtmlUrl;
            return (T) this;
        }

        public UserGuideIndexingTask build()
        {
            return new UserGuideIndexingTask(this);
        }
    }
}
