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
 * Enumeration class listing the XML element types manipulated by this library, that is:<br>
 * <ul>
 * <li><code>STRING</code> for a plain string;
 * <li><code>DATE</code> for an ISO 8601 date;
 * <li><code>DATETIME</code> for an ISO 8601 date&time;
 * <li><code>TIME</code> for an ISO 8601 time;
 * <li><code>BOOLEAN</code> for a boolean;
 * <li><code>INTEGER</code> for an integer;
 * <li><code>DECIMAL</code> for a decimal.
 * </ul>
 * An additional <code>UNKNOWN</code> value has been defined to represent an unknown XML type.
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public enum XML2CSVType
{
  /** Unknown type. */
  UNKNOWN("?", "Unknown"),

  /** String. */
  STRING("S", "String"),

  /** Date. */
  DATE("D", "ISO 8601 date"),

  /** Date&time. */
  DATETIME("DT", "ISO 8601 datetime"),

  /** Time. */
  TIME("T", "ISO 8601 time"),

  /** Boolean. */
  BOOLEAN("B", "Boolean"),

  /** Integer. */
  INTEGER("I", "Integer"),

  /** Decimal. */
  DECIMAL("DC", "Decimal");

  /** Attribute recording the code of the current enumeration occurrence. */
  private final String code;

  /** Attribute recording the label of the current enumeration occurrence. */
  private final String label;

  /**
   * <code>XML2CSVType</code> constructor.
   * @param code the <code>XML2CSVType</code> code.
   * @param lab the <code>XML2CSVType</code> label.
   */
  private XML2CSVType(String code, String lab)
  {
    this.code = code;
    this.label = lab;
  }

  /**
   * Getter which returns the type code.
   * @return the type code.
   */
  public String getCode()
  {
    return this.code;
  }

  /**
   * Getter which returns the type label.
   * @return the type label.
   */
  public String getLabel()
  {
    return this.label;
  }

  /**
   * Returns the <code>XML2CSVType</code> associated with the <code>code</code> provided, or <code>null</code> if not defined.
   * @param code a <code>XML2CSVType</code> code.
   * @return the associated <code>XML2CSVType</code>, or <code>null</code>.
   */
  public static XML2CSVType parse(String code)
  {
    XML2CSVType result = null;
    XML2CSVType[] values = XML2CSVType.values();
    for (int i = 0; i < values.length; i++)
    {
      XML2CSVType oneType = values[i];
      if (oneType.getCode().equals(code))
      {
        result = oneType;
        break;
      }
    }
    return result;
  }
}
