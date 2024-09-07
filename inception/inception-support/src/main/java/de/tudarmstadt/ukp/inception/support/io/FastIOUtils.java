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
package de.tudarmstadt.ukp.inception.support.io;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.channels.Channels.newChannel;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.walk;
import static java.util.Comparator.reverseOrder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

public final class FastIOUtils
{
    private FastIOUtils()
    {
        // No instances
    }

    public static void delete(File aFile) throws IOException
    {
        try (Stream<Path> walk = walk(aFile.toPath())) {
            // Walk returns the paths depth-first. So we reverse in order to get files first and
            // then the directories containing them
            Iterator<Path> i = walk.sorted(reverseOrder()).iterator();
            while (i.hasNext()) {
                deleteIfExists(i.next());
            }
        }
    }

    public static void copy(InputStream aIS, File aTargetFile) throws IOException
    {
        aTargetFile.getParentFile().mkdirs();

        try (var in = newChannel(aIS); var out = newChannel(new FileOutputStream(aTargetFile))) {
            final ByteBuffer buffer = allocateDirect(8192);
            while (in.read(buffer) != -1) {
                // Cast to buffer to permit code to run on Java 8.
                // See:
                // https://github.com/inception-project/inception/issues/1828#issuecomment-717047584
                ((Buffer) buffer).flip();
                out.write(buffer);
                buffer.compact();
            }
            // Cast to buffer to permit code to run on Java 8.
            // See:
            // https://github.com/inception-project/inception/issues/1828#issuecomment-717047584
            ((Buffer) buffer).flip();
            while (buffer.hasRemaining()) {
                out.write(buffer);
            }
        }
    }
}
