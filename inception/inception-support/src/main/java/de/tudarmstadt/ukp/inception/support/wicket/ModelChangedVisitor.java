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

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.model.ChainingModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

/**
 * In contrast to {@link Component#sameInnermostModel}, this visitor can not only handle
 * {@link IWrapModel} but also {@link ChainingModel}, e.g. {@link CompoundPropertyModel} which is
 * used in forms.
 */
public class ModelChangedVisitor
    implements IVisitor<Component, Void>
{
    private IModel<?> model;

    public ModelChangedVisitor(IModel<?> aModel)
    {
        model = aModel;
    }

    @Override
    public void component(Component aComponent, IVisit<Void> aVisit)
    {
        if (sameInnermostModel(aComponent, model)) {
            aComponent.modelChanged();
        }
    }

    private boolean sameInnermostModel(Component aComponent, IModel<?> aModel)
    {
        // Get the two models
        IModel<?> thisModel = aComponent.getDefaultModel();

        // If both models are non-null they could be the same
        if (thisModel != null && aModel != null) {
            return innermostModel(thisModel) == innermostModel(aModel);
        }

        return false;
    }

    private IModel<?> innermostModel(IModel<?> aModel)
    {
        IModel<?> nested = aModel;
        while (nested != null) {
            if (nested instanceof IWrapModel) {
                final IModel<?> next = ((IWrapModel<?>) nested).getWrappedModel();
                if (nested == next) {
                    throw new WicketRuntimeException(
                            "Model for " + nested + " is self-referential");
                }
                nested = next;
            }
            else if (nested instanceof ChainingModel) {
                final IModel<?> next = ((ChainingModel<?>) nested).getChainedModel();
                if (nested == next) {
                    throw new WicketRuntimeException(
                            "Model for " + nested + " is self-referential");
                }
                nested = next;
            }
            else {
                break;
            }
        }
        return nested;
    }
}
