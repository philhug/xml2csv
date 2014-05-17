/***********************************************************************************************************
 * XML2CSV-Generic-Converter - flatten XML into CSV to suit your mood.
 * Copyright 2014 Laurent Popieul (lochrann@rocketmail.com)
 * *********************************************************************************************************
 * This file is part of XML2CSV-Generic-Converter.
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 ***********************************************************************************************************/
package utils.xml.xml2csv;

import utils.xml.xml2csv.constants.XML2CSVCardinality;
import utils.xml.xml2csv.constants.XML2CSVLogLevel;
import utils.xml.xml2csv.constants.XML2CSVMisc;
import utils.xml.xml2csv.constants.XML2CSVNature;
import utils.xml.xml2csv.constants.XML2CSVOptimization;
import utils.xml.xml2csv.constants.XML2CSVType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This SAX handler implementation parses the data of an XML input file in order to extract:<br>
 * <ul>
 * <li>either an explicit list of expected leaf elements if such a list was provided in the first place;
 * <li>or all the XML leaf elements if no such list was provided.
 * </ul>
 * The {@link utils.xml.xml2csv.constants.XML2CSVOptimization#STANDARD STANDARD} optimization level implies data buffering and intermediate data packing before the data is actually
 * sent to the CSV output writer {@link utils.xml.xml2csv.OutputWriterFacade facade} provided to the class instance constructor.<br>
 * The amount of data buffered depends on the actual XML leaf elements tracked: buffering starts when the parent XPath of a tracked XML element starts and ends up when the last
 * parent XPath of tracked XML elements ends (the last because other tracked parent XPaths might be opened in cascade in between).<br>
 * The {@link utils.xml.xml2csv.constants.XML2CSVOptimization#STANDARD STANDARD} data packing operation consists in regrouping on the same line cells corresponding to
 * mono-occurrence elements of the same block (i.e. within the same parent block).<br>
 * When the optimization phase is over the buffer is flushed to the output and the rest of the XML input file is processed on the same <i>buffer-then-pack-then-flush</i> basis.<br>
 * This buffering strategy is fairly efficient and the buffer size remains relatively small no matter what the XML input file size is <b>*but*</b> for one case, that is, namely, if
 * it happens that one of the tracked XML leaf elements is both multi-occurrence and also <b>*heavily*</b> repeated (something like thousands of times): in such situation the
 * buffer size would grow accordingly in order to fit the data before the program actually gets a chance to flush it.<br>
 * In those rare situations the program might run out of memory and go bye-bye.<br>
 * In such case, the use should consider:<br>
 * <ul>
 * <li>either to narrow the extraction in order to discard such a pathological XML leaf element from the track list if it is not really needed, and then rerun the program;
 * <li>or to rerun the program with deactivated optimization (input parameter <code>level</code> = {@link utils.xml.xml2csv.constants.XML2CSVOptimization#NONE NONE}) in order to
 * turn both buffering and data packing off.
 * </ul>
 * The same SAX handler can be used to parse several XML input files in a row, one at a time, provided that its {@link utils.xml.xml2csv.DataHandler#reset() reset} method be called
 * between parsings.<br>
 * The {@link utils.xml.xml2csv.OutputWriterFacade OutputWriterFacade} instance provided to the constructor, which is in charge of the actual CSV file writings, might be
 * transparently reconfigured between parsings.<br>
 * An additional indicator, <code>singleHeader</code>, makes it possible to decide if column names have to be sent out the output for each new XML input file parsed, or once only
 * for the first XML input file parsed by the handler.
 * @author L. Popieul (lochrann@rocketmail.com)
 */
class DataHandler extends DefaultHandler implements LexicalHandler
{
  /** The character encoding of the XML input file. */
  private String inputEncoding = null;

  /** Single header indicator. */
  private boolean singleHeader = false;

  /** The root tag of the XML input file. */
  private String rootTag = null;

  /** Some particular SAX extension class to get access to the XML input encoding. */
  private Locator2 locator = null;

  /** CSV output file facade. */
  private OutputWriterFacade outputWriterFacade = null;

  /** The chosen CSV field separator. */
  private String fieldSeparator = null;

  /** The chosen character encoding for the output. */
  private Charset outputEncoding = null;

  /** Counts the number of XML documents parsed (useful in order to display headers once only). */
  private int parsedXMLDocumentCount = 0;

  /** Counts the number of parent elements of tracked elements currently opened. */
  private int trackedElementParentCount = 0;

  /** The chosen optimization level. */
  private XML2CSVOptimization level = null;

  /** Intermediary parsing variable: records element contents alongside the parsing. */
  private StringBuffer textBuffer = null;

  /** Ordered list of the currently parsed XML tag sequence (that is, for &lt;A&gt;&lt;B&gt;&lt;C&gt;, {A,B,C}). */
  private ArrayList<String> currentXMLTagSequence = new ArrayList<String>();

  /** The XPath list of expected elements in the CSV output file(s). */
  private String[] expectedElementsXpaths = null;

  /** The XPath list of elements to discard in the CSV output file(s). */
  private String[] discardedElementsXpaths = null;

  /** XML structure analysis: the ordered description list of leaf elements in the input XML files. */
  private ElementsDescription leafElementsDescription = null;

  /** XML data tracking: the ordered description list of tracked leaf elements in the input XML files. */
  private ElementsDescription trackedLeafElementsDescription = null;

  /** Data buffer used by this handler in regular/optimized mode. */
  DataBuffer dataBuffer = null;

  /** Attributes extraction indicator. */
  private boolean withAttributes = false;

  /** Name space awareness indicator. */
  private boolean withNamespaces = false;

  /** Unleashed optimization indicator. */
  private boolean unleashed = false;

  /** Attributes recorded in a hash map where the key is The.Element.Path and the value an ordered list of String arrays providing each: attribute name (index 0), value (index 1). */
  private HashMap<String, ArrayList<String[]>> attributes = new HashMap<String, ArrayList<String[]>>();

  /**
   * <code>DataHandler</code> constructor.
   * @param outputWriterFacade a non null <code>OutputWriterFacade</code> in charge of actual output, expected {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} when the
   *        actual XML parsing starts.
   * @param fieldSeparator the field separator to use in the CSV output file, or <code>null</code> to use the default field separator.
   * @param encoding the encoding to use in the CSV output file(s), or <code>null</code> to use the default encoding.
   * @param leafElementsDescription the description of the leaf elements of the XML input file.
   * @param expectedElementsXpaths an explicit list of element XPaths to extract from the XML input file, or <code>null</code> to have all leaf elements extracted.
   * @param discardedElementsXpaths an explicit list of element XPaths to discard from the XML input file, or <code>null</code> to have all leaf elements extracted.
   * @param level the chosen <code>XML2CSVOptimization</code> level (defaults to <code>STANDARD</code> it left <code>null</code>).
   * @param singleHeader <code>true</code> to have the column names displayed for the first file only, or <code>false</code> to have the column names atop each output file.
   * @param withAttributes <code>true</code> if element attributes should be extracted as well or <code>false</code> otherwise.
   * @param withNamespaces <code>true</code> if name space aware parsing should be performed or <code>false</code> otherwise.
   * @param unleashed <code>true</code> to deactivate the built-in root-safe optimization mechanism or <code>false</code> otherwise.
   */
  public DataHandler(OutputWriterFacade outputWriterFacade, String fieldSeparator, Charset encoding, ElementsDescription leafElementsDescription, String[] expectedElementsXpaths,
      String[] discardedElementsXpaths, XML2CSVOptimization level, boolean singleHeader, boolean withAttributes, boolean withNamespaces, boolean unleashed)
  {
    super();

    this.outputWriterFacade = outputWriterFacade;

    if (fieldSeparator != null) this.fieldSeparator = fieldSeparator;
    else
      this.fieldSeparator = XML2CSVMisc.DEFAULT_FIELD_SEPARATOR;

    if (encoding == null) this.outputEncoding = Charset.forName(XML2CSVMisc.UTF8);
    else
      this.outputEncoding = encoding;

    this.singleHeader = singleHeader;

    if (level != null) this.level = level;
    else
      this.level = XML2CSVOptimization.STANDARD;

    this.leafElementsDescription = leafElementsDescription;

    this.withAttributes = withAttributes;

    this.withNamespaces = withNamespaces;

    this.unleashed = unleashed;

    // Convention1: a null expectedElementsXpaths object means that there is no expected element list, or in other words, that all XML leaf elements are expected to
    // be sent to the CSV output file.
    // Convention2: expectedElementsXpaths might contain null elements (one for each incorrect XPath actually ignored).
    this.expectedElementsXpaths = expectedElementsXpaths;

    // Same conventions for discardedElementsXpaths and expectedElementsXpaths.
    this.discardedElementsXpaths = discardedElementsXpaths;

    // Computes the list of tracked XML elements.
    computeTrackedElementsDescription();

    // Initializes the data buffer used by this data handler in regular/optimized mode.
    dataBuffer = new DataBuffer(outputWriterFacade, fieldSeparator, trackedLeafElementsDescription, level);
  }

  // ================================================================================
  // ContentHandler method implementation
  // ================================================================================

  @Override
  public void startDocument() throws SAXException
  {
    if (locator != null) inputEncoding = locator.getEncoding();

    if (outputWriterFacade == null) throw new SAXException("Null CSV output stream.");

    parsedXMLDocumentCount++;

    if ((singleHeader == false) || ((singleHeader == true) && (parsedXMLDocumentCount == 1)))
    {
      if (withNamespaces == true)
      {
        // In case of name space aware parsing the name space list is displayed on the very first line (something like alias1=namespace1 alias2=namespace2 ...)
        String namespaceList = buildCSVOutputNamespaceLine(trackedLeafElementsDescription.getNamespaces());
        if (namespaceList.isEmpty() == false)
        {
          // Both name space aware parsing is activated and the XML input files use name spaces. The name space list is displayed.
          emit(namespaceList);
          ls(); // Next line.
          ls(); // Next line.
        }
        else
        {
          // Name space aware parsing is activated but the XML input files do not use name spaces. Nothing is displayed.
        }
      }
      // Before the first XML input file is processed the full XPaths of the tracked element's parents are sent to the CSV output file.
      // emit(buildCSVOutputHeaderLine(trackedLeafElementParentXPaths));
      // ls(); // Next line.
      // Before the first XML input file is processed the full XPaths of the tracked elements are sent to the CSV output file.
      emit(buildCSVOutputHeaderLine(trackedLeafElementsDescription.getElementsXPaths()));
      ls(); // Next line.
      // Before the first XML input file is processed the short column names of the tracked elements are sent to the CSV output file too.
      // emit(buildCSVOutputHeaderLine(trackedLeafElementShortNames));
      // ls(); // Next line.
    }

    if (parsedXMLDocumentCount == 1)
    {
      // Before the first XML input file is processed we compare the XML input encoding (expected shared by all XML input files) and the CSV output encoding.
      // They might be different, comparison is made for verbose display only.
      if ((inputEncoding != null) && (inputEncoding.equals(outputEncoding.displayName()) == false)) XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, "output character encoding <"
          + outputEncoding.displayName() + "> different from the input character encoding <" + inputEncoding + ">.");

      if ((XML2CSVLoggingFacade.VERBOSE_MODE == true) || (XML2CSVLoggingFacade.DEBUG_MODE == true))
      {
        // Displays synthetic informations about the tracked XML elements.
        XML2CSVLogLevel lvl = null;
        if (XML2CSVLoggingFacade.DEBUG_MODE == true) lvl = XML2CSVLogLevel.DEBUG;
        else
          lvl = XML2CSVLogLevel.VERBOSE;
        XML2CSVLoggingFacade.log(lvl, "tracked element list & properties:");

        for (int i = 0; i < trackedLeafElementsDescription.getElementsParentXPaths().length; i++)
        {
          StringBuffer sb = new StringBuffer();
          sb.append("\t");
          sb.append("column <");
          sb.append(i);
          sb.append(">=[");
          sb.append(trackedLeafElementsDescription.getElementsParentXPaths()[i]);
          sb.append(".");
          sb.append(trackedLeafElementsDescription.getElementsShortNames()[i]);
          sb.append("|");
          sb.append(trackedLeafElementsDescription.getElementsCardinalities()[i]);
          sb.append("|");
          sb.append(trackedLeafElementsDescription.getElementsTypes()[i]);
          sb.append("]");
          XML2CSVLoggingFacade.log(lvl, sb.toString());
        }
      }

      // Displays the whole unstructured dictionary contents if the debug degree is >=3.
      if ((XML2CSVLoggingFacade.DEBUG_MODE == true) && (XML2CSVLoggingFacade.debugDegree >= XML2CSVLogLevel.DEBUG3.getDegree()))
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG3, "unstructured dictionary contents:");
        HashMap<String, String[]> dictionary = trackedLeafElementsDescription.getDictionary();
        Iterator<String> iterator = dictionary.keySet().iterator();
        while (iterator.hasNext())
        {
          String oneXpath = iterator.next();
          String[] props = dictionary.get(oneXpath); // Index 0 = cardinality, index 1 = type, index 2 = occurrence global count in template file, index 3 = nature.
          StringBuffer sb = new StringBuffer();
          sb.append("\t");
          sb.append("column <");
          sb.append(oneXpath);
          sb.append(">=[");
          for (int i = 0; i < props.length - 1; i++)
          {
            sb.append(props[i]);
            sb.append("|");
          }
          sb.append(props[props.length - 1]);
          sb.append("]");
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG3, sb.toString());
        }
      }
    }
  }

  @Override
  public void endDocument() throws SAXException
  {
    try
    {
      outputWriterFacade.flush();
    }
    catch (IOException ioe)
    {
      throw new SAXException("I/O error", ioe);
    }
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException
  {
    // Adds the current tag to the current tag list (element opening).
    String eName = null;
    if (withNamespaces == false)
    {
      // Space less parsing: we use the current element name without its name space alias/prefix, that is, localName and not qName.
      eName = localName;
    }
    else
    {
      // Space full parsing: we use the current element name with its name space alias/prefix, that is, qName and not localName.
      eName = qName;
    }
    // Dots (.) in tag names are replaced by stars (*) because the dot is used by this handler as tag separator in XPaths.
    eName = EscapeUtils.escapeXML10TagName(eName);
    // Character @, which should not appear in correct XML tag names, is used by this handler later on to differentiate attributes from elements.
    if (localName.contains("@")) throw new SAXException("Bad XML holding tags containing '@' characters, which are forbidden.");
    // Character :, which should not appear in correct XML tag names, is used by this handler for name space aware parsing to separate the element's short name from its prefix.
    if (localName.contains(":")) throw new SAXException("Bad XML holding tags containing ':' characters, which are forbidden.");
    // Character #, which should not appear in correct XML tag names, is used by this handler for virtual attributes (name+content) implied in the most extensive optimization mode.
    if (localName.contains("#")) throw new SAXException("Bad XML holding tags containing '#' characters, which are forbidden.");
    currentXMLTagSequence.add(eName);

    // Root element detection
    if (rootTag == null) rootTag = eName;

    // Current element contents buffer reset before the element is started.
    textBuffer = null;

    // Increases the number of opened tracked element parents if it happens that the current opened element is a parent of a tracked element,
    // provided that it is not the root element (the root element is explicitly excluded for performance purpose even when it is nesting a
    // tracked element because including it would mean to wait for the root tag end to reach consistency and flush the buffer, that is, in
    // other words, to read the whole XML file before processing it which is definitely not what we want).
    // Sends the information to the data buffer too for optimization purpose. Does nothing in raw mode i.e. when optimization is deactivated.
    if (level != XML2CSVOptimization.NONE)
    {
      boolean record = false;
      if (isParentOfTrackedElement() == true)
      {
        if (unleashed == true) record = true;
        if ((unleashed == false) && (isRootElement() == false)) record = true;
      }
      if (record == true)
      {
        trackedElementParentCount++;
        dataBuffer.recordTrackedParentOpening(getXPathAsString(currentXMLTagSequence));
      }
    }

    // If attributes are expected and the current opening element has attributes then:
    // -1- if the element is a leaf element its attributes are recorded and their output is deferred until the element closing is actually met
    // (depending on whether the element is a tracked one or not in order to have the element attributes appear after (= below) the element content;
    // -2- if the element is not a leaf element then it cannot belong to the tracked elements (which are a subset of the leaf elements) and there
    // is no use waiting for the element closing: attributes which are tracked are immediately sent to the output.
    // The unstructured dictionary holds the leaf/intermediary information for each entry it holds but when an element pops up from nowhere in a file
    // outside the scope of the template file the dictionary doesn't contain its definition and the element cannot by construction be tracked either and,
    // as a result, the element's attributes should be treated like in option -2-.
    if ((withAttributes == true) && (atts != null) && (atts.getLength() != 0))
    {
      String[] props = trackedLeafElementsDescription.getDictionary().get(getXPathAsString(currentXMLTagSequence));
      recordAttributes(atts); // Stacks the attributes no matter which option -1- or -2- is chosen.
      boolean isLeaf = false;
      if (props != null)
      {
        // OK, the element is defined in the dictionary: regular option -1- or -2- depending on whether it is a leaf element or not.
        isLeaf = XML2CSVNature.isLeaf(props[props.length - 1]); // The nature is the last of the element properties.
      }
      else
      {
        // The element does not exit in the dictionary: option -2- by default.
        isLeaf = false;
      }
      if (isLeaf == false) handleAttributes(); // Option -2-: un-stacks and handles immediately the attributes of an intermediate element.
      else
      {
        // Option -1-: the attributes are already stacked and will be handled when the element is closed.
      }
    }
    else
    {
      // The element has no real attribute value.
      // In the most extensive optimization mode only, an extra virtual hidden attribute value is generated for each sub-root intermediate element (generation for leaf
      // elements is useless) which was granted a virtual attribute definition and which has no real attribute value.
      // This data handler will generate a virtual hidden attribute value no matter if the element has an actual content (good) or not (less good, for it causes bad edge
      // packing effects [extra pointless lines sent to the output]): indeed, the handler doesn't accumulate knowledge about elements alongside parsing and cannot sort
      // things out at this level.
      // Annoying virtual hidden attribute values generated here for intermediate elements without content have to be explicitly removed from within the data buffer
      // later on just before the optimization phase begins (knowledge about element contents can be devised at that time and the annoying virtual data removed before
      // they cause trouble).
      if (level == XML2CSVOptimization.EXTENSIVE_V3)
      {
        String[] props = trackedLeafElementsDescription.getDictionary().get(getXPathAsString(currentXMLTagSequence));
        boolean isLeaf = false;
        if (props != null)
        {
          // OK, the element is defined in the dictionary: we can devise if it's a intermediate element or not.
          isLeaf = XML2CSVNature.isLeaf(props[props.length - 1]); // The nature is the last of the element properties.
        }
        else
        {
          // The element does not exist in the dictionary: it wasn't met in the template file during structure analysis phase.
          // We don't generate a virtual attribute, just like if it was a leaf element.
          isLeaf = true;
        }
        if (isLeaf == false) generateVirtualAttribute();
      }
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException
  {
    // Retrieves the current element content and either buffers it or sends it to the CSV output file depending whether the raw mode is active or not.
    String content = getText(true);
    int index = getLeafElementCSVColumnIndex(getXPathAsString(currentXMLTagSequence));
    if (index == -1)
    {
      // The current closing XML element is not a leaf XML element or it is a leaf XML element but it does not belong to the tracked XML leaf elements.
      // We do nothing.
    }
    else
    {
      if (level == XML2CSVOptimization.NONE)
      {
        // This is a raw mode execution.
        // The current closing XML element belongs to the tracked XML leaf elements.
        // Because we are in raw mode the XML element content is placed in a new CSV line at the corresponding index (a CSV line with
        // trackedLeafElementXPaths.length fields). Then, the line is immediately sent to the CSV output file.
        String line = buildRawCSVOutputLine(index, trackedLeafElementsDescription.getElementsXPaths().length, content);
        emit(line);
        ls(); // Next line.
      }
      else
      {
        // This is a regular buffered/optimized execution.
        // The current closing XML element belongs to the tracked XML leaf elements.
        // If regular mode the XML element content is buffered.
        dataBuffer.addContent(index, content);
      }
    }

    // Time to handle element attributes for leaf elements if any, and if expected.
    // The output of leaf element attributes is deferred until the element closing is actually met in order to have the element attributes
    // appear before optimization after (= below) the element (if the element is tracked).
    // The output of intermediate element attributes was handled in method startElement because intermediate elements are never tracked.
    if (withAttributes == true)
    {
      String[] props = trackedLeafElementsDescription.getDictionary().get(getXPathAsString(currentXMLTagSequence));
      boolean isLeaf = false;
      if (props != null)
      {
        // OK, the element is defined in the dictionary and its nature (leaf or intermediary element) is defined.
        isLeaf = XML2CSVNature.isLeaf(props[props.length - 1]); // The nature is the last of the element properties.
      }
      else
      {
        // The element does not exit in the dictionary and its nature is undefined.
        // Its attributes were handled in method startElement like for intermediate elements.
        isLeaf = false;
      }
      if (isLeaf == true) handleAttributes(); // Un-stacks and handles now the attributes of a leaf element.
      else
      {
        // The attributes were handled when the element was opened. Nothing to do.
      }
    }

    // Decreases the number of opened tracked element parents if it happens that the current closed element is a parent of a tracked element,
    // provided that it is not the root element (the root element is explicitly excluded for performance purpose even when it is nesting a
    // tracked element because including it would mean to wait for the root tag end to reach consistency and flush the buffer, that is, in
    // other words, to read the whole XML file before processing it which is definitely not what we want).
    // Sends the information to the data buffer too for optimization purpose. Does nothing in raw mode i.e. when optimization is deactivated.
    if (level != XML2CSVOptimization.NONE)
    {
      boolean record = false;
      if (isParentOfTrackedElement() == true)
      {
        if (unleashed == true) record = true;
        if ((unleashed == false) && (isRootElement() == false)) record = true;
      }
      if (record == true)
      {
        trackedElementParentCount--;
        dataBuffer.recordTrackedParentClosing(getXPathAsString(currentXMLTagSequence));
      }
    }

    // Removes the current tag from the current tag list (element closure).
    currentXMLTagSequence.remove(currentXMLTagSequence.size() - 1);

    // If the number of opened tracked element parents has reached zero we trigger both buffer optimization and buffer flush to the CSV output file.
    // If the data buffer happens to be empty the optimize and flush methods will actually do nothing.
    // Of course, we do nothing in raw mode i.e. when optimization is deactivated.
    if (level != XML2CSVOptimization.NONE)
    {
      if (trackedElementParentCount == 0)
      {
        if ((level == XML2CSVOptimization.EXTENSIVE_V2) || (level == XML2CSVOptimization.EXTENSIVE_V3)) dataBuffer.optimizeV23();
        else
          dataBuffer.optimizeV1(); // STANDARD and EXTENSIVE_V1
        dataBuffer.flush();
      }
    }
  }

  @Override
  public void characters(char buf[], int offset, int len) throws SAXException
  {
    // Current element contents, or garbage characters (such as blanks) between regular tag.
    String s = new String(buf, offset, len);
    if (textBuffer == null) textBuffer = new StringBuffer(s);
    else
      textBuffer.append(s);
  }

  @Override
  public void ignorableWhitespace(char ch[], int start, int length) throws SAXException
  {
    // String s = new String(ch, start, length);
    // System.out.println("ignorableWhitespace: s="+s+".");
  }

  @Override
  public void processingInstruction(String target, String data) throws SAXException
  {
    // System.out.println("processingInstruction: target="+target+" data="+data);
  }

  @Override
  public void skippedEntity(String name) throws SAXException
  {
    // System.out.println("skippedEntity: name="+name);
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException
  {
    // System.out.println("startPrefixMapping: prefix="+prefix+ " uri="+uri);
  }

  @Override
  public void endPrefixMapping(String prefix) throws SAXException
  {
    // System.out.println("endPrefixMapping: prefix="+prefix);
  }

  @Override
  public void setDocumentLocator(Locator locator)
  {
    if (locator instanceof Locator2) this.locator = (Locator2) locator;
    super.setDocumentLocator(locator);
  }

  // ================================================================================
  // LexicalHandler method implementation
  // ================================================================================

  @Override
  public void startDTD(String name, String publicId, String systemId) throws SAXException
  {
    // System.out.println("startDTD: name=" + name + " publicId=" + publicId + " systemId=" + systemId);
  }

  @Override
  public void endDTD() throws SAXException
  {
    // System.out.println("endDTD");
  }

  @Override
  public void startEntity(String name) throws SAXException
  {
    // System.out.println("startEntity: name=" + name);
  }

  @Override
  public void endEntity(String name) throws SAXException
  {
    // System.out.println("endEntity: name=" + name);
  }

  @Override
  public void startCDATA() throws SAXException
  {
    // System.out.println("startCDATA");
    /**
     * String s = "<![CDATA["; if (textBuffer == null) { textBuffer = new StringBuffer(s); } else { textBuffer.append(s); }
     */
  }

  @Override
  public void endCDATA() throws SAXException
  {
    // System.out.println("endCDATA");
    /**
     * String s = "]]>"; if (textBuffer == null) { textBuffer = new StringBuffer(s); } else { textBuffer.append(s); }
     */
  }

  @Override
  public void comment(char[] ch, int start, int length) throws SAXException
  {
    // String s = LINE_SEPARATOR + "<!-- " + new String(ch, start, length).trim() + " -->" + LINE_SEPARATOR;
    // System.out.println("comment: s=" + s);
    /**
     * if (textBuffer == null) { textBuffer = new StringBuffer(s); } else { textBuffer.append(s); }
     */
  }

  // ================================================================================
  // Private helper methods
  // ================================================================================

  /**
   * Extracts the XPath of an element represented by its tag list and returns it as a plain <code>String</code> (for instance: returns A.B.C for {A,B,C}).
   * @param tagList the element XPath as an <code>ArrayList&lt;String&gt;</code>.
   * @return the XPath as a plain <code>String</code>, or null when there is no XPath.
   */
  private String getXPathAsString(ArrayList<String> tagList)
  {
    String result = null;
    if ((tagList != null) && (tagList.size() >= 1))
    {
      int tagListSize = tagList.size();
      if (tagListSize == 1) result = tagList.get(0);
      else
      {
        StringBuffer temp = new StringBuffer();
        for (int i = 0; i < tagListSize - 1; i++)
        {
          temp.append(tagList.get(i));
          temp.append(".");
        }
        temp.append(tagList.get(tagListSize - 1));
        result = temp.toString();
      }
    }
    return result;
  }

  /**
   * Sends a string to the CSV output file.
   * @param s the string to send.
   * @throws SAXException in case of error.
   */
  private void emit(String s) throws SAXException
  {
    try
    {
      outputWriterFacade.write(s);
      outputWriterFacade.flush();
    }
    catch (IOException ioe)
    {
      throw new SAXException("I/O error", ioe);
    }
  }

  /**
   * Sends a line separator to the CSV output file (CR-LF, CR or LF depending on the underneath OS).
   * @throws SAXException in case of error.
   */
  private void ls() throws SAXException
  {
    try
    {
      outputWriterFacade.write(XML2CSVMisc.LINE_SEPARATOR);
    }
    catch (IOException ioe)
    {
      throw new SAXException("I/O error", ioe);
    }
  }

  /**
   * Retrieves the current text from the current element text buffer, which is reset alongside.
   * @param trim <code>true</code> to have the text trimmed as well.
   * @throws SAXException in case of error.
   */
  private String getText(boolean trim) throws SAXException
  {
    if (textBuffer == null) return null;
    String s = XML2CSVMisc.EMPTY_STRING + textBuffer.toString();
    textBuffer = null;
    // "Entity References" parsing issue (example: &lt; , ...).
    // The reader.setFeature call in the main class should prevent the "Entity References" (such as &gt; , see http://www.w3schools.com/xml/xml_syntax.asp)
    // from being converted back to XML control character (such as >) in plain text in a declarative way without extra coding but it doesn't work (sigh).
    // As a consequence, re-escaping of control characters in plain text has to be done by hand here.
    // - initially by means of Apache Common's StringEscapeUtils method, but, because this method transforms accentuated characters such as é è à into #nnn;
    // values, something which wasn't done in the original XML files, now,
    // - by means now of an EscapeUtils method call (EscapeUtils being an IBM open source class available over the Internet).
    // s = StringEscapeUtils.escapeXml(s);
    s = EscapeUtils.escapeXML10Chars(s, true, true, true, true);
    // Puts double quotes at the beginning and at the end of the element content if it happens that it contains a line separator or the current CSV field
    // separator to avoid display issues in the CSV output.
    s = EscapeUtils.escapeCSVChars(s, fieldSeparator);
    if (trim == true) s = s.trim();
    return s;
  }

  /**
   * Computes the data of the elements which must be actually tracked in the XML input file.
   */
  private void computeTrackedElementsDescription()
  {
    if ((expectedElementsXpaths == null) && (discardedElementsXpaths == null))
    {
      // When the expected list of elements to track is empty all XML leaf elements should be tracked.
      trackedLeafElementsDescription = leafElementsDescription;
    }
    else
    {
      ArrayList<String> temp1 = new ArrayList<String>();
      ArrayList<String> temp2 = new ArrayList<String>();
      ArrayList<String> temp3 = new ArrayList<String>();
      ArrayList<XML2CSVCardinality> temp4 = new ArrayList<XML2CSVCardinality>();
      ArrayList<XML2CSVType> temp5 = new ArrayList<XML2CSVType>();
      if (expectedElementsXpaths != null)
      {
        // When the expected list of elements to track is not empty only the corresponding XML leaf elements should be tracked.
        for (int i = 0; i < expectedElementsXpaths.length; i++)
        {
          String oneExpectedElementXPath = expectedElementsXpaths[i];
          if (oneExpectedElementXPath != null) // Might have been set to null in order to discard an irrelevant XPath.
          {
            for (int j = 0; j < leafElementsDescription.getElementsXPaths().length; j++)
            {
              boolean include = false;
              String oneLeafElement = leafElementsDescription.getElementsXPaths()[j];
              if (oneLeafElement.equals(oneExpectedElementXPath)) include = true; // Element explicitly included.
              // Any attribute of an included element is included as well, provided that attributes be expected.
              if (include == true)
              {
                temp1.add(leafElementsDescription.getElementsXPaths()[j]);
                temp2.add(leafElementsDescription.getElementsParentXPaths()[j]);
                temp3.add(leafElementsDescription.getElementsShortNames()[j]);
                temp4.add(leafElementsDescription.getElementsCardinalities()[j]);
                temp5.add(leafElementsDescription.getElementsTypes()[j]);
              }
            }
          }
        }
      }
      else
      {
        // When the list of elements to discard is not empty all but the corresponding XML leaf elements should be tracked.
        for (int j = 0; j < leafElementsDescription.getElementsXPaths().length; j++)
        {
          String oneLeafElement = leafElementsDescription.getElementsXPaths()[j];
          boolean discard = false;
          for (int i = 0; i < discardedElementsXpaths.length; i++)
          {
            // discardedElementsXpaths[i] might have been set to null in order to get rid of an irrelevant XPath.
            if (discardedElementsXpaths[i] != null)
            {
              if (oneLeafElement.equals(discardedElementsXpaths[i])) discard = true; // Element explicitly discarded.
              if (discard == true) break;
            }
          }
          if (discard == false)
          {
            temp1.add(leafElementsDescription.getElementsXPaths()[j]);
            temp2.add(leafElementsDescription.getElementsParentXPaths()[j]);
            temp3.add(leafElementsDescription.getElementsShortNames()[j]);
            temp4.add(leafElementsDescription.getElementsCardinalities()[j]);
            temp5.add(leafElementsDescription.getElementsTypes()[j]);
          }
        }
      }
      // The tracked element list cannot be empty at this stage because of the early controls at program start.
      String[] temp = new String[temp1.size()];
      String[] trackedLeafElementXPaths = temp1.toArray(temp);
      temp = new String[temp2.size()];
      String[] trackedLeafElementParentXPaths = temp2.toArray(temp);
      temp = new String[temp3.size()];
      String[] trackedLeafElementShortNames = temp3.toArray(temp);
      XML2CSVCardinality[] ctemp = new XML2CSVCardinality[temp4.size()];
      XML2CSVCardinality[] trackedLeafElementCardinalities = temp4.toArray(ctemp);
      XML2CSVType[] ttemp = new XML2CSVType[temp5.size()];
      XML2CSVType[] trackedLeafElementTypes = temp5.toArray(ttemp);
      // The tracked leaf elements are backed to the same dictionary as the leaf elements (the former being a subset of the latter).
      trackedLeafElementsDescription = new ElementsDescription(trackedLeafElementShortNames, trackedLeafElementXPaths, trackedLeafElementParentXPaths,
          trackedLeafElementCardinalities, trackedLeafElementTypes, leafElementsDescription.getDictionary(), leafElementsDescription.getNamespaces());
    }
  }

  /**
   * Returns the column index of one particular XPath in the CSV output file, or <code>-1</code> if the XPath does not correspond to a tracked XML element.
   * @param xpath the XPath to examine.
   * @return the index of the XPath in the CSV output file, or -1.
   */
  private int getLeafElementCSVColumnIndex(String xpath)
  {
    int result = -1;
    for (int i = 0; i < trackedLeafElementsDescription.getElementsXPaths().length; i++)
    {
      if (trackedLeafElementsDescription.getElementsXPaths()[i].equals(xpath))
      {
        result = i;
        break;
      }
    }
    return result;
  }

  /**
   * Builds a CSV line with <code>numberOfFields</code> empty fields but for the field at the specified <code>index</code> which is provided the specified <code>content</code>.
   * @param index the index of the non empty field.
   * @param numberOfFields the total number of CSV fields of this line.
   * @param content the non empty field content.
   * @return the formatted CSV line ready to be sent to the CSV output file.
   */
  private String buildRawCSVOutputLine(int index, int numberOfFields, String content)
  {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < index; i++)
      result.append(fieldSeparator);
    result.append(content);
    for (int i = index; i < numberOfFields - 1; i++)
      result.append(fieldSeparator);
    return result.toString();
  }

  /**
   * Builds a CSV line listing the different name spaces and their aliases.
   * @param namespaces the name space list.
   * @return the formatted CSV line ready to be sent to the CSV output file.
   */
  private String buildCSVOutputNamespaceLine(HashMap<String, String> namespaces)
  {
    StringBuffer result = new StringBuffer();
    Iterator<String> keys = namespaces.keySet().iterator();
    if (keys.hasNext()) result.append("NAMESPACES: ");
    while (keys.hasNext())
    {
      String alias = keys.next();
      result.append(alias);
      result.append("=");
      String fullName = namespaces.get(alias);
      result.append(fullName);
      if (keys.hasNext()) result.append(" ");
    }
    return result.toString();
  }

  /**
   * Builds a CSV header line with the column names.
   * @param headers the string array which lists the headers to format.
   * @return the formatted CSV line ready to be sent to the CSV output file.
   */
  private String buildCSVOutputHeaderLine(String[] headers)
  {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < headers.length - 1; i++)
    {
      if (headers[i].endsWith("@" + XML2CSVMisc.VIRTUAL_ATTRIBUTE) == false)
      {
        result.append(headers[i]);
        result.append(fieldSeparator);
      }
    }
    if (headers[headers.length - 1].endsWith("@" + XML2CSVMisc.VIRTUAL_ATTRIBUTE) == false) result.append(headers[headers.length - 1]);
    return result.toString();
  }

  /**
   * Checks if the current element is a parent element of one of the tracked XML elements.
   * @return <code>true</code> if the current element is a parent element of one of the tracked XML elements, and <code>false</code> otherwise.
   */
  private boolean isParentOfTrackedElement()
  {
    boolean result = false;
    String xpath = getXPathAsString(currentXMLTagSequence); // Might be null for the root element which has no parent.
    if (xpath != null)
    {
      for (int i = 0; i < trackedLeafElementsDescription.getElementsParentXPaths().length; i++)
      {
        if (xpath.equals(trackedLeafElementsDescription.getElementsParentXPaths()[i]))
        {
          result = true;
          break;
        }
      }
    }
    return result;
  }

  /**
   * Checks if the current element is the root element.
   * @return <code>true</code> if the current element is the root element, and <code>false</code> otherwise.
   */
  private boolean isRootElement()
  {
    boolean result = false;
    String xpath = getXPathAsString(currentXMLTagSequence);
    if ((xpath != null) && (xpath.equals(rootTag))) result = true;
    return result;
  }

  /**
   * Records the attributes of the currently parsed element for later output.
   * @param atts the attributes of the currently parsed element.
   */
  private void recordAttributes(Attributes atts)
  {
    String xpath = getXPathAsString(currentXMLTagSequence);

    ArrayList<String[]> attsList = null;
    if (attributes.containsKey(xpath) == false) attsList = new ArrayList<String[]>();
    else
    {
      attsList = attributes.get(xpath);
      attsList.clear(); // The attributes of the currently parsed element replace any other, if any.
    }
    for (int i = 0; i < atts.getLength(); i++)
    {
      String[] att = new String[2];
      att[0] = atts.getLocalName(i);
      att[1] = atts.getValue(i);
      att[1] = EscapeUtils.escapeXML10Chars(att[1], true, true, true, true); // Handles the XML "Entity References" parsing issue (example: &lt; , ...). Details in method getText.
      att[1] = EscapeUtils.escapeCSVChars(att[1], fieldSeparator); // Escapes CSV control characters if needed. Details in method getText.
      attsList.add(att);
    }
    attributes.put(xpath, attsList);
  }

  /**
   * Generates a virtual attribute value for the current element if it happens that it was provided a virtual attribute definition.
   */
  private void generateVirtualAttribute()
  {
    int indexAtt = getLeafElementCSVColumnIndex(getXPathAsString(currentXMLTagSequence) + "@" + XML2CSVMisc.VIRTUAL_ATTRIBUTE);
    if (indexAtt != -1)
    {
      // The current element has a virtual attribute and a virtual attribute value is generated.
      // The virtual attribute value is not important because the virtual attribute is never displayed, but it cannot be left blank.
      // We use the virtual attribute name as virtual attribute value.
      dataBuffer.addContent(indexAtt, XML2CSVMisc.VIRTUAL_ATTRIBUTE);
    }
    else
    {
      // The current element doesn't have a virtual attribute definition. We do nothing.
    }
  }

  /**
   * Handles attributes for the current element.
   * @throws SAXException in case of error.
   */
  private void handleAttributes() throws SAXException
  {
    ArrayList<String[]> attsList = attributes.get(getXPathAsString(currentXMLTagSequence));
    if (attsList != null)
    {
      for (int i = 0; i < attsList.size(); i++)
      {
        String[] att = attsList.get(i);
        // The column name of an attribute is The.Element.Path + @ + the attribute name.
        int indexAtt = getLeafElementCSVColumnIndex(getXPathAsString(currentXMLTagSequence) + "@" + att[0]);
        if (indexAtt != -1)
        {
          if (level == XML2CSVOptimization.NONE)
          {
            // The attribute is written in a new line at its correct index in the CSV output file (a CSV line with
            // trackedLeafElementXPaths.length fields). Then, the line is immediately sent to the CSV output file.
            String line = buildRawCSVOutputLine(indexAtt, trackedLeafElementsDescription.getElementsXPaths().length, att[1]);
            emit(line);
            ls(); // Next line.
          }
          else
          {
            // The attribute is buffered.
            dataBuffer.addContent(indexAtt, att[1]);
          }
        }
        else
        {
          // The attribute does not belong to the list of tracked element attributes (index = -1) so it should be discarded.
          // We do nothing.
        }
      }
      attsList.clear();
    }
  }

  // ================================================================================
  // Public/protected getters and setters
  // ================================================================================

  /**
   * Resets this data handler for further use.
   */
  public void reset()
  {
    rootTag = null;
    textBuffer = null;
    dataBuffer.reset();
    withAttributes = false;
    withNamespaces = false;
    unleashed = false;
  }
}
