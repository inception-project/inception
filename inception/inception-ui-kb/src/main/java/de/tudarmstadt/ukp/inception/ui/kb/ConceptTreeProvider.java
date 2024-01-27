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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class ConceptTreeProvider
    implements ITreeProvider<KBObject>
{
    private static final long serialVersionUID = 5318498575532049499L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Map<KBObject, Boolean> childrenPresentCache = new HashMap<>();
    private Map<KBObject, List<KBHandle>> childrensCache = new HashMap<>();

    private final KnowledgeBaseService kbService;
    private final IModel<KnowledgeBase> kbModel;
    private final IModel<ConceptTreeProviderOptions> options;

    public ConceptTreeProvider(KnowledgeBaseService aKbService, IModel<KnowledgeBase> aKbModel,
            IModel<ConceptTreeProviderOptions> aPreferences)
    {
        kbModel = aKbModel;
        kbService = aKbService;
        options = aPreferences;
    }

    @Override
    public void detach()
    {
        if (kbModel.isPresent().getObject() && !kbModel.getObject().isReadOnly()) {
            childrenPresentCache.clear();
        }
    }

    @Override
    public Iterator<? extends KBHandle> getRoots()
    {
        if (!kbModel.isPresent().getObject()) {
            return Collections.emptyIterator();
        }

        try {
            return kbService
                    .listRootConcepts(kbModel.getObject(), options.getObject().isShowAllConcepts())
                    .iterator();
        }
        catch (QueryEvaluationException e) {
            Session.get().error("Unable to list root concepts: " + e.getLocalizedMessage());
            LOG.error("Unable to list root concepts.", e);
            return Collections.emptyIterator();
        }
    }

    @Override
    public boolean hasChildren(KBObject aNode)
    {
        if (!kbModel.isPresent().getObject()) {
            return false;
        }

        try {
            // If the KB is read-only, then we cache the values and re-use the cached values.
            if (kbModel.getObject().isReadOnly()) {
                // To avoid having to send a query to the KB for every child node, just assume
                // that there might be child nodes and show the expander until we have actually
                // loaded the children, cached them and can show the true information.
                var children = childrensCache.get(aNode);
                if (children == null) {
                    return true;
                }
                else {
                    return !children.isEmpty();
                }
            }
            else {
                Boolean hasChildren = childrenPresentCache.get(aNode);
                if (hasChildren == null) {
                    hasChildren = kbService.hasChildConcepts(kbModel.getObject(),
                            aNode.getIdentifier(), options.getObject().isShowAllConcepts());
                    childrenPresentCache.put(aNode, hasChildren);
                }

                return hasChildren;
            }
        }
        catch (QueryEvaluationException e) {
            Session.get().error("Unable to list child concepts: " + e.getLocalizedMessage());
            LOG.error("Unable to list child concepts.", e);
            return false;
        }
    }

    @Override
    public Iterator<? extends KBObject> getChildren(KBObject aNode)
    {
        if (!kbModel.isPresent().getObject()) {
            return Collections.emptyIterator();
        }

        try {
            // If the KB is read-only, then we cache the values and re-use the cached values.
            if (kbModel.getObject().isReadOnly()) {
                var children = childrensCache.get(aNode);
                if (children == null) {
                    children = kbService.listChildConcepts(kbModel.getObject(),
                            aNode.getIdentifier(), options.getObject().isShowAllConcepts());
                    childrensCache.put(aNode, children);
                }
                return children.iterator();
            }
            else {
                return kbService.listChildConcepts(kbModel.getObject(), aNode.getIdentifier(),
                        options.getObject().isShowAllConcepts()).iterator();
            }
        }
        catch (QueryEvaluationException e) {
            Session.get().error("Unable to list child concepts: " + e.getLocalizedMessage());
            LOG.error("Unable to list child concepts.", e);
            return Collections.emptyIterator();
        }
    }

    @Override
    public IModel<KBObject> model(KBObject aObject)
    {
        return Model.of(aObject);
    }
}
