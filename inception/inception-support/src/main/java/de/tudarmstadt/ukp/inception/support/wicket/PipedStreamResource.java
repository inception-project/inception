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
package de.tudarmstadt.ukp.inception.support.wicket;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.wicket.ThreadContext;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

public class PipedStreamResource
    extends AbstractResourceStream
{
    private static final long serialVersionUID = 6216245754512122720L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private final DataSupplier supplier;
    private PipedInputStream is;
    private OutputStream os;
    private List<Filter> filters;
    private MediaType contentType;

    public PipedStreamResource(DataSupplier aSupplier, Filter... aFilters)
    {
        this(aSupplier, null, aFilters);
    }

    public PipedStreamResource(DataSupplier aSupplier, MediaType aContentType, Filter... aFilters)
    {
        supplier = aSupplier;
        filters = asList(aFilters);
        contentType = aContentType;
    }

    @Override
    public String getContentType()
    {
        if (contentType == null) {
            return null;
        }

        return contentType.toString();
    }

    @Override
    public InputStream getInputStream() throws ResourceStreamNotFoundException
    {
        is = new PipedInputStream();

        try {
            os = new PipedOutputStream(is);
            for (var filter : filters) {
                os = filter.apply(os);
            }
        }
        catch (IOException e) {
            throw new ResourceStreamNotFoundException(e);
        }

        var wicketApp = ThreadContext.getApplication();
        var requestCycle = ThreadContext.getRequestCycle();
        var session = ThreadContext.getSession();
        var supplierThread = new Thread(() -> {
            ThreadContext.setApplication(wicketApp);
            ThreadContext.setRequestCycle(requestCycle);
            ThreadContext.setSession(session);
            try {
                supplier.write(os);
            }
            catch (IOException e) {
                LOG.error("Error producing resource", e);
            }
            finally {
                ThreadContext.detach();
                if (os != null) {
                    try {
                        os.close();
                    }
                    catch (IOException e) {
                        LOG.error("Error closing stream", e);
                    }
                    os = null;
                }

            }
        });
        supplierThread.setName("PipedStreamResource-worker-" + THREAD_COUNTER.incrementAndGet());
        supplierThread.start();
        return is;
    }

    @Override
    public void close() throws IOException
    {
        if (is != null) {
            is.close();
            is = null;
        }

        if (os != null) {
            os.close();
            os = null;
        }
    }

    @FunctionalInterface
    public interface DataSupplier
        extends Serializable
    {
        void write(OutputStream aOS) throws IOException;
    }

    @FunctionalInterface
    public interface Filter
        extends Serializable
    {
        OutputStream apply(OutputStream aT) throws IOException;
    }
}
