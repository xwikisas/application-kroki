
/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.kroki.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.kroki.macro.KrokiMacroParameters;
import org.xwiki.model.EntityType;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import com.xwiki.kroki.KrokiTagCloudMacroParameters;

/**
 * Kroki Tag Cloud Macro.
 *
 * @version $Id$
 */
@Component
@Named("kroki-tagcloud")
@Singleton
public class KrokiTagCloudMacro extends AbstractMacro<KrokiTagCloudMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Kroki Tag Cloud Macro";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("kroki")
    private Macro krokiMacro;

    @Inject
    private QueryManager queryManager;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public KrokiTagCloudMacro()
    {
        super("TagCloud", DESCRIPTION, KrokiTagCloudMacroParameters.class);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(KrokiTagCloudMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        List<Block> result = new ArrayList<Block>();
        String space = parameters.getSpace();
        XWikiContext xcontext = xcontextProvider.get();
        List<String> tags = null;
        String queryString =
            "select distinct doc.fullName from XWikiDocument doc, BaseObject as tagObj, " + "DBStringListProperty "
                + "as tags join tags.list tag where doc.fullName = tagObj.name and tagObj.className = 'XWiki.TagClass' "
                + "and tagObj.id = tags.id.id and tags.id.name='tags' and tag <> '' and doc.web = '" + space + "'";
        try {
            Query configQuery = queryManager.createQuery(queryString, Query.HQL);
            List<String> documentsInSpace = configQuery.execute();
            List<XWikiDocument> documents = new ArrayList<>();
            for (String docName : documentsInSpace) {
                documents.add(xcontext.getWiki().getDocument(docName, EntityType.DOCUMENT, xcontext));
            }
            tags =
                documents.stream().map(xWikiDocument -> Arrays.asList(xWikiDocument.getTags(xcontext).split("\\|")))
                    .flatMap(Collection::stream).collect(Collectors.toList());
        } catch (QueryException | XWikiException e) {
            throw new RuntimeException(e);
        }

        if (tags == null || tags.isEmpty()) {
            result.add(new ParagraphBlock(Collections.singletonList(new WordBlock("No tags found"))));
        } else {
            String tagsString = String.join(" ", tags);
            String graphContent = "{\n   \"$schema\": \"https://vega.github.io/schema/vega/v5.json\",\n"
                + "    \"width\": 800,\n    \"height\": 400,\n    \"padding\": 0,\n  \n    \"data\": [\n"
                + "      {\n        \"name\": \"table\",\n        \"values\": [\n          \"" + tagsString + "\"\n"
                + "        ],\n        \"transform\": [\n          {\n            \"type\": \"countpattern\",\n"
                + "            \"field\": \"data\",\n            \"case\": \"upper\",\n"
                + "            \"pattern\": \"[\\\\w']{3,}\"\n          },\n          {\n"
                + "            \"type\": \"formula\", \"as\": \"angle\",\n"
                + "            \"expr\": \"[-45, 0, 45][~~(random() * 3)]\"\n          },\n"
                + "          {\n            \"type\": \"formula\", \"as\": \"weight\",\n"
                + "            \"expr\": \"if(datum.text=='VEGA', 600, 300)\"\n          }\n        ]\n"
                + "      }\n    ],\n  \n    \"scales\": [\n      {\n        \"name\": \"color\",\n"
                + "        \"type\": \"ordinal\",\n        \"domain\": {\"data\": \"table\", \"field\": \"text\"},\n"
                + "        \"range\": [\"#d5a928\", \"#652c90\", \"#939597\"]\n      }\n    ],\n  \n"
                + "    \"marks\": [\n      {\n        \"type\": \"text\",\n        \"from\": {\"data\": \"table\"},\n"
                + "        \"encode\": {\n          \"enter\": {\n            \"text\": {\"field\": \"text\"},\n"
                + "            \"align\": {\"value\": \"center\"},\n"
                + "            \"baseline\": {\"value\": \"alphabetic\"},\n"
                + "            \"fill\": {\"scale\": \"color\", \"field\": \"text\"}\n          },\n"
                + "          \"update\": {\n            \"fillOpacity\": {\"value\": 1}\n"
                + "          },\n          \"hover\": {\n            \"fillOpacity\": {\"value\": 0.5}\n"
                + "          }\n        },\n        \"transform\": [\n          {\n"
                + "            \"type\": \"wordcloud\",\n            \"size\": [800, 400],\n"
                + "            \"text\": {\"field\": \"text\"},\n"
                + "            \"rotate\": {\"field\": \"datum.angle\"},\n"
                + "            \"font\": \"Helvetica Neue, Arial\",\n"
                + "            \"fontSize\": {\"field\": \"datum.count\"},\n"
                + "            \"fontWeight\": {\"field\": \"datum.weight\"},\n"
                + "            \"fontSizeRange\": [12, 56],\n"
                + "            \"padding\": 2\n          }\n        ]\n      }\n    ]\n  }";

            KrokiMacroParameters macroParameters = new KrokiMacroParameters();
            macroParameters.setDiagramType("vega");
            macroParameters.setOutputType("svg");
            result = krokiMacro.execute(macroParameters, graphContent, context);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return false;
    }
}
