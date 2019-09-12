/*
 * Copyright 2017
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
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import java.util.MissingResourceException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.AjaxCallback;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.SerializableMethodDelegate;

@Deprecated
public class ActionBarLink
    extends Panel
{
    private static final long serialVersionUID = -3934314689591280774L;

    private LambdaAjaxLink link;
    private ResourceReference imageRes;
    private AjaxCallback action;
    private SerializableMethodDelegate<ActionBarLink> onConfigureAction;
    
    public ActionBarLink(String aId, AjaxCallback aAction)
    {
        this(aId, null, null, aAction);
    }

    public ActionBarLink(String aId, String aImageRes, AjaxCallback aAction)
    {
        this(aId, new UrlResourceReference(Url.parse(aImageRes)).setContextRelative(true), aAction);
    }

    public ActionBarLink(String aId, ResourceReference aImageRes, AjaxCallback aAction)
    {
        this(aId, null, aImageRes, aAction);
    }

    public ActionBarLink(String aId, IModel<String> aLabel, ResourceReference aImageRes,
            AjaxCallback aAction)
    {
        super(aId);
        
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        
        imageRes = aImageRes;
        action = aAction;
        
        IModel<String> label = aLabel;
        if (label == null) {
            label = new StringResourceModel("label", this);
        }
        
        link = new LambdaAjaxLink("link", action);
        link.add(new Label("label", label));
        add(link);
    }
    
    @Override
    protected void onBeforeRender()
    {
        // Defer adding the image until rendering because we need to access the properties from
        // the embedding component/page
        if (!hasBeenRendered()) {
            ResourceReference _imageRes = imageRes;
            if (_imageRes == null) {
                _imageRes = new UrlResourceReference(Url.parse(getString("icon")))
                        .setContextRelative(true);
            }
            
            Image image = new Image("image", _imageRes);
            try {
                // The getString method throws an MissingResourceException if the property is
                // not available. In this case, we simply do not set a tooltip
                getString("tooltip");
                image.add(new AttributeModifier("title",
                        new StringResourceModel("tooltip", ActionBarLink.this)));
            }
            catch (MissingResourceException e) {
                // Nothing to do
            }
            link.addOrReplace(image);
        }
        
        super.onBeforeRender();
    }
    
    /**
     * Adds behaviors to the embedded link, e.g. to add hot keys.
     */
    @Override
    public Component add(Behavior... aBehaviors)
    {
        link.add(aBehaviors);
        return this;
    }
    
    public ActionBarLink onConfigure(SerializableMethodDelegate<ActionBarLink> aAction)
    {
        onConfigureAction = aAction;
        return this;
    }
    
    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        if (onConfigureAction != null) {
            onConfigureAction.run(this);
        }
    }
}
