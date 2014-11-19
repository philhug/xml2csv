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

/**
 * Technical notes.<br>
 * Default parse behavior is to add the current XML element reference at the end of the relevant list in the corresponding tree node when it does not exist yet.<br>
 * This said, an additional behavior is coded in order to handle optional elements not present right from the beginning of the XML file which might be misplaced in the graph
 * if the default behavior only applies.<br>
 * This special additional behavior consists in keeping track of the previous XML element parsed "of the same depth in the same parent block" (see [-A-]) as well as
 * the current one and, should the previous element be misplaced in the graph, to put it back to its right place.<br>
 * For instance: if 2 elements Path.To.Element and Path.To.AnotherElement are parsed then the graph will generate a node {Path} containing a node {To} containing
 * 2 nodes {Element} and {AnotherElement} in that order if they do not exist yet.<br>
 * However, imagine that later on in the XML file an intermediary Path.To.AnotherOne element pops up between Path.To.Element and Path.To.AnotherElement, then:<br>
 * <ol>
 * <li>when Path.To.AnotherOne is parsed then a node {AnotherOne} is added in {To} after {Element} and {AnotherElement} (default behavior, misplacing {AnotherOne});
 * <li>when Path.To.AnotherElement is parsed the node {AnotherElement} already exists so it is not recreated but {AnotherOne} is put back before {AnotherElement}.
 * </ol>
 */

