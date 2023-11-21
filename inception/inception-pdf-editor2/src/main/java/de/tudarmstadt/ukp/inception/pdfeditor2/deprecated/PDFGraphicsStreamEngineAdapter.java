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
package de.tudarmstadt.ukp.inception.pdfeditor2.deprecated;

import java.awt.geom.Point2D;
import java.io.IOException;

import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

/**
 * Base class that removes the need for subclasses to implement all the abstract methods of
 * {@link PDFGraphicsStreamEngine} that they might not even care about.
 */
@Deprecated
public abstract class PDFGraphicsStreamEngineAdapter
    extends PDFGraphicsStreamEngine
{
    protected PDFGraphicsStreamEngineAdapter(PDPage aPage)
    {
        super(aPage);
    }

    @Override
    public void appendRectangle(Point2D aP0, Point2D aP1, Point2D aP2, Point2D aP3)
        throws IOException
    {
        // No action
    }

    @Override
    public void drawImage(PDImage aPdImage) throws IOException
    {
        // No action
    }

    @Override
    public void clip(int aWindingRule) throws IOException
    {
        // No action
    }

    @Override
    public void moveTo(float aX, float aY) throws IOException
    {
        // No action
    }

    @Override
    public void lineTo(float aX, float aY) throws IOException
    {
        // No action
    }

    @Override
    public void curveTo(float aX1, float aY1, float aX2, float aY2, float aX3, float aY3)
        throws IOException
    {
        // No action
    }

    @Override
    public void closePath() throws IOException
    {
        // No action
    }

    @Override
    public void endPath() throws IOException
    {
        // No action
    }

    @Override
    public void strokePath() throws IOException
    {
        // No action
    }

    @Override
    public void fillPath(int aWindingRule) throws IOException
    {
        // No action
    }

    @Override
    public void fillAndStrokePath(int aWindingRule) throws IOException
    {
        // No action
    }

    @Override
    public void shadingFill(COSName aShadingName) throws IOException
    {
        // No action
    }
}
