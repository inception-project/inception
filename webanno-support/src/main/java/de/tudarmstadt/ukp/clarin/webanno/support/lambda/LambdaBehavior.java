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

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.danekja.java.util.function.serializable.SerializableBooleanSupplier;

public class LambdaBehavior
{
    public static Behavior onConfigure(SerializableConsumer<Component> aAction)
    {
        return new Behavior()
        {
            private static final long serialVersionUID = 4955142510510102959L;

            @Override
            public void onConfigure(Component aComponent)
            {
                aAction.accept(aComponent);
            }
        };
    }
    
    public static Behavior visibleWhen(SerializableBooleanSupplier aPredicate)
    {
        return new Behavior()
        {
            private static final long serialVersionUID = -4689671763746799691L;

            @Override
            public void onConfigure(Component aComponent)
            {
                aComponent.setVisible(aPredicate.getAsBoolean());
            }
        };
    }
    
    public static Behavior enabledWhen(SerializableBooleanSupplier aPredicate)
    {
        return new Behavior()
        {
            private static final long serialVersionUID = -4689671763746799691L;

            @Override
            public void onConfigure(Component aComponent)
            {
                aComponent.setEnabled(aPredicate.getAsBoolean());
            }
        };
    }
}