import utils.xml.xml2csv.constants.XML2CSVCardinality;
import utils.xml.xml2csv.constants.XML2CSVLogLevel;
import utils.xml.xml2csv.constants.XML2CSVNature;
import utils.xml.xml2csv.constants.XML2CSVOptimization;
import utils.xml.xml2csv.constants.XML2CSVType;
import utils.xml.xml2csv.constants.XML2CSVMisc;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Iterator;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.ext.Locator2;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This SAX handler implementation parses an XML file in order to build an in-memory representation of its structure in a graph.<br>
 * The data contained in the XML file is not relevant at this stage, only the structure matters.<br>
 * Extra properties (cardinality, type, ...) are recorded for each element alongside parsing.<br>
 * When the end of the XML file is reached the actual ordered XPath list of the leaf elements is build from a recursive access to the graph.<br>
 * The parsing result is available through a {@link utils.xml.xml2csv.StructureHandler#getLeafElementsDescription() getLeafElementsDescription} call which returns an
 * {@link utils.xml.xml2csv.ElementsDescription ElementsDescription} bean instance.
 * @author L. Popieul (lochrann@rocketmail.com)
 */
class StructureHandler extends DefaultHandler implements LexicalHandler
{
  // The character encoding of the XML input file.
  // private String encoding = null;

  /** The root tag of the XML input file. */
  private String rootTag = null;

  /** Some particular SAX extension class to get access to the XML input encoding. */
  private Locator2 locator = null;

  /** The top level graph element (will contain the XML root element and all sub structure elements). */
  private LinkedHashMap<String, Object> graph = new LinkedHashMap<String, Object>();

  /**
   * Properties of each element (cardinality, type) recorded in a hash map where the key is The.Element.Path and the value a String array. Recorded properties: cardinality
   * (index 0), type (index 1), global occurrence count (index 2).
   */
  private HashMap<String, String[]> properties = new HashMap<String, String[]>();

  /**
   * Attributes of each element recorded in a hash map where the key is The.Element.Path and the value an ordered list of String arrays providing each: attribute name
   * (index 0), cardinality (index 1), type (index 2).
   */
  private HashMap<String, ArrayList<String[]>> attributes = new HashMap<String, ArrayList<String[]>>();

  /** Name spaces met where the key is the name space alias and the value the name space full name. */
  private HashMap<String, String> namespaces = new HashMap<String, String>();

  /** Ordered list of the currently parsed XML tag sequence (that is, for &lt;A&gt;&lt;B&gt;&lt;C2&gt;, {A,B,C2}). */
  private ArrayList<String> currentXMLTagSequence = new ArrayList<String>();

  /**
   * Ordered list of the previously parsed XML tag sequence for each XML depth (Example: for &lt;A&gt;&lt;B2&gt;&lt;C1&gt;, {A,B2,C1} at index 2 and for &lt;A&gt;&lt;B2&gt;, {A,B2}
   * at index 1).
   */
  private ArrayList<ArrayList<String>> previousXMLTagSequencePerDepth = new ArrayList<ArrayList<String>>();

  /**
   * Ordered list of the XML children element short names actually met/parsed within a parent block for each parent XML depth (Example: for
   * &lt;A&gt;&lt;B&gt;&lt;C1&gt;&lt;/C1&gt;&lt;C2&gt;&lt;/C2&gt;&lt;/B&gt;&lt;/A&gt;, {C1,C2} at index 1).
   */
  private ArrayList<ArrayList<String>> childrenElementShortNamesPerParentDepth = new ArrayList<ArrayList<String>>();

  /**
   * The actual XML parsing result. Provides the ordered XPath list of the XML elements defined in this XML file (for instance: Root.Headers.Header, Root.Row.Date,
   * Root.Row.Data.Amount1, Root.Row.Data.Amount2, ...).
   */
  private ArrayList<String> flatLeafElementXPathList = new ArrayList<String>();

  /** Low level string buffer which records element contents alongside parsing. */
  private StringBuffer textBuffer = null;

  /** Parser adapted for XML dates. */
  private SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");

  /** Parser adapted for XML dates. */
  private SimpleDateFormat datetimeParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

  /** Parser adapted for XML times. */
  private SimpleDateFormat timeParser = new SimpleDateFormat("HH:mm:ss.SSS");

  /** Parser adapted for XML integers. */
  private DecimalFormat integerParser = new DecimalFormat("##################");

  /** Parser adapted for XML decimals. */
  private DecimalFormat decimalParser = new DecimalFormat("##########.#######");

  /** The chosen optimization level. */
  private XML2CSVOptimization level = null;

  /** Attributes extraction indicator. */
  private boolean withAttributes = false;

  /** Name space awareness indicator. */
  private boolean withNamespaces = false;

  /**
   * <code>StructureHandler</code> constructor.
   * @param level the chosen <code>XML2CSVOptimization</code> level.
   * @param withAttributes <code>true</code> if element attributes should be extracted as well or <code>false</code> otherwise.
   * @param withNamespaces <code>true</code> if name space aware parsing should be performed or <code>false</code> otherwise.
   */
  public StructureHandler(XML2CSVOptimization level, boolean withAttributes, boolean withNamespaces)
  {
    super();
    this.level = level;
    this.withAttributes = withAttributes;
    this.withNamespaces = withNamespaces;
  }

  // ================================================================================
  // ContentHandler method implementation
  // ================================================================================

  @Override
  public void startDocument() throws SAXException
  {
    if (locator != null)
    {
      // encoding = locator.getEncoding();
    }
  }

  @Override
  public void endDocument() throws SAXException
  {
    try
    {
      // In the most extensive optimization mode only, virtual hidden attribute definitions are added to certain elements.
      // They serve as aggregation catalysts which maximize optimization without major algorithm change.
      if (level == XML2CSVOptimization.EXTENSIVE_V3)
      {
        ArrayList<String> catalysElementXpaths = new ArrayList<String>();
        deviseCatalystElementXPaths(XML2CSVMisc.EMPTY_STRING, graph, catalysElementXpaths);
        if (XML2CSVLoggingFacade.VERBOSE_MODE == true)
        {
          if (catalysElementXpaths.size() > 0)
          {
            StringBuffer sb = new StringBuffer();
            sb.append("the following elements will be provided hidden virtual attributes for optimization maximization:" + XML2CSVMisc.LINE_SEPARATOR);
            for (int i = 0; i < catalysElementXpaths.size() - 1; i++)
              sb.append("\t- <" + catalysElementXpaths.get(i) + ">" + XML2CSVMisc.LINE_SEPARATOR);
            sb.append("\t- <" + catalysElementXpaths.get(catalysElementXpaths.size() - 1) + ">");
            XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, sb.toString());
          }
          else
            XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, "no candidate elements detected for optimization maximization. No hidden virtual attributes created.");
        }
        for (int i = 0; i < catalysElementXpaths.size(); i++)
          generateCatalystAttribute(catalysElementXpaths.get(i));
      }
      // XML structure finalization. The flat XML leaf element list is generated from the actual tree representation.
      flattenGraph(XML2CSVMisc.EMPTY_STRING, graph);
    }
    catch (Exception e)
    {
      throw new SAXException("I/O error", e);
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

    // Name space collation, if space full parsing is performed.
    if (withNamespaces)
    {
      if ((uri != null) && (uri.trim().isEmpty() == false) && (qName != null) && (qName.trim().isEmpty() == false))
      {
        String alias = null;
        if (qName.indexOf(":") != -1) alias = qName.substring(0, qName.indexOf(":"));
        else
          alias = XML2CSVMisc.DEFAULT_NAMESPACE_ALIAS;
        namespaces.put(alias, uri);
      }
    }

    // Current element contents buffer reset before the element is started.
    textBuffer = null;

    // Adds the current tag to the graph if is is not yet recorded.
    updateGraph();

    // Adds default tag properties if not yet defined.
    updateProperties();

    // Records the element attributes if any and if expected, with appropriate tag properties.
    if ((withAttributes == true) && (atts != null) && (atts.getLength() != 0)) updateAttributeProperties(atts);

    // Root element detection.
    if (rootTag == null) rootTag = eName;
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException
  {
    // This handler is interested in the XML structure only and element contents are read in order to guess the element type only.
    updateElementType(getText(true));

    // Compares the current XML element and the previous one and replaces the previous one at its right position in the graph if it is misplaced.
    testGraphForMisplacedElement();

    // [-A-]: track of the previous XML element parsed at the same depth in the same parent block.
    // Records the currently closing XML element as the new previous element of the corresponding depth (example: The.Previous.Element goes to index 2),
    // and voids all previous elements of deeper depth alongside (= forgets the previous occurrence of the parent block).
    // Useful later on to have misplaced elements in the graph (because they were optional and added lately) put back at their right place.
    recordPreviousElement();

    // [-B-]: track of the current XML element short name associated with the parent block at the parent's block depth (D-1 if D is the depth of the currently closing
    // XML element). Adds the currently closing XML element to the list at depth D-1, and if it is already present, marks it as unbounded.
    // In addition, and because the elements listed at D are precisely those which were actually met inside the currently closing XML element, comparison with the elements
    // officially listed in the graph: all graph elements not present in the list of depth D are immediately marked as optional.
    // At last, all records of depth >=D are voided to avoid to keep old data in memory.
    // All this put together makes it possible to detect optional absent elements which do not actually trigger anything when they are not present and have
    // to be handled with explicit comparisons.
    detectElementsCardinality();

    // Removes the current tag from the current tag list (element closure).
    currentXMLTagSequence.remove(currentXMLTagSequence.size() - 1);
  }

  @Override
  public void characters(char buf[], int offset, int len) throws SAXException
  {
    // Current element contents, or garbage characters (such as blanks) between regular tag.
    // This handler is interested in the XML structure only and element contents are read in order to guess the element type.
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
     * endingElementLeft = false; String s = "<![CDATA["; if (textBuffer == null) { textBuffer = new StringBuffer(s); } else { textBuffer.append(s); }
     */
  }

  @Override
  public void endCDATA() throws SAXException
  {
    // System.out.println("endCDATA");
    /**
     * endingElementLeft = false; String s = "]]>"; if (textBuffer == null) { textBuffer = new StringBuffer(s); } else { textBuffer.append(s); }
     */
  }

  @Override
  public void comment(char[] ch, int start, int length) throws SAXException
  {
    // endingElementLeft = false;
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
   * Takes the XPath tag sequence of the currently parsed XML element and creates related graph nodes if they don't exist yet.
   */
  @SuppressWarnings("unchecked")
  private void updateGraph()
  {
    LinkedHashMap<String, Object> pointer = graph;

    for (int i = 0; i < currentXMLTagSequence.size(); i++)
    {
      String oneTag = currentXMLTagSequence.get(i);
      LinkedHashMap<String, Object> temp = (LinkedHashMap<String, Object>) pointer.get(oneTag);
      if (temp == null) pointer.put(oneTag, new LinkedHashMap<String, Object>());
      pointer = (LinkedHashMap<String, Object>) pointer.get(oneTag);
    }
  }

  /**
   * Takes the XPath tag sequence of the currently parsed XML element and initializes the element properties if they don't exist yet.<br>
   * Default element properties are: <code>XML2CSVCardinality.ONE_TO_ONE</code> (mandatory single element), <code>XML2CSVType.UNKNOWN</code>, and <code>0</code> global occurrence
   * count.
   */
  private void updateProperties()
  {
    String xpath = getXPathAsString(currentXMLTagSequence);

    if (properties.containsKey(xpath) == false)
    {
      String[] props = new String[3];
      // By default, a new element is supposed mandatory and mono-occurrence and might be set to optional and/or unbounded later on.
      props[0] = XML2CSVCardinality.ONE_TO_ONE.getCode();
      // By default, a new element is of UNKNOWN type, which will be narrowed later on for something more accurate for a leaf element.
      props[1] = XML2CSVType.UNKNOWN.getCode();
      // The global element occurrence count is initialized to 0 for a new element and will be incremented by 1 for each occurrence in the template file when the element is closed.
      props[2] = Long.toString(0);
      properties.put(xpath, props);
    }
  }

  /**
   * Explores the element graph in order to devise a resulting list of elements which should be provided a virtual attribute.<br>
   * Candidate elements are the <i>intermediate</i> elements under the root element (excluded) which:
   * <ul>
   * <li>either are repeated "<i>enough</i>" themselves through the whole template file (conventionally: at least
   * {@link utils.xml.xml2csv.constants.XML2CSVMisc#ELEMENT_REPETITION_THRESHOLD this} occurrence count or above), or
   * <li>are mono-occurrence elements or multi-occurrence elements not repeated "<i>enough</i>", from which all non-leaf descendant elements are either single or not repeated
   * "<i>enough</i>" themselves through the whole template file.
   * </ul>
   * The search ends when candidates have been found at all graph depths (deeper depths are searched too).<br>
   * A virtual attribute serves as aggregation catalyst which maximizes {@link utils.xml.xml2csv.constants.XML2CSVOptimization#EXTENSIVE_V2 EXTENSIVE_V2} optimization into a
   * {@link utils.xml.xml2csv.constants.XML2CSVOptimization#EXTENSIVE_V3 EXTENSIVE_V3} one without major algorithm change.<br>
   * Recursive method.
   * @param currentXpath the current XPath associated with the graph part.
   * @param currentGraph the current graph part to explore.
   * @param result the XPath list of elements which should get a virtual attribute, enriched while the graph is searched.
   * @throws SAXException in case of error.
   */
  @SuppressWarnings("unchecked")
  private void deviseCatalystElementXPaths(String currentXpath, LinkedHashMap<String, Object> currentGraph, ArrayList<String> result) throws SAXException
  {
    Set<String> keySet = currentGraph.keySet();
    if (keySet.size() == 0)
    {
      // Bottom graph leaf element reached. Leaf elements are never good candidates for virtual attributes. We do nothing.
    }
    else
    {
      // Intermediate graph element reached.
      boolean candidate = false;
      String[] props = properties.get(currentXpath);
      if (props != null)
      {
        // If the element is repeated "enough" it is added to the candidate list.
        long globalElementCount = Long.parseLong(props[2]);
        if (globalElementCount >= XML2CSVMisc.ELEMENT_REPETITION_THRESHOLD)
        {
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "deviseCatalystElementXPaths: element <" + currentXpath + "> is a primary candidate for optimization maximization.");
          candidate = true;
        }
        else
        {
          // If the element is not repeated "enough" its descendants are examined and if none of them are repeated "enough" themselves the element is added to the candidate list.
          Iterator<String> iterator = keySet.iterator();
          if (iterator.hasNext()) candidate = true;
          while (iterator.hasNext())
          {
            String node = iterator.next();
            String oneChildXpath = null;
            if (!currentXpath.equals(XML2CSVMisc.EMPTY_STRING)) oneChildXpath = currentXpath + "." + node;
            else
              oneChildXpath = node;
            LinkedHashMap<String, Object> nextGraph = (LinkedHashMap<String, Object>) currentGraph.get(node);
            candidate = candidate && examineChildrenRepetition(oneChildXpath, oneChildXpath, nextGraph);
            if (candidate == false)
            {
              XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "deviseCatalystElementXPaths: element <" + currentXpath
                  + "> cannot be a secondary candidate for optimization maximization.");
              break;
            }
          }
        }
        if (currentXpath.indexOf(".") == -1) candidate = false; // The root element is excluded (only XPath without ".") to avoid edge effects.
        if (candidate == true)
        {
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "deviseCatalystElementXPaths: element <" + currentXpath + "> is a secondary candidate for optimization maximization.");
          result.add(currentXpath);
        }
      }
      else
      {
        if (currentXpath.isEmpty() == false)
        {
          // Cannot happen unless there is a bug somewhere => SAXException raised immediately in order to abort everything.
          throw new SAXException("Internal parsing failure. Missing properties for element <" + currentXpath + ">. Location: method <deviseCatalystElementXPaths> #1");
        }
        else
        {
          // Edge effect at the beginning of the recursive graph analysis, which starts with an empty currentXpath. We do nothing.
        }
      }
      // The nested elements of the intermediate element are searched too, no matter if the current element was a candidate or not.
      // if (candidate == false)
      {
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext())
        {
          String node = iterator.next();
          String nextXpath = null;
          if (!currentXpath.equals(XML2CSVMisc.EMPTY_STRING)) nextXpath = currentXpath + "." + node;
          else
            nextXpath = node;
          LinkedHashMap<String, Object> nextGraph = (LinkedHashMap<String, Object>) currentGraph.get(node);
          deviseCatalystElementXPaths(nextXpath, nextGraph, result);
        }
      }
    }
  }

  /**
   * Examines all the intermediate descendant elements of the element associated with the <code>initialXPath</code> parameter and returns <code>true</code> if none of them are
   * repeated "<i>enough</i>" (convention: at least {@link utils.xml.xml2csv.constants.XML2CSVMisc#ELEMENT_REPETITION_THRESHOLD this} occurrence count) or <code>false</code>
   * otherwise.<br>
   * Recursive method.
   * @param initialXPath the initial XPath associated with the starting graph part.
   * @param currentXpath the current XPath associated with the graph part.
   * @param currentGraph the current graph part to explore.
   * @return <code>true</code> if all the descendant elements of the initial XPath are not repeated "<i>enough</i>" and <code>false</code> otherwise.
   * @throws SAXException in case of error.
   */
  @SuppressWarnings("unchecked")
  private boolean examineChildrenRepetition(String initialXPath, String currentXpath, LinkedHashMap<String, Object> currentGraph) throws SAXException
  {
    boolean result = true;
    Set<String> keySet = currentGraph.keySet();
    if (keySet.size() == 0)
    {
      // Bottom graph leaf element reached. Leaf elements are not concerned by the examination.
    }
    else
    {
      // Intermediate graph element reached.
      String[] props = properties.get(currentXpath);
      if (props != null)
      {
        // If the element is repeated "enough" it is added to the candidate list.
        long globalElementCount = Long.parseLong(props[2]);
        if (globalElementCount >= XML2CSVMisc.ELEMENT_REPETITION_THRESHOLD)
        {
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG2, "examineChildrenRepetition: element <" + currentXpath + "> prevents its ancestor <" + initialXPath
              + "> from being a secondary candidate for optimization maximization.");
          result = false;
        }
        else
        {
          Iterator<String> iterator = keySet.iterator();
          while (iterator.hasNext())
          {
            String node = iterator.next();
            String nextXpath = null;
            if (!currentXpath.equals(XML2CSVMisc.EMPTY_STRING)) nextXpath = currentXpath + "." + node;
            else
              nextXpath = node;
            LinkedHashMap<String, Object> nextGraph = (LinkedHashMap<String, Object>) currentGraph.get(node);
            result = result && examineChildrenRepetition(initialXPath, nextXpath, nextGraph);
          }
        }
      }
      else
      {
        // Cannot happen unless there is a bug somewhere => SAXException raised immediately in order to abort everything.
        throw new SAXException("Internal parsing failure. Missing properties for element <" + currentXpath + ">. Location: method <examineChildren> #1");
      }
    }
    return result;
  }

  /**
   * Creates a virtual hidden attribute definition for the element associated with the XPath provided in parameter.<br>
   * A virtual attribute serves as aggregation catalyst which maximizes {@link utils.xml.xml2csv.constants.XML2CSVOptimization#EXTENSIVE_V2 EXTENSIVE_V2} optimization into a
   * {@link utils.xml.xml2csv.constants.XML2CSVOptimization#EXTENSIVE_V3 EXTENSIVE_V3} one without major algorithm change.
   * @param xpath the XPath of the element which will be provided a virtual attribute.
   */
  private void generateCatalystAttribute(String xpath)
  {
    if (attributes.containsKey(xpath) == false)
    {
      // The element has no attribute list.
      ArrayList<String[]> attsList = new ArrayList<String[]>();
      String[] att = new String[3];
      att[0] = XML2CSVMisc.VIRTUAL_ATTRIBUTE;
      // Virtual attributes have neither cardinality nor type but are provided default values to avoid edge effects.
      att[1] = XML2CSVCardinality.ONE_TO_ONE.getCode();
      att[2] = XML2CSVType.STRING.getCode();
      attsList.add(att);
      attributes.put(xpath, attsList);
    }
    else
    {
      // The element has already an attribute definition list.
      ArrayList<String[]> attsList = attributes.get(xpath);
      int idxOfAtt = -1;
      for (int j = 0; j < attsList.size(); j++)
      {
        if (attsList.get(j)[0].equals(XML2CSVMisc.VIRTUAL_ATTRIBUTE))
        {
          idxOfAtt = j;
          break;
        }
      }
      if (idxOfAtt == -1)
      {
        // The element has already an attribute definition list but no virtual attribute yet. We add one.
        String[] att = new String[3];
        att[0] = XML2CSVMisc.VIRTUAL_ATTRIBUTE;
        // Virtual attributes have neither cardinality nor type but are provided default values to avoid edge effects.
        att[1] = XML2CSVCardinality.ONE_TO_ONE.getCode();
        att[2] = XML2CSVType.STRING.getCode();
        attsList.add(0, att); // The virtual attribute is placed by default on the top of the attribute list.
      }
      else
      {
        // The virtual attribute has already been added to the element's attribute list. We do nothing.
      }
    }
  }

  /**
   * Updates the attribute list of the currently parsed element.
   * @param atts the attributes of the currently parsed element.
   */
  private void updateAttributeProperties(Attributes atts)
  {
    String xpath = getXPathAsString(currentXMLTagSequence);

    if (attributes.containsKey(xpath) == false)
    {
      // It's the first time the currently parsed element got attributes.
      ArrayList<String[]> attsList = new ArrayList<String[]>();
      for (int i = 0; i < atts.getLength(); i++)
      {
        String[] att = new String[3];
        att[0] = atts.getLocalName(i);
        // By default, a new attribute is supposed mandatory and mono-occurrence and might be set to optional later on.
        att[1] = XML2CSVCardinality.ONE_TO_ONE.getCode();
        // The type of the new attribute is guessed from its value.
        att[2] = checkType(atts.getValue(i), typeSniffer(atts.getValue(i))).getCode();
        attsList.add(att);
      }
      attributes.put(xpath, attsList);
    }
    else
    {
      // We have already met attributes for the currently parsed element. It has already an attribute definition list, which we examine now.
      ArrayList<String[]> attsList = attributes.get(xpath);
      int idxOfPrevAtt = -1; // Helps to insert a new attribute at its correct place in the list.
      for (int i = 0; i < atts.getLength(); i++)
      {
        // We look for the attribute in the list associated with the element.
        String attName = atts.getLocalName(i);
        int idxOfAtt = -1;
        for (int j = 0; j < attsList.size(); j++)
        {
          if (attsList.get(j)[0].equals(attName))
          {
            idxOfAtt = j;
            break;
          }
        }
        if (idxOfAtt != -1)
        {
          // The type of the attribute is updated appropriately.
          String[] att = attsList.get(idxOfAtt);
          att[2] = checkType(atts.getValue(i), XML2CSVType.parse(att[2])).getCode();
          attsList.set(idxOfAtt, att);
          // All the attributes between idxOfPrevAtt and idxOfAtt, if any, were not provided for the current element occurrence and should be set to optional.
          for (int k = idxOfPrevAtt + 1; k < idxOfAtt; k++)
          {
            String[] oneAtt = attsList.get(k);
            oneAtt[1] = XML2CSVCardinality.ZERO_TO_ONE.getCode();
            attsList.set(k, oneAtt);
          }
          // The value of idxOfPrevAtt is updated and will help dealing with the next attribute.
          idxOfPrevAtt = idxOfAtt;
        }
        else
        {
          // The attribute does not exist in the list yet. We're dealing with an optional attribute popping up from nowhere.
          // We add it at the correct position in the attribute list with a optional cardinality and an appropriate type.
          String[] att = new String[3];
          att[0] = attName;
          att[1] = XML2CSVCardinality.ZERO_TO_ONE.getCode();
          att[2] = checkType(atts.getValue(i), typeSniffer(atts.getValue(i))).getCode();
          if (idxOfPrevAtt == -1) attsList.add(0, att); // The optional new attribute is the first and is placed accordingly.
          else
          {
            // The optional new attribute is placed just after the previous attribute (idxOfPrevAtt) or, if the previous attribute
            // was the last at the end of the list.
            if (idxOfPrevAtt < attsList.size())
            {
              attsList.add(idxOfPrevAtt + 1, att);
              idxOfPrevAtt = idxOfPrevAtt + 1;
            }
            else
            {
              attsList.add(att);
              idxOfPrevAtt = attsList.size();
            }
          }
        }
      }
      attributes.put(xpath, attsList);
    }
  }

  /**
   * Detects optional elements when an element is closed as well as multi-occurrence elements.<br>
   * The detection is based on the recording for each parent element's XPath depth of the short names of the sub elements met/parsed, and comparison with the graph which serves
   * as a reference.
   */
  @SuppressWarnings("unchecked")
  private void detectElementsCardinality()
  {
    ArrayList<String> childrenElementShortNamesOfParent = null;
    if (childrenElementShortNamesPerParentDepth.size() < (currentXMLTagSequence.size() + 1))
    {
      int delta = currentXMLTagSequence.size() + 1 - childrenElementShortNamesPerParentDepth.size();
      for (int j = 0; j <= delta; j++)
        childrenElementShortNamesPerParentDepth.add(null);
    }

    // The root tag has no parent but the element at index 0 will record it; the root tag's children will be listed at index 1 (root's depth), and so on.
    childrenElementShortNamesOfParent = childrenElementShortNamesPerParentDepth.get(currentXMLTagSequence.size() - 1);
    if (childrenElementShortNamesOfParent == null) childrenElementShortNamesOfParent = new ArrayList<String>();

    // The current closing element short name is added to the list at index {current element's depth -1} which is the current element's parent depth.
    // By the way, if the current closing element short name already exists in that list it means this is a multi-occurrence element.
    String shortNameOfCurrentElement = currentXMLTagSequence.get(currentXMLTagSequence.size() - 1);
    boolean alreadyThere = false;
    for (int i = 0; i < childrenElementShortNamesOfParent.size(); i++)
    {
      String oneShortName = childrenElementShortNamesOfParent.get(i);
      if (oneShortName.equals(shortNameOfCurrentElement))
      {
        alreadyThere = true;
        break;
      }
    }
    if (alreadyThere == true)
    {
      // OK: already there. The current element is definitely a multi-occurrence element in its parent block.
      String[] props = properties.get(getXPathAsString(currentXMLTagSequence)); // Exists since startElement occurs before endElement.
      if (XML2CSVCardinality.isUnbounded(props[0]) == false)
      {
        props[0] = XML2CSVCardinality.setToUnbounded(props[0]);
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG2, "detectElementsCardinality: cardinality of element <" + getXPathAsString(currentXMLTagSequence) + "> set to unbounded.");
      }
      // We don't forget to increase the global element count.
      props[2] = Long.toString(Long.parseLong(props[2]) + 1L);
      properties.put(getXPathAsString(currentXMLTagSequence), props); // Useless instruction, but anyway.
    }
    else
    {
      // OK: not already there. We add it.
      childrenElementShortNamesOfParent.add(shortNameOfCurrentElement);
      childrenElementShortNamesPerParentDepth.set(currentXMLTagSequence.size() - 1, childrenElementShortNamesOfParent);
      // We don't forget to increase the global element count.
      String[] props = properties.get(getXPathAsString(currentXMLTagSequence)); // Exists since startElement occurs before endElement.
      props[2] = Long.toString(Long.parseLong(props[2]) + 1L);
      properties.put(getXPathAsString(currentXMLTagSequence), props); // Useless instruction, but anyway.
    }

    // The element short name list at index {current element's depth} lists the direct children elements which were actually met in the current XML element block.
    // It's time to compare them with the list of the current's elements children as recorded in the graph and to mark any element present in the graph but absent
    // in the short name list as optional.
    LinkedHashMap<String, Object> pointer = graph;
    for (int i = 0; i <= currentXMLTagSequence.size() - 1; i++)
    {
      String oneTag = currentXMLTagSequence.get(i);
      pointer = (LinkedHashMap<String, Object>) pointer.get(oneTag);
    }
    // Now pointer points to the graph's node N representing the contents of the current XML element.
    String[] temp = new String[pointer.keySet().size()];
    String[] childrenElementShortNamesOfCurrentElementInGraph = pointer.keySet().toArray(temp);

    // Any element listed in childrenElementShortNamesOfCurrentElementInGraph but not present in childrenElementShortNamesOfCurrentElement should be marked as optional.
    ArrayList<String> childrenElementShortNamesOfCurrentElement = childrenElementShortNamesPerParentDepth.get(currentXMLTagSequence.size());
    if (childrenElementShortNamesOfCurrentElement != null) // Null for all leaf elements.
    {
      for (int i = 0; i < childrenElementShortNamesOfCurrentElementInGraph.length; i++)
      {
        String oneChildShortNameInGraph = childrenElementShortNamesOfCurrentElementInGraph[i];
        boolean found = false;
        for (int j = 0; j < childrenElementShortNamesOfCurrentElement.size(); j++)
        {
          if (childrenElementShortNamesOfCurrentElement.get(j).equals(oneChildShortNameInGraph))
          {
            found = true;
            break;
          }
        }
        if (found == false)
        {
          // OK: oneChildShortNameInGraph should be marked as optional. Being a child of the current element its complete XPath is obtained with an appropriate concatenation.
          String xpath = getXPathAsString(currentXMLTagSequence) + "." + oneChildShortNameInGraph;
          String[] props = properties.get(xpath);
          if (XML2CSVCardinality.isOptional(props[0]) == false)
          {
            props[0] = XML2CSVCardinality.setToOptional(props[0]);
            properties.put(xpath, props); // Useless instruction, but anyway.
            XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG2, "detectElementsCardinality: cardinality of element <" + xpath + "> set to optional");
          }
        }
      }
    }

    // All records after index {current element's depth} inclusive are voided.
    if (childrenElementShortNamesPerParentDepth.size() > currentXMLTagSequence.size())
    {
      int delta = childrenElementShortNamesPerParentDepth.size() - currentXMLTagSequence.size();
      for (int j = 0; j < delta; j++)
        childrenElementShortNamesPerParentDepth.set(currentXMLTagSequence.size() + j, null);
    }
  }

  /**
   * Records the currently closing XML element as the new previous element of the corresponding depth and clears all previous elements of deeper depth to avoid edge effects (=
   * with the previous occurrence of the parent block).
   */
  private void recordPreviousElement()
  {
    ArrayList<String> previousXMLTagSequence = null;
    if (previousXMLTagSequencePerDepth.size() < currentXMLTagSequence.size())
    {
      int delta = currentXMLTagSequence.size() - previousXMLTagSequencePerDepth.size();
      for (int j = 0; j < delta; j++)
        previousXMLTagSequencePerDepth.add(null);
    }
    // Root tag (depth 1) is recorded at index 0, and so on.
    previousXMLTagSequence = previousXMLTagSequencePerDepth.get(currentXMLTagSequence.size() - 1);
    if (previousXMLTagSequence == null) previousXMLTagSequence = new ArrayList<String>();

    // The current closing element is recorded as the new previous element of the corresponding depth.
    previousXMLTagSequence.clear();
    previousXMLTagSequence.addAll(currentXMLTagSequence);
    previousXMLTagSequencePerDepth.set(currentXMLTagSequence.size() - 1, previousXMLTagSequence);

    // Previous elements of deeper depth are discarded to avoid edge effects.
    // If not imagine:
    // <Row>
    // <Data1>12</Data1>
    // <Data2>34</Data2> [-a-]
    // </Row> [-c-]
    // <Row>
    // <Data1>56</Data1> [-b-]
    // <Data2>78</Data2>
    // </Row>
    // Then when Row.Data1 [-b-] is met Row.Data2 [-a-], still marked as [-b-]'s previous element, will trigger a swap between Data1 and Data2 in Row in the graph
    // placing Data2 before Data1 which is not what we want. We ensure that when [-c-] is closed [-a-] is cleared/forgotten.
    if (previousXMLTagSequencePerDepth.size() > currentXMLTagSequence.size())
    {
      int delta = previousXMLTagSequencePerDepth.size() - currentXMLTagSequence.size();
      for (int j = 0; j < delta; j++)
        previousXMLTagSequencePerDepth.set(currentXMLTagSequence.size() + j, null);
    }
  }

  /**
   * Controls that the current XML element and the previous one of the same depth are at the right relative position in the graph if they are not identical,<br>
   * and if the position is wrong moves the node corresponding to the previous XML element before the node corresponding to the current XML element.
   * @throws SAXException in case of error.
   */
  @SuppressWarnings("unchecked")
  private void testGraphForMisplacedElement() throws SAXException
  {
    LinkedHashMap<String, Object> pointer = graph;
    LinkedHashMap<String, Object> previousPointer = null;

    ArrayList<String> previousXMLTagSequence = null;
    if (previousXMLTagSequencePerDepth.size() < currentXMLTagSequence.size())
    {
      int delta = currentXMLTagSequence.size() - previousXMLTagSequencePerDepth.size();
      for (int j = 0; j < delta; j++)
        previousXMLTagSequencePerDepth.add(null);
    }
    // Root tag (depth 1) is recorded at index 0, and so on.
    previousXMLTagSequence = previousXMLTagSequencePerDepth.get(currentXMLTagSequence.size() - 1); // We consider the previous element of the same depth as the current element.

    // ArrayList previousXMLTagSequence nests the previous element parsed of the same depth N as currentXMLTagSequence (that is: of depth currentXMLTagSequence.size()),
    // recorded at index N in previousXMLTagSequencePerDepth.
    // Depth N means that previousXMLTagSequence and currentXMLTagSequence both have N tags (example: Path.To.Element has 3 tags).
    boolean performTest = true;
    if (previousXMLTagSequence == null)
    {
      // ArrayList previousXMLTagSequence is null if there is no previous element of the same depth closed/recorded yet in the same parent block
      // (might occur if the current element is the 1st one parsed of that depth in the same parent block) and if so the test is aborted
      // (there is nothing to compare with the current element).
      performTest = false;
    }
    else
    {
      if (getXPathAsString(currentXMLTagSequence).equals(getXPathAsString(previousXMLTagSequence)))
      {
        // The tag sequence of the previous element is exactly the same as the current tag sequence of the current element.
        // The previous element and the current one are the same (we are facing a multi-occurrence element in sequence).
        // The test is aborted because the previous element and the current one will point to the same node in the graph.
        // However, the element properties are updated in order to reflect the actual element cardinality.
        performTest = false;
        String[] props = properties.get(getXPathAsString(currentXMLTagSequence)); // Exists since startElement occurs before endElement.
        if (XML2CSVCardinality.isUnbounded(props[0]) == false)
        {
          props[0] = XML2CSVCardinality.setToUnbounded(props[0]);
          properties.put(getXPathAsString(currentXMLTagSequence), props); // Useless instruction, but anyway.
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG2, "testGraphForMisplacedElement: cardinality of element <" + getXPathAsString(currentXMLTagSequence)
              + "> set to unbounded");
        }
      }
      else if (getParentXPathAsString(currentXMLTagSequence) == null)
      {
        // The current element has no parent, which means the current element is the root tag (being closed).
        // There is only one root element in an XML file which has no previous element of the same depth (definition of the root element!) and the test is pointless.
        performTest = false;
      }
      else if (getParentXPathAsString(previousXMLTagSequence) == null)
      {
        // The previous element of the same depth as the current element has no parent while the current one has one.
        // Cannot happen unless there is a bug somewhere => SAXException raised immediately in order to abort everything.
        throw new SAXException("Internal parsing failure. Location: method <testGraphForMisplacedElement> #1");
      }
      else if (getParentXPathAsString(currentXMLTagSequence).equals(getParentXPathAsString(previousXMLTagSequence)) == false)
      {
        // The parent element of the current element and the parent element of the previous element of the same depth do not match
        // (at least one tag in the related parent XPath is not the same) which means that the current element and the previous element of the
        // same depth are not related and that the test is pointless.
        performTest = false;
      }
    }

    // If performTest is true then the comparison makes sense, which, technically, means that we won't hit an ugly runtime NullPointerException or IndexOutOfBoundsException.
    if (performTest == true)
    {
      // All the tags of previousXMLTagSequence and currentXMLTagSequence match but for the last one, which are different (see previous multi-occurrence test).
      // It's time to compare the position of the last tags of previousXMLTagSequence and currentXMLTagSequence in the graph and if it happens that the last tag of
      // previousXMLTagSequence is misplaced in the corresponding node then it is moved before the node corresponding to the last tag of currentXMLTagSequence.
      // The node corresponding to the last tag of currentXMLTagSequence exists in the graph because of the updateGraph() call in startElement method.
      // The node corresponding to the last tag of previousXMLTagSequence exists in the graph too because of the updateGraph() call in startElement method
      // when the previous element was started.
      for (int i = 0; i < currentXMLTagSequence.size() - 1; i++) // The current element has a depth >=2 (root element case leads to performTest)
      {
        String oneTag = currentXMLTagSequence.get(i);
        previousPointer = pointer;
        pointer = (LinkedHashMap<String, Object>) pointer.get(oneTag);
      }
      // Here:
      // - pointer points to the graph's node N representing the contents of the parent element of both the current XML element and the previous one.
      // - this node N is recorded as the content of the node pointed by previousPointer under the key corresponding to the last tag of the parent element.
      // Example: with Root.Row.Data.Elt1 and Root.Row.Data.Elt2: previousPointer.get("Data")=N=pointer to the node whose keys are {"Elt1","Elt2"}.
      // If the node corresponding to the last tag of previousXMLTagSequence is misplaced in N it is moved at its correct place
      // (just before the node corresponding to the last tag of currentXMLTagSequence) but because N is immutable (being a LinkedHashMap) a new ordered
      // node N' is created and this node N' must replace N in previousPointer's parent element's last tag key (with the previous example something
      // like previousPointer.put("Data", N') must be done, that is previousPointer.put(currentXMLTagSequence.size() - 2),N');
      // Whenever a node is moved it is because the associated XML element was optional and popped up lately => the element properties are updated
      // in order to reflect the actual element cardinality.
      String[] temp = new String[pointer.size()];
      String[] childrenTagList = pointer.keySet().toArray(temp);
      String lastTagOfCurrentElement = currentXMLTagSequence.get(currentXMLTagSequence.size() - 1);
      String lastTagOfPreviousElement = previousXMLTagSequence.get(previousXMLTagSequence.size() - 1);
      // LinkedHashMap<String, Object> currentTagNode = (LinkedHashMap<String, Object>) pointer.get(currentXMLTagSequence.get(currentXMLTagSequence.size() - 1));
      // LinkedHashMap<String, Object> previousTagNode = (LinkedHashMap<String, Object>) pointer.get(previousXMLTagSequence.get(previousXMLTagSequence.size() - 1));
      int indexOfCurrentTagNode = -1;
      int indexOfPreviousTagNode = -1;
      for (int i = 0; i < childrenTagList.length; i++)
      {
        if (childrenTagList[i].equals(lastTagOfCurrentElement)) indexOfCurrentTagNode = i;
        if (childrenTagList[i].equals(lastTagOfPreviousElement)) indexOfPreviousTagNode = i;
      }
      if ((indexOfCurrentTagNode == -1) || (indexOfPreviousTagNode == -1))
      {
        // Should never happen: means that, though the current element and the last one have been opened once, they were not provided a corresponding node in
        // the graph which is impossible unless there is a bug somewhere.
        throw new SAXException("Internal parsing failure. Location: method <testGraphForMisplacedElement> #2");
      }
      else
      {
        if (indexOfPreviousTagNode > indexOfCurrentTagNode)
        {
          // Element cardinalities are dealt with first.
          // The element associated with the previous XML element is marked as optional (misplaced because it was optional and not provided right from the beginning).
          // Element moves are dealt with, then.
          // The node of the previous XML element is moved before the node of the current XML element.
          // Technically: the node of the previous XML element is swapped with the node just before the node of the current XML element (if it exists, otherwise
          // it is put it at the top position) in the LinkedHashMap<String,Object> pointer... Which can only be done by recreating a new LinkedHashMap<String,Object>
          // object (the insert order of a LinkedHashMap being immutable) and replacing pointer in the corresponding previousPointer value under the key
          // corresponding to the shared current element & previous element parent element's last tag.
          if (XML2CSVLoggingFacade.DEBUG_MODE == true)
          {
            XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG2, "testGraphForMisplacedElement: swapping <" + getXPathAsString(currentXMLTagSequence) + "/<" + indexOfCurrentTagNode
                + ">> and <" + getXPathAsString(previousXMLTagSequence) + "/<" + indexOfPreviousTagNode + ">> in <" + getParentXPathAsString(currentXMLTagSequence) + ">");
          }
          // Changes the previous element cardinality.
          String[] prevProps = properties.get(getXPathAsString(previousXMLTagSequence)); // Exists since startElement occurs before endElement.
          if (XML2CSVCardinality.isOptional(prevProps[0]) == false)
          {
            prevProps[0] = XML2CSVCardinality.setToOptional(prevProps[0]);
            properties.put(getXPathAsString(previousXMLTagSequence), prevProps); // Useless instruction, but anyway.
            XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG2, "testGraphForMisplacedElement: cardinality of element <" + getXPathAsString(previousXMLTagSequence)
                + "> set to optional");
          }
          // Actually swaps the associated nodes in the graph.
          LinkedHashMap<String, Object> replacementNode = new LinkedHashMap<String, Object>();
          if (indexOfCurrentTagNode > 0)
          {
            for (int i = 0; i < indexOfCurrentTagNode; i++)
            {
              replacementNode.put(temp[i], pointer.get(temp[i]));
            }
            replacementNode.put(lastTagOfPreviousElement, pointer.get(lastTagOfPreviousElement));
            replacementNode.put(lastTagOfCurrentElement, pointer.get(lastTagOfCurrentElement));
            for (int i = indexOfCurrentTagNode + 1; i < temp.length; i++)
              if (temp[i].equals(lastTagOfPreviousElement) == false) replacementNode.put(temp[i], pointer.get(temp[i]));
          }
          else
          {
            // The node of the current XML element is the 1st one: the node of the previous XML element is moved at index 0.
            replacementNode.put(lastTagOfPreviousElement, pointer.get(lastTagOfPreviousElement));
            replacementNode.put(lastTagOfCurrentElement, pointer.get(lastTagOfCurrentElement));
            for (int i = indexOfCurrentTagNode + 1; i < temp.length; i++)
              if (temp[i].equals(lastTagOfPreviousElement) == false) replacementNode.put(temp[i], pointer.get(temp[i]));
          }
          // Replaces the content of the shared parent element's last tag node (shared parent of the current element & previous element).
          previousPointer.put(currentXMLTagSequence.get(currentXMLTagSequence.size() - 2), replacementNode);
        }
      }
    }
  }

  /**
   * Flattens the node graph to an ordered element list.<br>
   * Recursive method.
   * @param currentXpath the current XPath associated with the graph part.
   * @param currentGraph the current graph part to flatten.
   */
  @SuppressWarnings("unchecked")
  private void flattenGraph(String currentXpath, LinkedHashMap<String, Object> currentGraph)
  {
    Set<String> keySet = currentGraph.keySet();
    if (keySet.size() == 0)
    {
      // Bottom graph element reached: currentXpath is the XPath of an actual leaf XML element.
      flatLeafElementXPathList.add(currentXpath);
      // If the element has attributes (either because attribute tracking is activated or because virtual attributes were generate to comply with the most extensive optimization
      // mode, or both), they are added just after the element to the list.
      if (attributes.containsKey(currentXpath) == true)
      {
        ArrayList<String[]> attsList = attributes.get(currentXpath);
        for (int i = 0; i < attsList.size(); i++)
          flatLeafElementXPathList.add(currentXpath + "@" + attsList.get(i)[0]); // Attributes are added to the flat list with a "@" between the element and the attribute name.
      }
    }
    else
    {
      // Intermediate graph element reached.
      // Because we deal with leaf elements only an intermediate element is skipped but if the element has attributes, they are added to the list like if the were leaf elements.
      // Exception: a mixture element (both intermediate and leaf) should be treated as a leaf element. A mixture element has an actual element type (something which might happen
      // only if the element has a content) but contains also sub elements.
      if (currentXpath.isEmpty() == false) // Edge effect at the beginning of the graph analysis (which starts with an empty XPath, obviously absent from the property list.
      {
        String[] props = properties.get(currentXpath);
        XML2CSVType type = XML2CSVType.parse(props[1]);
        if (type != XML2CSVType.UNKNOWN)
        {
          // We're dealing with a mixture element.
          flatLeafElementXPathList.add(currentXpath);
        }
      }
      if (attributes.containsKey(currentXpath) == true)
      {
        ArrayList<String[]> attsList = attributes.get(currentXpath);
        for (int i = 0; i < attsList.size(); i++)
          flatLeafElementXPathList.add(currentXpath + "@" + attsList.get(i)[0]); // Attributes are added to the flat list with a "@" between the element and the attribute name.
      }
      // The nested elements of the intermediate element are processed.
      Iterator<String> iterator = keySet.iterator();
      while (iterator.hasNext())
      {
        String node = iterator.next();
        String nextXpath = null;
        if (!currentXpath.equals(XML2CSVMisc.EMPTY_STRING)) nextXpath = currentXpath + "." + node;
        else
          nextXpath = node;
        LinkedHashMap<String, Object> nextGraph = (LinkedHashMap<String, Object>) currentGraph.get(node);
        flattenGraph(nextXpath, nextGraph);
      }
    }
  }

  /**
   * Extracts the parent XPath of an element represented by its tag list and returns it as a plain <code>String</code> (for instance: returns A.B for {A,B,C}).
   * @param tagList the element tag list as an <code>ArrayList&lt;String&gt;</code>.
   * @return the parent XPath as a plain <code>String</code>, or null when there is no parent XPath (for instance for the root element).
   */
  private String getParentXPathAsString(ArrayList<String> tagList)
  {
    String result = null;
    int tagListSize = tagList.size();
    if ((tagList != null) && (tagListSize >= 2)) // Root element A has no parent, element {A,B} has A for parent path, element {A,B,C} has A.B for parent path.
    {
      if (tagListSize == 2) result = tagList.get(0);
      else
      {
        StringBuffer temp = new StringBuffer();
        for (int i = 0; i < tagListSize - 2; i++)
        {
          temp.append(tagList.get(i));
          temp.append(".");
        }
        temp.append(tagList.get(tagListSize - 2));
        result = temp.toString();
      }
    }
    return result;
  }

  /**
   * Extracts the XPath of an element represented by its tag list and returns it as a plain <code>String</code> (for instance: returns A.B.C for {A,B,C}).
   * @param tagList the element tag list as an <code>ArrayList&lt;String&gt;</code>.
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
   * Retrieves the current text from the current element text buffer, which is reset alongside.
   * @param trim <code>true</code> to have the text trimmed as well.
   * @return the current text from the current element text buffer.
   * @throws SAXException in case of error.
   */
  private String getText(boolean trim) throws SAXException
  {
    if (textBuffer == null) return null;
    String s = "" + textBuffer.toString();
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
    if (trim == true) s = s.trim();
    return s;
  }

  /**
   * Updates the current XML element type depending its previous value and its current content.<br>
   * @param contents the current XML element content.
   */
  private void updateElementType(String contents)
  {
    if ((contents == null) || (contents.isEmpty())) return; // No type guess can be made if the element content is empty or null.

    String xpath = getXPathAsString(currentXMLTagSequence);
    String[] props = properties.get(xpath); // Cannot be null because the element content analysis happens after the startElement call.

    XML2CSVType type = XML2CSVType.parse(props[1]); // Transforms the type code back to a Type object.
    if (type == XML2CSVType.UNKNOWN)
    {
      XML2CSVType guessedType = typeSniffer(contents);
      try
      {
        if ((guessedType == XML2CSVType.BOOLEAN) || (guessedType == XML2CSVType.STRING)) props[1] = guessedType.getCode(); // Definitely a boolean or a string.
        else if (guessedType == XML2CSVType.DATE)
        {
          dateParser.parse(contents);
          props[1] = XML2CSVType.DATE.getCode(); // Definitely a date.
        }
        else if (guessedType == XML2CSVType.TIME)
        {
          timeParser.parse(contents);
          props[1] = XML2CSVType.TIME.getCode(); // Definitely a time.
        }
        else if (guessedType == XML2CSVType.DATETIME)
        {
          datetimeParser.parse(contents);
          props[1] = XML2CSVType.DATETIME.getCode(); // Definitely a date&time.
        }
        else if (guessedType == XML2CSVType.INTEGER)
        {
          integerParser.parse(contents);
          props[1] = XML2CSVType.INTEGER.getCode(); // Definitely an integer.
        }
        else if (guessedType == XML2CSVType.DECIMAL)
        {
          decimalParser.parse(contents);
          props[1] = XML2CSVType.DECIMAL.getCode(); // Definitely a decimal.
        }
        else
          props[1] = XML2CSVType.STRING.getCode(); // Defaults to a string.
      }
      catch (ParseException pe)
      {
        props[1] = XML2CSVType.STRING.getCode(); // Plain string in fact.
      }
    }
    else
    {
      // The current XML element type has already been guessed and, if different from STRING, the current element content should be parsed without issue by
      // the corresponding parser. In case of exception, the element type is set to STRING.
      props[1] = checkType(contents, type).getCode();
    }
    properties.put(xpath, props); // Useless instruction, but anyway.
  }

  /**
   * Tries to parse the <code>value</code> provided according to its {@link utils.xml.xml2csv.constants.XML2CSVType expectedType}; if OK returns the same
   * {@link utils.xml.xml2csv.constants.XML2CSVType type} and if not, returns a more appropriate type for the value (most of the time:
   * {@link utils.xml.xml2csv.constants.XML2CSVType#STRING STRING}).
   * @param value the value to test.
   * @param expectedType the expected type of the value.
   * @return the actual type of the value, that is, either the same type if the value was correctly parsed, or, if not, a more appropriate type.
   */
  private XML2CSVType checkType(String value, XML2CSVType expectedType)
  {
    XML2CSVType result = expectedType;
    try
    {
      if ((expectedType == XML2CSVType.BOOLEAN) && ((!value.toLowerCase().equals("true")) && (!value.toLowerCase().equals("false")))) result = XML2CSVType.STRING;
      else if (expectedType == XML2CSVType.DATE) dateParser.parse(value);
      else if (expectedType == XML2CSVType.TIME) timeParser.parse(value);
      else if (expectedType == XML2CSVType.DATETIME) datetimeParser.parse(value);
      else if (expectedType == XML2CSVType.INTEGER)
      {
        if (value.indexOf('.') != -1) // The data is expected without decimal part but one has popped out => tries to swaps to DECIMAL.
        {
          decimalParser.parse(value);
          result = XML2CSVType.DECIMAL; // Definitely a decimal.
        }
        else
          integerParser.parse(value);
      }
      else if (expectedType == XML2CSVType.DECIMAL) decimalParser.parse(value);
    }
    catch (ParseException pe)
    {
      result = XML2CSVType.STRING; // Type set to STRING.
    }
    return result;
  }

  /**
   * Tries to guess the type (DATE, INTEGER, ...) of its input string without raising any exception in the process.
   * @param s the input string.
   * @return the guessed data type.
   */
  private XML2CSVType typeSniffer(String s)
  {
    XML2CSVType result = XML2CSVType.STRING;
    if (s == null) result = XML2CSVType.STRING;
    else if ((s.toLowerCase().equals("true")) || (s.toLowerCase().equals("false"))) result = XML2CSVType.BOOLEAN;
    else if ((s.length() >= 8) && (s.charAt(2) == ':') && (s.charAt(5) == ':')) result = XML2CSVType.TIME;
    else if ((s.length() >= 10) && (s.charAt(4) == '-') && (s.charAt(7) == '-'))
    {
      if ((s.length() >= 19) && (s.charAt(13) == ':')) result = XML2CSVType.DATETIME;
      else
        result = XML2CSVType.DATE;
    }
    else
    {
      int digitCount = 0;
      int decimalSignCount = 0;
      int minusSignCount = 0;
      int nonDecimalSignCount = 0;
      for (int i = 0; i < s.length(); i++)
      {
        char c = s.charAt(i);
        if (Character.isDigit(c)) digitCount++;
        else if (c == '-') minusSignCount++;
        else if (c == '.') decimalSignCount++;
        else
          nonDecimalSignCount++;
      }
      if ((digitCount == 0) || (nonDecimalSignCount > 0) || (minusSignCount > 1) || (decimalSignCount > 1)) result = XML2CSVType.STRING;
      else
      {
        if (decimalSignCount == 0) result = XML2CSVType.INTEGER;
        else
          result = XML2CSVType.DECIMAL;
      }
    }
    return result;
  }

  // ================================================================================
  // Public/protected getters and setters
  // ================================================================================

  /**
   * Returns the actual the ordered XPath list of the leaf XML elements defined in this XML file.
   * @return the XPath list as an <code>ArrayList<String></code>.
   */
  public ArrayList<String> getLeafElementXPathList()
  {
    return flatLeafElementXPathList;
  }

  /**
   * Returns the number of leaf XML elements defined in this XML file.
   * @return the actual leaf XML element count.
   */
  public int getLeafElementCount()
  {
    return flatLeafElementXPathList.size();
  }

  /**
   * Returns the XPath of the i-th leaf XML element defined in this XML file, or <code>null</code> if it is not defined.
   * @param i the index of the leaf XML element.
   * @return the XPath of the i-th leaf XML element, of <code>null</code>.
   */
  public String getLeafElementXPath(int i)
  {
    String result = null;
    if ((i >= 0) && (i < flatLeafElementXPathList.size())) result = flatLeafElementXPathList.get(i); // Example: Root.Row.Amount (element) or Root.Row.Amount@Currency (attribute)
    return result;
  }

  /**
   * Returns the parent XPath of the i-th leaf XML element defined in this XML file, or <code>null</code> if it is not defined.
   * @param i the index of the leaf XML element.
   * @return the XPath of the parent element of the i-th leaf XML element, of <code>null</code>.
   */
  public String getLeafElementParentXPath(int i)
  {
    String result = null;
    if ((i >= 0) && (i < flatLeafElementXPathList.size())) result = flatLeafElementXPathList.get(i); // Example: Root.Row.Amount (element) or Root.Row.Amount@Currency (attribute)
    int j = result.lastIndexOf("@");
    if (j != -1) result = result.substring(0, j); // The parent of an element's attribute is the element itself.
    else
    {
      j = result.lastIndexOf(".");
      if (j == -1) result = null; // The i-th element was the root element (Example: Root), which has no parent.
      else
        result = result.substring(0, j); // Parent of a regular element. Example: Root.Row
    }
    return result;
  }

  /**
   * Returns the short name of the i-th leaf XML element defined in this XML file (that is: the last part of its XPath), or <code>null</code> if it is not defined.
   * @param i the index of the leaf XML element.
   * @return the short name of the i-th leaf XML element, of <code>null</code>.
   */
  public String getLeafElementShortName(int i)
  {
    String result = null;
    if ((i >= 0) && (i < flatLeafElementXPathList.size())) result = flatLeafElementXPathList.get(i); // Example: Root.Row.Amount (element) or Root.Row.Amount@Currency (attribute)
    int j = result.lastIndexOf("@");
    if (j != -1) result = result.substring(j + 1); // The short name of an element attribute is the attribute name itself.
    else
    {
      j = result.lastIndexOf(".");
      if (j != -1) result = result.substring(j + 1); // Short name of a regular element. Example: Amount
    }
    return result;
  }

  /**
   * Returns the cardinality of the i-th leaf XML element defined in this XML file, or <code>null</code> if it is not defined.
   * @param i the index of the leaf XML element.
   * @return the cardinality of the i-th leaf XML element, of <code>null</code>.
   */
  public XML2CSVCardinality getLeafElementCardinality(int i)
  {
    XML2CSVCardinality result = null;
    String xpath = null;
    if ((i >= 0) && (i < flatLeafElementXPathList.size())) xpath = flatLeafElementXPathList.get(i); // Example: Root.Row.Amount (element) or Root.Row.Amount@Currency (attribute)
    if (xpath != null)
    {
      int j = xpath.lastIndexOf("@");
      if (j != -1)
      {
        // The i-th element is an attribute.
        String xpathElement = xpath.substring(0, j);
        String attShortName = xpath.substring(j + 1);
        ArrayList<String[]> attsList = attributes.get(xpathElement);
        String[] att = null;
        for (int k = 0; k < attsList.size(); k++)
        {
          if (attsList.get(k)[0].equals(attShortName))
          {
            att = attsList.get(k);
            break;
          }
        }
        if (att != null) result = XML2CSVCardinality.parse(att[1]);
      }
      else
      {
        // The i-th element is a regular element.
        String[] props = properties.get(xpath);
        if (props != null) result = XML2CSVCardinality.parse(props[0]);
      }
    }
    return result;
  }

  /**
   * Returns the type of the i-th leaf XML element defined in this XML file, or <code>null</code> if it is not defined.
   * @param i the index of the leaf XML element.
   * @return the type of the i-th leaf XML element, of <code>null</code>.
   */
  public XML2CSVType getLeafElementType(int i)
  {
    XML2CSVType result = null;
    String xpath = null;
    if ((i >= 0) && (i < flatLeafElementXPathList.size())) xpath = flatLeafElementXPathList.get(i); // Example: Root.Row.Amount (element) or Root.Row.Amount@Currency (attribute)
    if (xpath != null)
    {
      int j = xpath.lastIndexOf("@");
      if (j != -1)
      {
        // The i-th element is an attribute.
        String xpathElement = xpath.substring(0, j);
        String attShortName = xpath.substring(j + 1);
        ArrayList<String[]> attsList = attributes.get(xpathElement);
        String[] att = null;
        for (int k = 0; k < attsList.size(); k++)
        {
          if (attsList.get(k)[0].equals(attShortName))
          {
            att = attsList.get(k);
            break;
          }
        }
        if (att != null) result = XML2CSVType.parse(att[2]);
      }
      else
      {
        // The i-th element is a regular element.
        String[] props = properties.get(xpath);
        if (props != null) result = XML2CSVType.parse(props[1]);
      }
    }
    return result;
  }

  /**
   * Returns the index of one particular XPath in the XPath list of the leaf XML elements defined in this XML file, of <code>-1</code> if the XPath cannot be found.
   * @param xpath the XPath to find.
   * @return the index of the XPath in the leaf element XPath list, or <code>-1</code>.
   */
  public int getIndexOfLeafElement(String xpath)
  {
    int result = -1;
    for (int i = 0; i < flatLeafElementXPathList.size(); i++)
    {
      if (flatLeafElementXPathList.get(i).equals(xpath))
      {
        result = i;
        break;
      }
    }
    return result;
  }

  /**
   * Returns the XPath list of leaf elements or attributes which have the input XPath as ancestor, of <code>null</code> if the input XPath is not an ancestor of any leaf element or
   * attribute.
   * @param ancestorXpath the input XPath.
   * @return the XPath list of leaf elements or attributes which have the input XPath as ancestor, or <code>null</code>.
   */
  public String[] getDescendantLeafElements(String ancestorXpath)
  {
    String[] result = null;
    ArrayList<String> res = new ArrayList<String>();
    for (int i = 0; i < flatLeafElementXPathList.size(); i++)
      if ((flatLeafElementXPathList.get(i).startsWith(ancestorXpath)) && (flatLeafElementXPathList.get(i).length() > ancestorXpath.length())
          && ((flatLeafElementXPathList.get(i).charAt(ancestorXpath.length()) == '.') || (flatLeafElementXPathList.get(i).charAt(ancestorXpath.length()) == '@')))
      {
        res.add(flatLeafElementXPathList.get(i));
      }
    if (res.size() > 0)
    {
      String[] temp = new String[res.size()];
      result = res.toArray(temp);
    }
    return result;
  }

  /**
   * Builds an {@link utils.xml.xml2csv.ElementsDescription ElementsDescription} instance describing the ordered list of leaf XML elements of the XML file analyzed and returns it.<br>
   * The description is backed by an unstructured {@link utils.xml.xml2csv.ElementsDescription#getDictionary() dictionary} providing extra information about the XML structure.
   * @return the <code>ElementsDescription</code> instance describing leaf XML elements of the XML file.
   */
  public ElementsDescription getLeafElementsDescription()
  {
    int leafElementCount = getLeafElementCount();
    String[] leafElementsShortNames = new String[leafElementCount];
    String[] leafElementsXPaths = new String[leafElementCount];
    String[] leafElementsParentXPaths = new String[leafElementCount];
    XML2CSVCardinality[] leafElementsCardinalities = new XML2CSVCardinality[leafElementCount];
    XML2CSVType[] leafElementsTypes = new XML2CSVType[leafElementCount];

    for (int i = 0; i < leafElementCount; i++)
    {
      leafElementsShortNames[i] = getLeafElementShortName(i);
      leafElementsXPaths[i] = getLeafElementXPath(i);
      leafElementsParentXPaths[i] = getLeafElementParentXPath(i);
      leafElementsCardinalities[i] = getLeafElementCardinality(i);
      leafElementsTypes[i] = getLeafElementType(i);
    }
    HashMap<String, String[]> dictionary = getAllElementsDescription();
    return new ElementsDescription(leafElementsShortNames, leafElementsXPaths, leafElementsParentXPaths, leafElementsCardinalities, leafElementsTypes, dictionary, namespaces);
  }

  /**
   * Returns a description of all the elements (leaf or intermediate) met in the structure provided as a <code>HashMap&lt;String, String[]&gt;</code> for random access where:<br>
   * <ul>
   * <li>keys are element XPaths;
   * <li>the value associated with a key is a <code>String[4]</code> with: at index <code>0</code>, the element {@link utils.xml.xml2csv.constants.XML2CSVCardinality#getCode()
   * cardinality code}, at index <code>1</code> the element {@link utils.xml.xml2csv.constants.XML2CSVType#getCode() type code}, at index <code>2</code> the element's occurrence
   * count (in all the file across all element repetitions) and at index <code>3</code> the element's {@link utils.xml.xml2csv.constants.XML2CSVNature#getCode() nature code}.
   * </ul>
   * @return the whole set of structure elements provided as a <code>HashMap&lt;String, String[]&gt;</code> for random XPath access.
   */
  public HashMap<String, String[]> getAllElementsDescription()
  {
    // The inner dictionary is cloned, and an additional property is added after the regular properties for each element indicating the element's nature.
    // At present attributes are not recorded in the dictionary but they might be in the future, and this method handles them already.
    HashMap<String, String[]> clone = new HashMap<String, String[]>();
    Iterator<String> iterator = properties.keySet().iterator();
    while (iterator.hasNext())
    {
      String xpath = iterator.next();
      String[] props = properties.get(xpath);
      String[] copy = new String[props.length + 1];
      for (int i = 0; i < props.length; i++)
        copy[i] = props[i];
      XML2CSVType type = XML2CSVType.parse(props[1]);
      boolean attribute = false; // True if the XPath maps an attribute, and false if it is a regular element.
      if (xpath.indexOf("@") != -1) attribute = true;
      boolean intermediate = false; // True if the XPath maps an intermediate element (with sub elements), and false if it is a leaf element.
      Iterator<String> iterator2 = properties.keySet().iterator();
      while (iterator2.hasNext())
      {
        String xpath2 = iterator2.next();
        if ((xpath2.startsWith(xpath)) && (xpath2.length() > xpath.length()) && (xpath2.substring(xpath.length()).startsWith("."))) intermediate = true;
      }
      copy[props.length] = XML2CSVNature.parse(intermediate, attribute, type).getCode(); // The element's nature is added at the end of the regular properties.
      clone.put(xpath, copy);
    }
    return clone;
  }
}
