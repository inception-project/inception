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
package de.tudarmstadt.ukp.inception.support.uima;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

public class ThreadLockingInvocationHandler
    implements InvocationHandler
{
    private final List<String> UNCHECKED_METHODS = asList("toString", "hashCode", "equals");

    private Thread owner;
    private Object target;
    private StackTraceElement[] trace;

    public ThreadLockingInvocationHandler(Object aTarget)
    {
        target = aTarget;
        owner = Thread.currentThread();
        trace = new Exception().getStackTrace();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        Thread current = Thread.currentThread();

        if (current != owner && !UNCHECKED_METHODS.contains(method.getName())) {
            throw new IllegalStateException("Object " + target + " bound to thread " + owner
                    + " but method " + method + " was called by thread " + current
                    + ". Object originally created at:\n" + getTrace());

        }

        return method.invoke(target, args);
    }

    public void transferOwnershipToCurrentThread()
    {
        owner = Thread.currentThread();
    }

    public Object getTarget()
    {
        Thread current = Thread.currentThread();
        if (current != owner) {
            throw new IllegalStateException("Object " + target + " bound to thread " + owner
                    + " but unwrapping was requested by thread " + current
                    + ". Object originally created at:\n" + getTrace());
        }

        return target;
    }

    private String getTrace()
    {
        return Stream.of(trace).map(e -> "\t * " + e.toString()).collect(joining("\n"))
                + "--- END OF CREATION TRACE ---\n";
    }
}
