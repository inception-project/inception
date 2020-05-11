/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ThreadLockingInvocationHandler
    implements InvocationHandler
{
    private Thread owner;
    private Object target;

    public ThreadLockingInvocationHandler(Object aTarget)
    {
        target = aTarget;
        owner = Thread.currentThread();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        Thread current = Thread.currentThread();
        if (current != owner) {
            throw new IllegalStateException("Object " + target + " bound to thread " + owner
                    + " but method " + method + " was called by thread " + current + ".");
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
                    + " but unwrapping was requested by thread " + current + ".");
        }
        
        return target;
    }
}
