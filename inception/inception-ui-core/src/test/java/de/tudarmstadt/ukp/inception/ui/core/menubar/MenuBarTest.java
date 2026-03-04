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
package de.tudarmstadt.ukp.inception.ui.core.menubar;

import static java.lang.System.setProperty;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.writeString;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.resource.FileSystemResourceReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import de.tudarmstadt.ukp.inception.support.help.ImageLinkDecl;

@Execution(ExecutionMode.SAME_THREAD)
public class MenuBarTest
{
    private String previousInceptionHome;

    @BeforeEach
    public void saveProperties()
    {
        previousInceptionHome = System.getProperty("inception.home");
    }

    @AfterEach
    public void restoreProperties()
    {
        if (previousInceptionHome == null) {
            System.clearProperty("inception.home");
        }
        else {
            System.setProperty("inception.home", previousInceptionHome);
        }
    }

    @Test
    public void acceptsPublicPath(@TempDir Path aTempDir) throws Exception
    {
        // Arrange: create public/logo.png under application home
        var publicDir = aTempDir.resolve("public");
        createDirectories(publicDir);
        var img = publicDir.resolve("logo.png");
        writeString(img, "pngdata");

        setProperty("inception.home", aTempDir.toString());

        var decl = new ImageLinkDecl("test");
        decl.setImageUrl("file:logo.png");

        // Act
        var res = MenuBar.getLinkIcon(decl);

        // Assert
        assertThat(res).as("Expected resource reference for valid public path").isPresent().get()
                .isInstanceOf(FileSystemResourceReference.class);
    }

    @Test
    public void rejectsPathTraversal(@TempDir Path aTempDir) throws Exception
    {
        // Arrange: create a file outside public
        var secret = aTempDir.resolve("secret.txt");
        writeString(secret, "secret");

        var publicDir = aTempDir.resolve("public");
        createDirectories(publicDir);

        setProperty("inception.home", aTempDir.toString());

        var decl = new ImageLinkDecl("bad");
        decl.setImageUrl("file:../secret.txt");

        // Act
        var res = MenuBar.getLinkIcon(decl);

        // Assert
        assertThat(res).as("Expected traversal attempt to be rejected").isEmpty();
    }

    @Test
    public void acceptsAbsoluteUrl()
    {
        var decl = new ImageLinkDecl("u");
        decl.setImageUrl("https://example.com/icon.png");

        var res = MenuBar.getLinkIcon(decl);

        assertThat(res).isPresent().get().isInstanceOf(UrlResourceReference.class);
    }
}
