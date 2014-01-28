/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 ******************************************************************************/

package de.tudarmstadt.ukp.clarin.webanno.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;

/**
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

    public void onRequest()
    {
        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(
                getResourceStream(), getFileName());
        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }

    /**
     * Override this method for a file name which will let the browser prompt with a save/open
     * dialog.
     * 
     * @see ResourceStreamRequestTarget#getFileName()
     */
    protected String getFileName()
    {
        return new File(fileName).getName();
    }

    /**
     * Hook method providing the actual resource stream.
     */
    protected IResourceStream getResourceStream()
    {
        
        IResourceStream resStream = new AbstractResourceStream() {
            private static final long serialVersionUID = 1L;
            InputStream inStream;
            public InputStream getInputStream() throws ResourceStreamNotFoundException{
                try {
                    inStream =  new FileInputStream(fileName);
                } catch (IOException e) {                               
                }
                return inStream;
            }
            public void close() throws IOException {
                inStream.close();
                inStream = null;
                FileUtils.forceDelete(new File(fileName));
            }
        };
        return resStream; 

    }
}