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
 import java.util.Collections;
 import java.util.List;
 import java.util.Set;

 import javax.inject.Inject;
 import javax.inject.Named;
 import javax.inject.Provider;
 import javax.inject.Singleton;

 import org.dom4j.Element;
 import org.xwiki.component.annotation.Component;
 import org.xwiki.contrib.kroki.macro.KrokiClassDiagramMacroParameters;
 import org.xwiki.contrib.kroki.macro.KrokiMacroParameters;
 import org.xwiki.model.EntityType;
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
 import com.xpn.xwiki.objects.PropertyInterface;
 import com.xpn.xwiki.objects.classes.BaseClass;


 import com.xwiki.kroki.KrokiClassDiagramMacroParameters;



 /**
  * Kroki Class Diagram Macro.
  *
  * @version $Id$
  */
 @Component
 @Named("kroki-class-diagram")
 @Singleton
 public class KrokiClassDiagramMacro extends AbstractMacro<KrokiClassDiagramMacroParameters>
 {
     /**
      * The description of the macro.
      */
     private static final String DESCRIPTION = "Kroki Class Diagram Macro";

     @Inject
     protected Provider<XWikiContext> xcontextProvider;

     @Inject
     @Named("kroki")
     private Macro krokiMacro;

     /**
      * Create and initialize the descriptor of the macro.
      */
     public KrokiClassDiagramMacro()
     {
         super("Class Diagram", DESCRIPTION, KrokiClassDiagramMacroParameters.class);
     }

     /**
      * {@inheritDoc}
      *
      * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
      */
     public List<Block> execute(KrokiClassDiagramMacroParameters parameters, String content,
         MacroTransformationContext context) throws MacroExecutionException
     {
         List<Block> result = new ArrayList<Block>();
         String className = parameters.getClassName();
         XWikiContext xcontext = xcontextProvider.get();
         StringBuilder contentBuilder = new StringBuilder();
         try {
             XWikiDocument classDoc = xcontext.getWiki().getDocument(className, EntityType.DOCUMENT, xcontext);
             BaseClass xClass = classDoc.getXClass();
             contentBuilder.append("[").append(classDoc.getName());
             Set<String> propertiesSet = xClass.getPropertyList();

             for (String propertyName : propertiesSet) {
                 PropertyInterface propertyInterface = xClass.get(propertyName);
                 String propName = propertyInterface.getName();
                 contentBuilder.append("|");
                 contentBuilder.append(propName).append(": ");
                 Element propXML = propertyInterface.toXML();
                 Element classElement = propXML.element("classType");
                 String propType = classElement.getStringValue();
                 contentBuilder.append(lastWord(propType, "\\."));
             }
             contentBuilder.append("]");
         } catch (XWikiException ignored) {
         }

         if (contentBuilder.length() == 0) {
             result.add(new ParagraphBlock(Collections.singletonList(
                 new WordBlock("Could not create UML diagram for " + "class " + parameters.getClassName()))));
         } else {
             KrokiMacroParameters macroParameters = new KrokiMacroParameters();
             macroParameters.setDiagramType("nomnoml");
             macroParameters.setOutputType("svg");
             result = krokiMacro.execute(macroParameters, contentBuilder.toString(), context);
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

     private String lastWord(String sentence, String separator)
     {
         String[] wordArray = sentence.split(separator);
         if (wordArray.length == 0) {
             return null;
         }
         return wordArray[wordArray.length - 1];
     }
 }

