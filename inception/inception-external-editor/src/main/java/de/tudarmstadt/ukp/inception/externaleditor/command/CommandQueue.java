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
package de.tudarmstadt.ukp.inception.externaleditor.command;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;

public class CommandQueue
    implements Serializable, Iterable<EditorCommand>
{
    private static final long serialVersionUID = -1148267068474211314L;

    private final List<EditorCommand> queue = new ArrayList<>();

    public void add(EditorCommand aCommand)
    {
        queue.removeIf(item -> Objects.equals(item.getClass(), aCommand.getClass()));
        queue.add(aCommand);
        queue.sort(AnnotationAwareOrderComparator.INSTANCE);
    }

    @Override
    public Iterator<EditorCommand> iterator()
    {
        return queue.iterator();
    }

    public void removeIf(Predicate<? super EditorCommand> aFilter)
    {
        queue.removeIf(aFilter);
    }
}
