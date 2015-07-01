/*******************************************************************************
 * Copyright 2015
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

import java.io.Serializable;
import java.util.Iterator;

import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;

public abstract class DefaultRefreshingView<T extends Serializable>
    extends RefreshingView<T>
{
    private static final long serialVersionUID = -4301501168097494558L;

    public DefaultRefreshingView(String aId)
    {
        super(aId);
    }

    public DefaultRefreshingView(String aId, IModel<?> aModel)
    {
        super(aId, aModel);
    }

    @Override
    protected Iterator<IModel<T>> getItemModels()
    {
        return DefaultModelIteratorAdapter.of(getModelObject());
    }

    @SuppressWarnings("unchecked")
    public Iterable<T> getModelObject()
    {
        return (Iterable<T>) getDefaultModelObject();
    }
}
