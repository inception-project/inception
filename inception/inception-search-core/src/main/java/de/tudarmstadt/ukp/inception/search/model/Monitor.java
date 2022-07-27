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
package de.tudarmstadt.ukp.inception.search.model;

import static java.lang.Math.round;
import static java.lang.System.currentTimeMillis;

public class Monitor
{
    private final long start;

    private long lastIncrement;
    private long lastDuration;
    private int done;
    private int todo;

    public Monitor()
    {
        start = currentTimeMillis();
        lastIncrement = start;
        lastDuration = 0;
    }

    public synchronized void setDone(int aDone)
    {
        done = aDone;
    }

    public synchronized void setTodo(int aTodo)
    {
        todo = aTodo;
    }

    public synchronized void set(int aDone, int aTodo)
    {
        done = aDone;
        todo = aTodo;
    }

    public synchronized Progress toProgress()
    {
        return new Progress(done, todo);
    }

    public synchronized void incDone()
    {
        done++;
        var now = System.currentTimeMillis();
        lastDuration = now - lastIncrement;
        lastIncrement = now;
    }

    public long getTotalDuration()
    {
        return System.currentTimeMillis() - start;
    }

    public long getLastDuration()
    {
        return lastDuration;
    }

    public long getEstimatedDuration()
    {
        return round(((double) (getTotalDuration()) / done) * todo);
    }

    public boolean isDone()
    {
        return done >= todo;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(done);
        sb.append(" of ");
        sb.append(todo);
        if (done > 0 && todo > 0) {
            final int perc = 100 - (int) (((todo - done) * 100) / todo);
            sb.append(" (");
            sb.append(perc);
            sb.append("%  ETA ");
            final long timeSoFar = getTotalDuration();
            final long estTotal = (timeSoFar / done) * todo;
            final long timeLeft = estTotal - timeSoFar;
            sb.append(milliToStringShort(timeLeft));
            sb.append("  RUN ");
            sb.append(milliToStringShort(timeSoFar));
            sb.append("  AVG ");
            sb.append(timeSoFar / done);
            sb.append("  LAST ");
            sb.append(lastDuration);
            sb.append(")");
        }
        return sb.toString();
    }

    private static String milliToStringShort(final long milli)
    {
        final long fracs = milli % 1000;
        final long seconds = milli / 1000;
        final long minutes = seconds / 60;
        final long hours = minutes / 60;
        return String.format("%02d:%02d:%02d.%-3d", hours, (minutes % 60), (seconds % 60), fracs);
    }

}
