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

import java.util.HashMap;
import java.util.StringTokenizer;

import utils.xml.xml2csv.constants.XML2CSVCardinality;
import utils.xml.xml2csv.constants.XML2CSVType;

/**
 * This class groups arrays which, as a whole, provide a description of ordered XML elements issued from an XML structure.<br>
 * It is backed by an extra unstructured dictionary shaped as a <code>HashMap&lt;String, String[]&gt;</code> which provides extra information about the XML structure for random
 * access where:<br>
 * <ul>
 * <li>keys are element XPaths;
 * <li>the value associated with a key is a <code>String[4]</code> with: at index <code>0</code>, the element {@link utils.xml.xml2csv.constants.XML2CSVCardinality#getCode()
 * cardinality code}, at index <code>1</code> the element {@link utils.xml.xml2csv.constants.XML2CSVType#getCode() type code}, at index <code>2</code> the element's occurrence
 * count (in all the file across all element repetitions) and at index <code>3</code> the element {@link utils.xml.xml2csv.constants.XML2CSVNature#getCode() nature code}.
 * </ul>
 * @author L. Popieul (lochrann@rocketmail.com)
 */
class ElementsDescription
{
  /** An array listing the elements short names. */
  private String[] elementsShortNames = null;

  /** An array listing the elements XPaths. */
  private String[] elementsXPaths = null;

  /** An array listing the elements parent XPaths. */
  private String[] elementsParentXPaths = null;

  /** An array listing the elements cardinalities. */
  private XML2CSVCardinality[] elementsCardinalities = null;

  /** An array listing the elements types. */
  private XML2CSVType[] elementsTypes = null;

  /** A hash map providing unstructured details about the surrounding XML structure. */
  private HashMap<String, String[]> dictionary = null;

  /** A hash map providing details about name spaces. */
  private HashMap<String, String> namespaces = null;

  /**
   * <code>ElementsDescription</code> constructor.
   * @param elementsXPaths the array listing the XML elements XPaths.
   * @param elementsParentXPaths the array listing the XML elements parent XPaths.
   * @param elementsCardinalities the array listing the XML elements cardinalities.
   * @param elementsTypes the array listing the XML elements types.
   * @param dictionary the unstructured dictionary from which the XML elements are extracted.
   * @param namespaces the name spaces.
   */
  public ElementsDescription(String[] elementsShortNames, String[] elementsXPaths, String[] elementsParentXPaths, XML2CSVCardinality[] elementsCardinalities,
      XML2CSVType[] elementsTypes, HashMap<String, String[]> dictionary, HashMap<String, String> namespaces)
  {
    this.elementsShortNames = elementsShortNames;
    this.elementsXPaths = elementsXPaths;
    this.elementsParentXPaths = elementsParentXPaths;
    this.elementsCardinalities = elementsCardinalities;
    this.elementsTypes = elementsTypes;
    this.dictionary = dictionary;
    this.namespaces = namespaces;
  }

  /**
   * Returns the element count.
   * @return the element count.
   */
  public int getElementCount()
  {
    return elementsXPaths.length;
  }

  /**
   * Returns the array listing the XML elements short names.
   * @return the array listing the XML elements short names.
   */
  public String[] getElementsShortNames()
  {
    return elementsShortNames;
  }

  /**
   * Returns the array listing the XML elements XPaths.
   * @return the array listing the XML elements XPaths.
   */
  public String[] getElementsXPaths()
  {
    return elementsXPaths;
  }

  /**
   * Returns the array listing the XML elements parent XPaths.
   * @return the array listing the XML elements parent XPaths.
   */
  public String[] getElementsParentXPaths()
  {
    return elementsParentXPaths;
  }

  /**
   * Returns the array listing the XML elements cardinalities.
   * @return the array listing the XML elements cardinalities.
   */
  public XML2CSVCardinality[] getElementsCardinalities()
  {
    return elementsCardinalities;
  }

