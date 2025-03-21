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
package de.tudarmstadt.ukp.inception.externaleditor.xhtml;

import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.ATTR_CLASS;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.BLOCKQUOTE;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.BR;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.CDATA;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.CODE;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.EM;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.HR;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.LI;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.OL;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.P;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.S;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.SPAN;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.STRONG;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.TABLE;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.TABLE_WRAP;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.TBODY;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.TD;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.TH;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.THEAD;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.TR;
import static de.tudarmstadt.ukp.inception.externaleditor.xhtml.XHtmlConstants.UL;
import static java.lang.String.join;
import static java.util.Arrays.asList;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ast.WhiteSpace;
import com.vladsch.flexmark.ext.footnotes.Footnote;
import com.vladsch.flexmark.ext.footnotes.FootnoteBlock;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TableSeparator;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.Visitor;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class XHtmlXmlMarkdownProcessor
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public void process(ContentHandler ch, String aString) throws SAXException
    {
        var options = new MutableDataSet() //
                .set(Parser.EXTENSIONS, asList( //
                        // FootnoteExtension.create(), //
                        TaskListExtension.create(), //
                        TablesExtension.create(), //
                        StrikethroughExtension.create())) //
                .set(TablesExtension.COLUMN_SPANS, false) //
                .toImmutable();

        var parser = Parser.builder(options).build();
        var document = parser.parse(aString);

        var visitor = new XHtmlVisitor(ch);

        visitor.visit(document);
    }

    interface NodeHandler<T extends Node>
    {
        void handleNode(ContentHandler aHandler, T aNode, Runnable aProceessChildren)
            throws Exception;
    }

    static class XHtmlVisitor
        extends NodeVisitor
    {
        private static final String IAA_MD_CHECKBOX_UNCHECKED = "iaa-md-checkbox-unchecked";
        private static final String IAA_MD_CHECKBOX_CHECKED = "iaa-md-checkbox-checked";
        private static final String IAA_MD_D_NONE = "iaa-md-d-none";
        private int processedText = 0;
        private ContentHandler ch;
        private Map<Class<?>, NodeHandler<?>> handlers = new HashMap<>();
        private boolean tableSeparator = false;
        private boolean fencedCodeBlock = false;

        public XHtmlVisitor(ContentHandler aHandler)
        {
            ch = aHandler;

            addHandler(Text.class, this::text);
            addHandler(WhiteSpace.class, this::text);
            addHandler(HardLineBreak.class, this::text);
            addHandler(Document.class, this::noop);
            addHandler(ThematicBreak.class, (h, n, cont) -> milestoneBefore(h, n, cont, HR));
            addHandler(HardLineBreak.class, (h, n, cont) -> milestoneBefore(h, n, cont, BR));
            addHandler(SoftLineBreak.class, (h, n, cont) -> milestoneBefore(h, n, cont, BR));
            addHandler(BlockQuote.class, (h, n, cont) -> wrap(h, n, cont, BLOCKQUOTE));
            addHandler(Emphasis.class, (h, n, cont) -> wrap(h, n, cont, EM));
            addHandler(StrongEmphasis.class, (h, n, cont) -> wrap(h, n, cont, STRONG));
            addHandler(Strikethrough.class, (h, n, cont) -> wrap(h, n, cont, S));
            addHandler(Code.class, (h, n, cont) -> wrap(h, n, cont, CODE));
            addHandler(TableBlock.class, this::tableBlock);
            addHandler(TableHead.class, (h, n, cont) -> wrap(h, n, cont, THEAD));
            addHandler(TableSeparator.class, this::tableSeparator);
            addHandler(TableBody.class, (h, n, cont) -> wrap(h, n, cont, TBODY));
            addHandler(TableRow.class, this::tableRow);
            addHandler(TableCell.class, this::tableCell);
            addHandler(Paragraph.class, this::paragraph);
            addHandler(BulletList.class, (h, n, cont) -> wrap(h, n, cont, UL));
            addHandler(BulletListItem.class, (h, n, cont) -> wrap(h, n, cont, LI));
            addHandler(TaskListItem.class, this::taskListItem);
            addHandler(OrderedList.class, (h, n, cont) -> wrap(h, n, cont, OL));
            addHandler(OrderedListItem.class, (h, n, cont) -> wrap(h, n, cont, LI));
            addHandler(Heading.class, this::heading);
            addHandler(FencedCodeBlock.class, this::codeBlock);
            addHandler(FootnoteBlock.class, this::noop);
            addHandler(Footnote.class, this::footnote);
        }

        <T extends Node> void addHandler(Class<T> aClass, NodeHandler<T> aHandler)
        {
            handlers.put(aClass, aHandler);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void processNode(Node aNode, boolean aWithChildren,
                BiConsumer<Node, Visitor<Node>> aProcessor)
        {
            try {
                LOG.trace("[{}-{}] <{}>", aNode.getStartOffset(), aNode.getEndOffset(),
                        aNode.getNodeName());

                @SuppressWarnings("rawtypes")
                var handler = (NodeHandler) handlers.get(aNode.getClass());
                if (handler != null) {
                    handler.handleNode(ch, (Node) aNode, () -> processChildren(aNode, aProcessor));
                }
                else if (aWithChildren) {
                    LOG.trace("No handler for node type [{}]", aNode.getNodeName());
                    processChildren(aNode, aProcessor);
                }

                LOG.trace("[{}-{}] <{}/>", aNode.getStartOffset(), aNode.getEndOffset(),
                        aNode.getNodeName());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void noop(ContentHandler aHandler, Node aNode, Runnable aProceessChildren)
        {
            aProceessChildren.run();
        }

        private void milestoneBefore(ContentHandler aHandler, Node aNode,
                Runnable aProceessChildren, String qName)
            throws SAXException
        {
            alignStart(aHandler, aNode);

            ch.startElement(null, null, qName, null);
            ch.endElement(null, null, qName);
            aProceessChildren.run();

            alignEnd(aHandler, aNode);
        }

        private void codeBlock(ContentHandler aHandler, FencedCodeBlock aCodeBlock,
                Runnable aProceessChildren)
            throws SAXException
        {
            alignStart(aHandler, aCodeBlock);

            fencedCodeBlock = true;
            var attrs = new AttributesImpl();
            attrs.addAttribute("", "", ATTR_CLASS, CDATA, "iaa-md-codeblock");
            attrs.addAttribute("", "", "data-iaa-md-info", CDATA, aCodeBlock.getInfo().toString());

            ch.startElement(null, null, CODE, attrs);
            aProceessChildren.run();
            ch.endElement(null, null, CODE);
            fencedCodeBlock = false;

            alignEnd(aHandler, aCodeBlock);
        }

        private void tableSeparator(ContentHandler aHandler, TableSeparator aSeparator,
                Runnable aProceessChildren)
            throws SAXException
        {
            tableSeparator = true;
            aProceessChildren.run();
            tableSeparator = false;
        }

        private void paragraph(ContentHandler aHandler, Paragraph aParagraph,
                Runnable aProceessChildren)
            throws SAXException
        {
            if (aParagraph.getParent() instanceof ListItem listItem) {
                if (listItem.isParagraphInTightListItem(aParagraph)) {
                    aProceessChildren.run();
                    return;
                }
            }

            ch.startElement(null, null, P, null);
            aProceessChildren.run();
            ch.endElement(null, null, P);
        }

        private void taskListItem(ContentHandler aHandler, TaskListItem aTaskListItem,
                Runnable aProceessChildren)
            throws SAXException
        {
            alignStart(aHandler, aTaskListItem);

            var classes = new HashSet<String>();

            var checkboxAttrs = new AttributesImpl();
            if (aTaskListItem.isItemDoneMarker()) {
                classes.add(IAA_MD_CHECKBOX_CHECKED);
            }
            else {
                classes.add(IAA_MD_CHECKBOX_UNCHECKED);
            }

            checkboxAttrs.addAttribute("", "", ATTR_CLASS, CDATA, join(" ", classes));

            ch.startElement(null, null, LI, checkboxAttrs);
            aProceessChildren.run();
            ch.endElement(null, null, LI);

            alignEnd(aHandler, aTaskListItem);
        }

        private void heading(ContentHandler aHandler, Heading aHeading, Runnable aProceessChildren)
            throws SAXException
        {
            alignStart(aHandler, aHeading);

            ch.startElement(null, null, "h" + aHeading.getLevel(), null);
            aProceessChildren.run();
            ch.endElement(null, null, "h" + aHeading.getLevel());

            alignEnd(aHandler, aHeading);
        }

        private void tableBlock(ContentHandler aHandler, TableBlock aTable,
                Runnable aProceessChildren)
            throws SAXException
        {
            alignStart(aHandler, aTable);

            ch.startElement(null, null, TABLE_WRAP, null);
            ch.startElement(null, null, TABLE, null);
            aProceessChildren.run();
            ch.endElement(null, null, TABLE);
            ch.endElement(null, null, TABLE_WRAP);

            alignEnd(aHandler, aTable);
        }

        private void tableRow(ContentHandler aHandler, TableRow aTableCell,
                Runnable aProceessChildren)
            throws SAXException
        {
            if (tableSeparator) {
                alignStart(aHandler, aTableCell);

                var attrs = new AttributesImpl();
                attrs.addAttribute("", "", ATTR_CLASS, CDATA, IAA_MD_D_NONE);

                ch.startElement(null, null, TR, attrs);
                aProceessChildren.run();
                ch.endElement(null, null, TR);

                alignEnd(aHandler, aTableCell);
                return;
            }

            wrap(aHandler, aTableCell, aProceessChildren, TR);
        }

        private void tableCell(ContentHandler aHandler, TableCell aTableCell,
                Runnable aProceessChildren)
            throws SAXException
        {
            var attrs = new AttributesImpl();
            if (aTableCell.getSpan() > 1) {
                attrs.addAttribute("", "", "colspan", CDATA, String.valueOf(aTableCell.getSpan()));
            }

            if (aTableCell.isHeader()) {
                wrap(aHandler, aTableCell, aProceessChildren, TH, attrs);
                return;
            }

            wrap(aHandler, aTableCell, aProceessChildren, TD, attrs);
        }

        private void footnote(ContentHandler aHandler, Footnote aHeading,
                Runnable aProceessChildren)
            throws SAXException
        {
            alignStart(aHandler, aHeading);

            ch.startElement(null, null, SPAN, null);
            aProceessChildren.run();
            ch.endElement(null, null, SPAN);

            alignEnd(aHandler, aHeading);
        }

        private void wrap(ContentHandler aHandler, Node aNode, Runnable aProceessChildren,
                String aQName)
            throws SAXException
        {
            wrap(aHandler, aNode, aProceessChildren, aQName, null);
        }

        private void wrap(ContentHandler aHandler, Node aNode, Runnable aProceessChildren,
                String aQName, Attributes aAttrs)
            throws SAXException
        {
            alignStart(aHandler, aNode);

            ch.startElement(null, null, aQName, aAttrs);
            aProceessChildren.run();
            ch.endElement(null, null, aQName);

            alignEnd(aHandler, aNode);
        }

        void text(ContentHandler aHandler, Node aNode, Runnable aProceessChildren)
            throws SAXException
        {
            alignStart(aHandler, aNode);

            if (fencedCodeBlock) {
                ch.startElement(null, null, "pre", null);
            }

            var chars = aNode.getChars().toString().toCharArray();
            aHandler.characters(chars, 0, chars.length);
            processedText += chars.length;

            LOG.trace("[{}-{}] [{}]", aNode.getStartOffset(), aNode.getEndOffset(),
                    aNode.getChars());

            if (fencedCodeBlock) {
                ch.endElement(null, null, "pre");
            }

            alignEnd(aHandler, aNode);
        }

        private void alignStart(ContentHandler aHandler, Node aNode) throws SAXException
        {
            if (processedText > aNode.getStartOffset()) {
                throw new IllegalStateException("Misalignment between node and text. Aborting!");
            }

            while (processedText < aNode.getStartOffset()) {
                aHandler.characters(new char[] { ' ' }, 0, 1);
                processedText++;
            }
        }

        private void alignEnd(ContentHandler aHandler, Node aNode) throws SAXException
        {
            if (processedText > aNode.getEndOffset()) {
                throw new IllegalStateException("Misalignment between node and text. Aborting!");
            }

            while (processedText < aNode.getEndOffset()) {
                aHandler.characters(new char[] { ' ' }, 0, 1);
                processedText++;
            }
        }
    }
}
