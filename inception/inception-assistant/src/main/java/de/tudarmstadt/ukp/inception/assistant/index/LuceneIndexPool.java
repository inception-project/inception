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
package de.tudarmstadt.ukp.inception.assistant.index;

import static de.tudarmstadt.ukp.inception.project.api.ProjectService.PROJECT_FOLDER;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.embedding.EmbeddingService;

public class LuceneIndexPool
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EmbeddingService embeddingService;

    private final String name;
    private final Duration evictionDelay;
    private final Duration minIdleTime;
    private final Duration borrowWaitTimeout;
    private final Path repositoryPath;

    private final GenericKeyedObjectPool<Long, PooledIndex> indexPool;

    private LuceneIndexPool(Builder builder)
    {
        embeddingService = builder.embeddingService;
        name = builder.name;
        evictionDelay = builder.evictionDelay;
        minIdleTime = builder.minIdleTime;
        borrowWaitTimeout = builder.borrowWaitTimeout;
        repositoryPath = builder.path;

        var indexPoolConfig = new GenericKeyedObjectPoolConfig<PooledIndex>();
        // We only ever want one pooled index per project
        indexPoolConfig.setMaxTotalPerKey(1);
        indexPoolConfig.setMaxIdlePerKey(1);
        // We do not want the pooled index to hang around forever. It can be closed when unused.
        indexPoolConfig.setMinIdlePerKey(0);
        // Run an evictor thread periodically
        indexPoolConfig.setTimeBetweenEvictionRuns(evictionDelay);
        // Allow the evictor to drop idle CASes from pool after a short time. This should avoid that
        // CASes that are used regularly are dropped from the pool too quickly.
        indexPoolConfig.setMinEvictableIdleDuration(minIdleTime);
        // Allow the evictor to drop all idle CASes on every eviction run
        indexPoolConfig.setNumTestsPerEvictionRun(-1);
        // Allow viewing the pool in JMX
        indexPoolConfig.setJmxEnabled(true);
        indexPoolConfig.setJmxNameBase(getClass().getPackage().getName() + ":type="
                + getClass().getSimpleName() + ",name=");
        indexPoolConfig.setJmxNamePrefix(name);
        // Max. time we wait for a CAS to become available before giving up with an error
        indexPoolConfig.setMaxWait(borrowWaitTimeout);
        indexPool = new GenericKeyedObjectPool<>(new PooledIndexWriterFactory(), indexPoolConfig);
    }

    public PooledIndex borrowIndex(Project aProject) throws Exception
    {
        return indexPool.borrowObject(aProject.getId());
    }

    private Path getIndexDirectory(Long aProjectId)
    {
        return repositoryPath //
                .toAbsolutePath() //
                .resolve(PROJECT_FOLDER) //
                .resolve(Long.toString(aProjectId)) //
                .resolve(name);
    }

    public void clearIndex(Project aProject) throws Exception
    {
        try (var index = borrowIndex(aProject)) {
            index.getIndexWriter().deleteAll();
            index.getIndexWriter().commit();
        }
    }

    public class PooledIndex
        implements AutoCloseable
    {
        final long projectId;
        final Directory directory;
        final IndexWriter indexWriter;

        PooledIndex(long aProjectId, Directory aDirectory, IndexWriter aIndexWriter)
        {
            projectId = aProjectId;
            directory = aDirectory;
            indexWriter = aIndexWriter;
        }

        long getProjectId()
        {
            return projectId;
        }

        Directory getDirectory()
        {
            return directory;
        }

        public IndexWriter getIndexWriter()
        {
            return indexWriter;
        }

        @Override
        public void close() throws Exception
        {
            indexPool.returnObject(projectId, this);
        }

    }

    class PooledIndexWriterFactory
        extends BaseKeyedPooledObjectFactory<Long, PooledIndex>
    {
        @Override
        public PooledIndex create(Long aKey) throws Exception
        {
            var dir = new MMapDirectory(getIndexDirectory(aKey));
            var iwc = new IndexWriterConfig();
            iwc.setCodec(new HighDimensionLucene912Codec(embeddingService.getDimension()));
            return new PooledIndex(aKey, dir, new IndexWriter(dir, iwc));
        }

        @SuppressWarnings("resource")
        @Override
        public void destroyObject(Long aProjectId, PooledObject<PooledIndex> aPooledIndex)
            throws Exception
        {
            var index = aPooledIndex.getObject();
            try {
                index.getIndexWriter().close();
            }
            catch (IOException e) {
                LOG.error("Error closing assistant index writer for project [{}]", aProjectId);
            }

            try {
                index.getDirectory().close();
            }
            catch (IOException e) {
                LOG.error("Error closing assistant index directory for project [{}]", aProjectId);
            }
        }

        @Override
        public PooledObject<PooledIndex> wrap(PooledIndex aWriter)
        {
            return new DefaultPooledObject<>(aWriter);
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private EmbeddingService embeddingService;
        private String name;
        private Duration evictionDelay;
        private Duration minIdleTime;
        private Duration borrowWaitTimeout;
        private Path path;

        private Builder()
        {
        }

        public Builder withEmbeddingService(EmbeddingService aEmbeddingService)
        {
            embeddingService = aEmbeddingService;
            return this;
        }

        public Builder withName(String aName)
        {
            name = aName;
            return this;
        }

        public Builder withEvictionDelay(Duration aEvictionDelay)
        {
            evictionDelay = aEvictionDelay;
            return this;
        }

        public Builder withMinIdleTime(Duration aMinIdleTime)
        {
            minIdleTime = aMinIdleTime;
            return this;
        }

        public Builder withBorrowWaitTimeout(Duration aBorrowWaitTimeout)
        {
            borrowWaitTimeout = aBorrowWaitTimeout;
            return this;
        }

        public Builder withRepositoryPath(Path aPath)
        {
            path = aPath;
            return this;
        }

        public LuceneIndexPool build()
        {
            return new LuceneIndexPool(this);
        }
    }
}
