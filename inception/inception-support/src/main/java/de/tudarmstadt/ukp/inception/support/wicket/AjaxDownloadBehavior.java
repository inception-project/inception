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
package de.tudarmstadt.ukp.inception.support.wicket;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.FileSystemResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

/**
 * @see <a href=
 *      "https://cwiki.apache.org/confluence/display/WICKET/AJAX+update+and+file+download+in+one+blow">AJAX
 *      update and file download in one blow</a>
 * @author Sven Meier
 * @author Ernesto Reinaldo Barreiro (reiern70@gmail.com)
 * @author Jordi Deu-Pons (jordi@jordeu.net)
 */
public class AjaxDownloadBehavior
    extends AbstractAjaxBehavior
{
    private static final long serialVersionUID = -3702600595997355221L;

    private IModel<String> filename;
    private IModel<IResourceStream> data;

    private boolean addAntiCache = true;

    public AjaxDownloadBehavior()
    {
        this(null, Model.of());
    }

    public AjaxDownloadBehavior(IModel<IResourceStream> aData)
    {
        this(null, aData);
    }

    public AjaxDownloadBehavior(IModel<String> aFilename, IModel<IResourceStream> aData)
    {
        filename = aFilename;
        data = aData;
    }

    /**
     * Call this method to initiate the download.
     * 
     * @param aTarget
     *            the AJAX target.
     */
    public void initiate(AjaxRequestTarget aTarget)
    {
        String url = getCallbackUrl().toString();

        if (addAntiCache) {
            url = url + (url.contains("?") ? "&" : "?");
            url = url + "antiCache=" + System.currentTimeMillis();
        }

        // the timeout is needed to let Wicket release the channel
        aTarget.appendJavaScript("setTimeout(\"window.location.href='" + url + "'\", 100);");
    }

    public void initiate(AjaxRequestTarget aTarget, String aFilename, IResourceStream aStream)
    {
        filename = Model.of(aFilename);
        data = Model.of(aStream);
        initiate(aTarget);
    }

    @Override
    public void onRequest()
    {
        IResourceStream is = data.getObject();

        if (is == null) {
            return;
        }

        // If no filename has been set explicitly, try to get it from the resource
        String name = filename != null ? filename.getObject() : null;
        if (name == null) {
            if (is instanceof FileResourceStream) {
                name = ((FileResourceStream) is).getFile().getName();
            }
            else if (is instanceof FileSystemResourceStream) {
                name = ((FileSystemResourceStream) is).getPath().getFileName().toString();
            }
            else if (is instanceof InputStreamResourceStream) {
                name = ((InputStreamResourceStream) is).getName();
            }
        }

        ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(is, name);
        handler.setContentDisposition(ContentDisposition.ATTACHMENT);
        getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
    }
}
