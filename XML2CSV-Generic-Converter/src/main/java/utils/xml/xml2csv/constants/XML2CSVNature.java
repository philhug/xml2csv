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
package utils.xml.xml2csv.constants;

/**
 * Enumeration class listing XML tag natures, that is:<br>
 * <ul>
 * <li><code>INTERMEDIATE_ELEMENT</code> for an intermediate element;
 * <li><code>INTERMEDIATE_ELEMENT_ATTRIBUTE</code> for an attribute of an intermediate element;
 * <li><code>LEAF_ELEMENT</code> for a leaf element;
 * <li><code>LEAF_ELEMENT_ATTRIBUTE</code> for an attribute of a leaf element.
 * </ul>
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public enum XML2CSVNature
{
  /** Intermediate element. */
  INTERMEDIATE_ELEMENT("IE", "Intermediate element"),

  /** Attribute of an intermediate element. */
  INTERMEDIATE_ELEMENT_ATTRIBUTE("IEA", "Attribute of an intermediate element"),

  /** Leaf element. */
  LEAF_ELEMENT("LE", "Leaf element"),

  /** Attribute of a leaf element. */
  LEAF_ELEMENT_ATTRIBUTE("LEA", "Attribute of a leaf element");

  /** Attribute recording the code of the current enumeration occurrence. */
  private final String code;

  /** Attribute recording the label of the current enumeration occurrence. */
  private final String label;

  /**
   * <code>XML2CSVNature</code> constructor.
   * @param code the <code>XML2CSVNature</code> code.
   * @param lab the <code>XML2CSVNature</code> label.
   */
  private XML2CSVNature(String code, String lab)
  {
    this.code = code;
    this.label = lab;
  }

  /**
   * Getter which returns the nature code.
   * @return the nature code.
   */
  public String getCode()
  {
    return this.code;
  }

  /**
   * Getter which returns the nature label.
   * @return the nature label.
   */
  public String getLabel()
  {
    return this.label;
  }

  /**
   * Returns the <code>XML2CSVNature</code> associated with the <code>code</code> provided, or <code>null</code> if not defined.
   * @param code a <code>XML2CSVNature</code> code.
   * @return the associated <code>XML2CSVNature</code>, or <code>null</code>.
   */
  public static XML2CSVNature parse(String code)
  {
    XML2CSVNature result = null;
    XML2CSVNature[] values = XML2CSVNature.values();
    for (int i = 0; i < values.length; i++)
    {
      XML2CSVNature oneNat = values[i];
      if (oneNat.getCode().equals(code))
      {
        result = oneNat;
        break;
      }
    }
    return result;
  }

  /**
   * Returns the <code>XML2CSVNature</code> corresponding to the parameters provided.
   * @param intermediate <code>true</code> for an intermediate tag and <code>false</code> for a leaf tag.
   * @param attribute <code>true</code> for an attribute and <code>false</code> for a regular element.
   * @return the associated <code>XML2CSVNature</code>, or <code>null</code>.
   */
  public static XML2CSVNature parse(boolean intermediate, boolean attribute)
  {
    XML2CSVNature result = null;
    if (intermediate == true)
    {
      if (attribute == true) result = XML2CSVNature.INTERMEDIATE_ELEMENT_ATTRIBUTE;
      else
        result = XML2CSVNature.INTERMEDIATE_ELEMENT;
    }
    else
    {
      if (attribute == true) result = XML2CSVNature.LEAF_ELEMENT_ATTRIBUTE;
      else
        result = XML2CSVNature.LEAF_ELEMENT;
    }
    return result;
  }

  /**
   * Takes an <code>XML2CSVNature</code> code for input and returns <code>true</code> if it marks a leaf tag, and <code>false</code> otherwise.<br>
   * In other words:<br>
   * <ul>
   * <li>returns <code>false</code> for an input <code>INTERMEDIATE_ELEMENT</code> nature;
   * <li>returns <code>false</code> for an input <code>INTERMEDIATE_ELEMENT_ATTRIBUTE</code> nature;
   * <li>returns <code>true</code> for an input <code>LEAF_ELEMENT</code> nature;
   * <li>returns <code>true</code> for an input <code>LEAF_ELEMENT_ATTRIBUTE</code> nature.
   * </ul>
   * @param code the input nature code.
   * @return <code>true</code> if it is a leaf nature.
   */
  public static boolean isLeaf(String code)
  {
    boolean result = true;
    XML2CSVNature temp = XML2CSVNature.parse(code);
    if (temp == XML2CSVNature.INTERMEDIATE_ELEMENT) result = false;
    if (temp == XML2CSVNature.INTERMEDIATE_ELEMENT_ATTRIBUTE) result = false;
    return result;
  }

  /**
   * Takes an <code>XML2CSVNature</code> code for input and returns <code>true</code> if it marks an attribute, and <code>false</code> otherwise.<br>
   * In other words:<br>
   * <ul>
   * <li>returns <code>false</code> for an input <code>INTERMEDIATE_ELEMENT</code> nature;
   * <li>returns <code>true</code> for an input <code>INTERMEDIATE_ELEMENT_ATTRIBUTE</code> nature;
   * <li>returns <code>false</code> for an input <code>LEAF_ELEMENT</code> nature;
   * <li>returns <code>true</code> for an input <code>LEAF_ELEMENT_ATTRIBUTE</code> nature.
   * </ul>
   * @param code the input nature code.
   * @return <code>true</code> if it is an attribute.
   */
  public static boolean isAttribute(String code)
  {
    boolean result = true;
    XML2CSVNature temp = XML2CSVNature.parse(code);
    if (temp == XML2CSVNature.INTERMEDIATE_ELEMENT) result = false;
    if (temp == XML2CSVNature.LEAF_ELEMENT) result = false;
    return result;
  }
}
