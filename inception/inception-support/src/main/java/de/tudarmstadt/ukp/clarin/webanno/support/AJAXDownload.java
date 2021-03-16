/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

/**
 * @see <a href=
 *      "https://cwiki.apache.org/confluence/display/WICKET/AJAX+update+and+file+download+in+one+blow">AJAX
 *      update and file download in one blow</a>
 * @author Sven Meier
 * @author Ernesto Reinaldo Barreiro (reiern70@gmail.com)
 * @author Jordi Deu-Pons (jordi@jordeu.net)
 */
public class AJAXDownload
    extends AbstractAjaxBehavior
{
    private static final long serialVersionUID = -3702600595997355221L;
    private boolean addAntiCache;
    private String fileName;

    public AJAXDownload()
    {
        this(true);
    }

    public AJAXDownload(boolean addAntiCache)
    {
        super();
        this.addAntiCache = addAntiCache;
    }

    /**
     * Call this method to initiate the download.
     * 
     * @param aTarget
     *            the AJAX target.
     * @param aFileName
     *            the filename.
     */
    public void initiate(AjaxRequestTarget aTarget, String aFileName)
    {
        fileName = aFileName;
        String url = getCallbackUrl().toString();

        if (addAntiCache) {
            url = url + (url.contains("?") ? "&" : "?");
            url = url + "antiCache=" + System.currentTimeMillis();
        }

        // the timeout is needed to let Wicket release the channel
        aTarget.appendJavaScript("setTimeout(\"window.location.href='" + url + "'\", 100);");
    }

    @Override
    public void onRequest()
    {
        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(getResourceStream(),
                getFileName());
        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    /**
     * Override this method for a file name which will let the browser prompt with a save/open
     * dialog.
     * 
     * @return the filename.
     */
    protected String getFileName()
    {
        return new File(fileName).getName();
    }

    /**
     * Hook method providing the actual resource stream.
     * 
     * @return the stream.
     */
    protected IResourceStream getResourceStream()
    {
        return new AbstractResourceStream()
        {
            private static final long serialVersionUID = 1L;

            private InputStream inStream;

            @Override
            public InputStream getInputStream() throws ResourceStreamNotFoundException
            {
                try {
                    inStream = new FileInputStream(fileName);
                }
                catch (IOException e) {
                    throw new ResourceStreamNotFoundException(e);
                }
                return inStream;
            }

            @Override
            public void close() throws IOException
            {
                IOUtils.closeQuietly(inStream);
                inStream = null;
                FileUtils.forceDelete(new File(fileName));
            }
        };
    }
}