  /**
   * Returns the array listing the XML elements types.
   * @return the array listing the XML elements types.
   */
  public XML2CSVType[] getElementsTypes()
  {
    return elementsTypes;
  }

  /**
   * Returns the unstructured dictionary from which the ordered XML elements where extracted.<br>
   * The dictionary is provided as a <code>HashMap&lt;String, String[]&gt;</code> optimized for random access where:<br>
   * <ul>
   * <li>keys are element XPaths;
   * <li>the value associated with a key is a <code>String[4]</code> with: at index <code>0</code>, the element {@link utils.xml.xml2csv.constants.XML2CSVCardinality#getCode()
   * cardinality code}, at index <code>1</code> the element {@link utils.xml.xml2csv.constants.XML2CSVType#getCode() type code}, at index <code>2</code> the element's occurrence
   * count (in all the file across all element repetitions) and at index <code>3</code> the element {@link utils.xml.xml2csv.constants.XML2CSVNature#getCode() nature code}.
   * </ul>
   * @return the unstructured element dictionary from which the ordered XML elements where extracted.
   */
  public HashMap<String, String[]> getDictionary()
  {
    return dictionary;
  }

  /**
   * Returns the list of name spaces defined in the XML structure.
   * The list is provided as a <code>HashMap&lt;String, String&gt;</code> where:<br>
   * <ul>
   * <li>keys are name spaces aliases, plus one {@link utils.xml.xml2csv.constants.XML2CSVMisc#DEFAULT_NAMESPACE_ALIAS default} key for the default name space;
   * <li>the value associated with a key is the full name space string value.
   * </ul>
   * @return the name space list.
   */
  public HashMap<String, String> getNamespaces()
  {
    return namespaces;
  }

  /**
   * Returns a subset of the dictionary providing unstructured information about the intermediary elements between two elements known by their XPaths <code>xpath1</code> and
   * <code>xpath2</code>, or <code>null</code> if the elements are not related (that is: if <code>xpath2</code> is not a sub XPath of <code>xpath1</code>).<br>
   * The subset will be empty if <code>xpath2</code> is a direct child of <code>xpath1</code>.
   * @param xpath1 the first element XPath.
   * @param xpath2 the second element XPath.
   * @return the dictionary subset of elements between <code>xpath1</code> and <code>xpath2</code>, or <code>null</code> when there is no relationship between the elements.
   */
  public HashMap<String, String[]> getIntermediateXPaths(String xpath1, String xpath2)
  {
    HashMap<String, String[]> result = null;
    if ((xpath2.startsWith(xpath1)) && (xpath2.length() > xpath1.length()) && ((xpath2.charAt(xpath1.length()) == '.') || (xpath2.charAt(xpath1.length()) == '@')))
    {
      result = new HashMap<String, String[]>();
      // OK, the 2 XPaths are really related.
      String subXPath = xpath2.substring(xpath1.length()); // If xpath1 = one.two.three and xpath2 = one.two.three.four.five then subXPath = .four.five.
      if (((subXPath.lastIndexOf('.') == 0) && (subXPath.lastIndexOf('@') == -1)) || ((subXPath.lastIndexOf('.') == -1) && (subXPath.lastIndexOf('@') == 0)))
      {
        // xpath2 is a direct child of xpath1.
        // For instance: if xpath1 = one.two.three and xpath2 = one.two.three.four then subXPath = .four only a '.' at index 0.
        // We leave the subset result non null, but empty.
      }
      else
      {
        StringTokenizer st = new StringTokenizer(subXPath, ".@", true);
        String intermediateXPath = xpath1;
        while (st.hasMoreTokens())
        {
          String separator = st.nextToken(); // @ or .
          String shortName = st.nextToken(); // We expect shortName = four, then five.
          intermediateXPath = intermediateXPath + separator + shortName;
          if (intermediateXPath.equals(xpath2) == false) // We do not add xpath2 to the list of intermediary elements between xpath1 and xpath2.
          result.put(intermediateXPath, dictionary.get(intermediateXPath));
        }
      }
    }
    return result;
  }
}
