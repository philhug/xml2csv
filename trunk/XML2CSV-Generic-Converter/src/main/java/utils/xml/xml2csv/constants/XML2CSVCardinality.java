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
 * Enumeration class listing XML element cardinalities, that is:<br>
 * <ul>
 * <li><code>ZERO_TO_MANY</code> for a <code>0..N</code> cardinality (that is, for an optional XML multi-occurrence element);
 * <li><code>ONE_TO_MANY</code> for a <code>1..N</code> cardinality (that is, for a mandatory XML multi-occurrence element);
 * <li><code>ZERO_TO_ONE</code> for a <code>0..1</code> cardinality (that is, for an optional XML mono-occurrence element);
 * <li><code>ONE_TO_ONE</code> for a <code>1..1</code> cardinality (that is, for a mandatory XML mono-occurrence element).
 * </ul>
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public enum XML2CSVCardinality
{
  /** <code>0..N</code> cardinality. */
  ZERO_TO_MANY("0N", "Optional multiple elements", "0", "unbounded"),

  /** <code>1..N</code> cardinality. */
  ONE_TO_MANY("1N", "Mandatory multiple elements", "1", "unbounded"),

  /** <code>0..1</code> cardinality. */
  ZERO_TO_ONE("01", "Optional single element", "0", "1"),

  /** <code>1..1</code> cardinality. */
  ONE_TO_ONE("11", "Mandatory single element", "1", "1");

  /** Attribute recording the code of the current enumeration occurrence. */
  private final String code;

  /** Attribute recording the label of the current enumeration occurrence. */
  private final String label;

  /** Attribute recording the minimum element count of the current enumeration occurrence. */
  private final String minOccurs;

  /** Attribute recording the max element count of the current enumeration occurrence. */
  private final String maxOccurs;

  /**
   * <code>XML2CSVCardinality<code> constructor.
   * @param code the <code>XML2CSVCardinality</code> code.
   * @param lab the <code>XML2CSVCardinality</code> label.
   * @param min the <code>XML2CSVCardinality</code> minimum element count.
   * @param max the <code>XML2CSVCardinality</code> max element count.
   */
  private XML2CSVCardinality(String code, String lab, String min, String max)
  {
    this.code = code;
    this.label = lab;
    this.minOccurs = min;
    this.maxOccurs = max;
  }

  /**
   * Getter which returns the cardinality code.
   * @return the cardinality code.
   */
  public String getCode()
  {
    return this.code;
  }

  /**
   * Getter which returns the cardinality label.
   * @return the cardinality label.
   */
  public String getLabel()
  {
    return this.label;
  }

  /**
   * Getter which returns the cardinality minimum element count.
   * @return the cardinality minimum element count.
   */
  public String getMinOccurs()
  {
    return this.minOccurs;
  }

  /**
   * Getter which returns the cardinality max element count.
   * @return the cardinality max element count.
   */
  public String getMaxOccurs()
  {
    return this.maxOccurs;
  }

  /**
   * Returns the <code>XML2CSVCardinality</code> associated with the <code>code</code> provided, or <code>null</code> if not defined.
   * @param code a <code>XML2CSVCardinality</code> code.
   * @return the associated <code>XML2CSVCardinality</code>, or <code>null</code>.
   */
  public static XML2CSVCardinality parse(String code)
  {
    XML2CSVCardinality result = null;
    XML2CSVCardinality[] values = XML2CSVCardinality.values();
    for (int i = 0; i < values.length; i++)
    {
      XML2CSVCardinality oneCard = values[i];
      if (oneCard.getCode().equals(code))
      {
        result = oneCard;
        break;
      }
    }
    return result;
  }

  /**
   * Takes a <code>XML2CSVCardinality</code> for input and returns the corresponding <code>XML2CSVCardinality</code> with an unbounded max element count.<br>
   * In other words:<br>
   * <ul>
   * <li>returns <code>ONE_TO_MANY</code> for an input <code>ONE_TO_ONE</code> cardinality and leaves <code>ONE_TO_MANY</code> unchanged;
   * <li>returns <code>ZERO_TO_MANY</code> for an input <code>ZERO_TO_ONE</code> cardinality and leaves <code>ZERO_TO_MANY</code> unchanged.
   * </ul>
   * @param c the input <code>XML2CSVCardinality</code>.
   * @return the output <code>XML2CSVCardinality</code>.
   */
  public static XML2CSVCardinality setToUnbounded(XML2CSVCardinality c)
  {
    XML2CSVCardinality result = c;
    if (c == XML2CSVCardinality.ONE_TO_ONE) result = XML2CSVCardinality.ONE_TO_MANY;
    if (c == XML2CSVCardinality.ZERO_TO_ONE) result = XML2CSVCardinality.ZERO_TO_MANY;
    return result;
  }

  /**
   * Takes a <code>XML2CSVCardinality</code> code for input and returns the corresponding <code>XML2CSVCardinality</code> code with an unbounded max element count.<br>
   * In other words:<br>
   * <ul>
   * <li>returns <code>ONE_TO_MANY</code>'s code for an input <code>ONE_TO_ONE</code> cardinality code and leaves <code>ONE_TO_MANY</code>'s code unchanged;
   * <li>returns <code>ZERO_TO_MANY</code>'s code for an input <code>ZERO_TO_ONE</code> cardinality code and leaves <code>ZERO_TO_MANY</code>'s code unchanged.
   * </ul>
   * @param code the input cardinality code.
   * @return the output cardinality code.
   */
  public static String setToUnbounded(String code)
  {
    String result = null;
    XML2CSVCardinality temp = XML2CSVCardinality.parse(code);
    result = temp.code;
    if (temp != null)
    {
      if (temp == XML2CSVCardinality.ONE_TO_ONE) result = XML2CSVCardinality.ONE_TO_MANY.code;
      if (temp == XML2CSVCardinality.ZERO_TO_ONE) result = XML2CSVCardinality.ZERO_TO_MANY.code;
    }
    return result;
  }

  /**
   * Takes a <code>XML2CSVCardinality</code> code for input and returns <code>true</code> if the cardinality has an unbounded max element count, and <code>false</code> otherwise.<br>
   * In other words:<br>
   * <ul>
   * <li>returns <code>false</code> for an input <code>ZERO_TO_ONE</code> cardinality;
   * <li>returns <code>false</code> for an input <code>ONE_TO_ONE</code> cardinality;
   * <li>returns <code>true</code> for an input <code>ZERO_TO_MANY</code> cardinality;
   * <li>returns <code>true</code> for an input <code>ONE_TO_MANY</code> cardinality.
   * </ul>
   * @param code the input cardinality code.
   * @return true if it is an unbounded cardinality.
   */
  public static boolean isUnbounded(String code)
  {
    boolean result = true;
    XML2CSVCardinality temp = XML2CSVCardinality.parse(code);
    if (temp == XML2CSVCardinality.ONE_TO_ONE) result = false;
    if (temp == XML2CSVCardinality.ZERO_TO_ONE) result = false;
    return result;
  }

  /**
   * Takes a <code>XML2CSVCardinality</code> for input and returns the corresponding <code>XML2CSVCardinality</code> with a zero minimum element count.<br>
   * In other words:<br>
   * <ul>
   * <li>returns <code>ZERO_TO_ONE</code> for an input <code>ONE_TO_ONE</code> cardinality and leaves <code>ZERO_TO_ONE</code> unchanged;
   * <li>returns <code>ZERO_TO_MANY</code> for an input <code>ONE_TO_MANY</code> cardinality and leaves <code>ZERO_TO_MANY</code> unchanged.
   * </ul>
   * @param c the input cardinality.
   * @return the output cardinality.
   */
  public static XML2CSVCardinality setToOptional(XML2CSVCardinality c)
  {
    XML2CSVCardinality result = c;
    if (c == XML2CSVCardinality.ONE_TO_ONE) result = XML2CSVCardinality.ZERO_TO_ONE;
    if (c == XML2CSVCardinality.ONE_TO_MANY) result = XML2CSVCardinality.ZERO_TO_MANY;
    return result;
  }

  /**
   * Takes a <code>XML2CSVCardinality</code> code for input and returns the corresponding <code>XML2CSVCardinality</code> code with a zero minimum element count.<br>
   * In other words:<br>
   * <ul>
   * <li>returns <code>ZERO_TO_ONE</code>'s code for an input <code>ONE_TO_ONE</code> cardinality code and leaves <code>ZERO_TO_ONE</code>'s code unchanged;
   * <li>returns <code>ZERO_TO_MANY</code>'s code for an input <code>ONE_TO_MANY</code> cardinality code and leaves <code>ZERO_TO_MANY</code>'s code unchanged.
   * </ul>
   * @param code the input cardinality code.
   * @return the output cardinality code.
   */
  public static String setToOptional(String code)
  {
    String result = null;
    XML2CSVCardinality temp = XML2CSVCardinality.parse(code);
    result = temp.code;
    if (temp != null)
    {
      if (temp == XML2CSVCardinality.ONE_TO_ONE) result = XML2CSVCardinality.ZERO_TO_ONE.code;
      if (temp == XML2CSVCardinality.ONE_TO_MANY) result = XML2CSVCardinality.ZERO_TO_MANY.code;
    }
    return result;
  }

  /**
   * Takes a <code>XML2CSVCardinality</code> code for input and returns <code>true</code> if the cardinality has a zero minimum element count, and <code>false</code> otherwise.<br>
   * In other words:<br>
   * <ul>
   * <li>returns <code>true</code> for an input <code>ZERO_TO_ONE</code> cardinality;
   * <li>returns <code>false</code> for an input <code>ONE_TO_ONE</code> cardinality;
   * <li>returns <code>true</code> for an input <code>ZERO_TO_MANY</code> cardinality;
   * <li>returns <code>false</code> for an input <code>ONE_TO_MANY</code> cardinality.
   * </ul>
   * @param code the input cardinality code.
   * @return true if it is an optional cardinality.
   */
  public static boolean isOptional(String code)
  {
    boolean result = true;
    XML2CSVCardinality temp = XML2CSVCardinality.parse(code);
    if (temp == XML2CSVCardinality.ONE_TO_ONE) result = false;
    if (temp == XML2CSVCardinality.ONE_TO_MANY) result = false;
    return result;
  }
}
