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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.FileSystemResourceStream;
import org.apache.wicket.util.resource.IResourceStream;

public class AjaxDownloadLink
    extends AjaxLink<IResourceStream>
{
    private static final long serialVersionUID = 6051420355052284475L;

    private IModel<String> filename;
    private AbstractAjaxBehavior downloadBehavior;
    private boolean addAntiCache = true;

    public AjaxDownloadLink(String aId, IModel<IResourceStream> aData)
    {
        super(aId, aData);
        filename = null;
        commonInit();
    }

    public AjaxDownloadLink(String aId, IModel<String> aFilename, IModel<IResourceStream> aData)
    {
        super(aId, aData);
        filename = aFilename;
        commonInit();
    }

    public boolean isAddAntiCache()
    {
        return addAntiCache;
    }

    public void setAddAntiCache(boolean aAddAntiCache)
    {
        addAntiCache = aAddAntiCache;
    }

    @Override
    public void onClick(AjaxRequestTarget aTarget)
    {
        String url = downloadBehavior.getCallbackUrl().toString();

        if (addAntiCache) {
            url = url + (url.contains("?") ? "&" : "?");
            url = url + "antiCache=" + System.currentTimeMillis();
        }

        // the timeout is needed to let Wicket release the channel
        aTarget.appendJavaScript("setTimeout(\"window.location.href='" + url + "'\", 100);");
    }

    void commonInit()
    {
        downloadBehavior = new AbstractAjaxBehavior()
        {
            private static final long serialVersionUID = 3472918725573624819L;

            @Override
            public void onRequest()
            {
                var is = AjaxDownloadLink.this.getModelObject();

                if (is != null) {
                    // If no filename has been set explicitly, try to get it from the resource
                    var name = filename != null ? filename.getObject() : null;
                    if (name == null) {
                        if (is instanceof FileResourceStream) {
                            name = ((FileResourceStream) is).getFile().getName();
                        }
                        else if (is instanceof FileSystemResourceStream) {
                            name = ((FileSystemResourceStream) is).getPath().getFileName()
                                    .toString();
                        }
                        else if (is instanceof InputStreamResourceStream) {
                            name = ((InputStreamResourceStream) is).getName();
                        }
                    }

                    var handler = new ResourceStreamRequestHandler(
                            AjaxDownloadLink.this.getModelObject(), name);
                    handler.setContentDisposition(ContentDisposition.ATTACHMENT);
                    getComponent().getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
                }
            }
        };
        add(downloadBehavior);
    }
}
