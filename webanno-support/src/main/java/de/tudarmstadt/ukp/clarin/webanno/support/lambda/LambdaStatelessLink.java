/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.support.lambda;

import org.apache.wicket.markup.html.link.StatelessLink;
import org.danekja.java.misc.serializable.SerializableRunnable;

public class LambdaStatelessLink
    extends StatelessLink<Void>
{
    private static final long serialVersionUID = 3946442967075930557L;

    private SerializableRunnable action;
    private SerializableMethodDelegate<LambdaStatelessLink> onConfigureAction;

    public LambdaStatelessLink(String aId, SerializableRunnable aAction)
    {
        super(aId);
        action = aAction;
    }

    public LambdaStatelessLink onConfigure(SerializableMethodDelegate<LambdaStatelessLink> aAction)
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
    
    @Override
    public void onClick()
    {
        action.run();
    }
}
