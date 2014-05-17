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
 * Enumeration class listing the XML2CSV optimization levels available, that is from the lowest to the highest:<br>
 * <ul>
 * <li><code>NONE</code> for no optimization at all;
 * <li><code>STANDARD</code> for standard optimization;
 * <li><code>EXTENSIVE_V1</code> for extensive optimization using variant 1;
 * <li><code>EXTENSIVE_V2</code> for extensive optimization using variant 2;
 * <li><code>EXTENSIVE_V3</code> for extensive optimization using variant 3.
 * </ul>
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public enum XML2CSVOptimization
{
  /** Disabled optimization. */
  NONE("N", "No optomization"),

  /** Standard optimization. */
  STANDARD("S", "Standard optimization mode"),

  /** Extensive optimization, variant 1. */
  EXTENSIVE_V1("XV1", "Extensive optimization mode variant 1"),

  /** Extensive optimization, variant 2. */
  EXTENSIVE_V2("XV2", "Extensive optimization mode variant 2"),

  /** Extensive optimization, variant 3. */
  EXTENSIVE_V3("XV3", "Extensive optimization mode variant 3");

  /** Attribute recording the code of the current enumeration occurrence. */
  private final String code;

  /** Attribute recording the label of the current enumeration occurrence. */
  private final String label;

  /**
   * <code>XML2CSVOptimization</code> constructor.
   * @param code the <code>XML2CSVOptimization</code> code.
   * @param lab the <code>XML2CSVOptimization</code> label.
   */
  private XML2CSVOptimization(String code, String lab)
  {
    this.code = code;
    this.label = lab;
  }

  /**
   * Getter which returns the optimization level code.
   * @return the optimization level code.
   */
  public String getCode()
  {
    return this.code;
  }

  /**
   * Getter which returns the optimization level label.
   * @return the optimization level label.
   */
  public String getLabel()
  {
    return this.label;
  }

  /**
   * Returns the <code>XML2CSVOptimization</code> associated with the <code>code</code> provided, or <code>null</code> if not defined.
   * @param code an optimization level code.
   * @return the associated <code>XML2CSVOptimization</code>, or <code>null</code>.
   */
  public static XML2CSVOptimization parse(String code)
  {
    XML2CSVOptimization result = null;
    XML2CSVOptimization[] values = XML2CSVOptimization.values();
    for (int i = 0; i < values.length; i++)
    {
      XML2CSVOptimization oneOpt = values[i];
      if (oneOpt.getCode().equals(code))
      {
        result = oneOpt;
        break;
      }
    }
    return result;
  }
}
