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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.util;

import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.markup.repeater.IItemFactory;
import org.apache.wicket.markup.repeater.IItemReuseStrategy;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Generics;

/**
 * Reuses items not only across a single request but for as long as the RefreshingView exists. This
 * allows the view to contain Ajax behaviors.
 */
public class CachingReuseStrategy
    implements IItemReuseStrategy
{
    private static final long serialVersionUID = -6888699389854940706L;

    private final Map<IModel<?>, Item<?>> modelToItem = Generics.newHashMap();

    @Override
    public <T> Iterator<Item<T>> getItems(final IItemFactory<T> factory,
            final Iterator<IModel<T>> newModels, Iterator<Item<T>> existingItems)
    {
        while (existingItems.hasNext()) {
            final Item<T> item = existingItems.next();
            modelToItem.put(item.getModel(), item);
        }

        return new Iterator<Item<T>>()
        {
            private int index = 0;

            @Override
            public boolean hasNext()
            {
                return newModels.hasNext();
            }

            @Override
            public Item<T> next()
            {
                final IModel<T> model = newModels.next();

                final @SuppressWarnings("unchecked") Item<T> oldItem = (Item<T>) modelToItem
                        .get(model);

                final Item<T> item;
                if (oldItem == null) {
                    item = factory.newItem(index, model);
                }
                else {
                    oldItem.setIndex(index);
                    item = oldItem;
                }
                index++;

                return item;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
