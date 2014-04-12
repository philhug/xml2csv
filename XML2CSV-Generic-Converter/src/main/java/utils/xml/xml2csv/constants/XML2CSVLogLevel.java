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
 * Enumeration class listing the XML2CSV message log levels available, that is:<br>
 * <ul>
 * <li><code>DEBUG3</code> for an extra debug message displayed during execution (debug degree 3, alias DEBUG++);
 * <li><code>DEBUG2</code> for an extra debug message displayed during execution (debug degree 2, alias DEBUG+);
 * <li><code>DEBUG</code> for a debug message displayed during execution (default debug degree 1);
 * <li><code>INFO</code> for a regular information message displayed during execution;
 * <li><code>VERBOSE</code> for an extra progression message displayed during execution;
 * <li><code>WARNING</code> for a warning message displayed during execution;
 * <li><code>ERROR</code> for an error message displayed during execution;
 * <li><code>FATAL</code> for an error message displayed during execution which will trigger immediate program termination.
 * </ul>
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public enum XML2CSVLogLevel
{
  /** <code>DEBUG3</code> level (alias DEBUG++). */
  DEBUG3("DEBUG3", "Debug message - degree 3", 3),

  /** <code>DEBUG2</code> level (alias DEBUG+). */
  DEBUG2("DEBUG2", "Debug message - degree 2", 2),

  /** <code>DEBUG</code> level. */
  DEBUG("DEBUG", "Debug message", 1),

  /** <code>INFO</code> level. */
  INFO("INFO", "Information message", 1),

  /** <code>VERBOSE</code> level. */
  VERBOSE("VERBOSE", "Verbose logging", 1),

  /** <code>WARNING</code> level. */
  WARNING("WARNING", "Warning message", 1),

  /** <code>ERROR</code> level. */
  ERROR("ERROR", "Error message", 1),

  /** <code>FATAL</code> level. */
  FATAL("FATAL", "Fatal message", 1);

  /** Attribute recording the code of the current enumeration occurrence. */
  private final String code;

  /** Attribute recording the label of the current enumeration occurrence. */
  private final String label;

  /** Attribute recording the degree of the current enumeration occurrence. */
  private final int degree;

  /**
   * <code>XML2CSVLogLevel</code> constructor.
   * @param code the <code>XML2CSVLogLevel</code> code.
   * @param lab the <code>XML2CSVLogLevel</code> label.
   * @param degree the <code>XML2CSVLogLevel</code> degree.
   */
  private XML2CSVLogLevel(String code, String lab, int degree)
  {
    this.code = code;
    this.label = lab;
    this.degree = degree;
  }

  /**
   * Getter which returns the logging level code.
   * @return the logging level code.
   */
  public String getCode()
  {
    return this.code;
  }

  /**
   * Getter which returns the logging level label.
   * @return the logging level label.
   */
  public String getLabel()
  {
    return this.label;
  }

  /**
   * Getter which returns the logging level degree.
   * @return the logging level degree.
   */
  public int getDegree()
  {
    return degree;
  }

  /**
   * Returns the <code>XML2CSVLogLevel</code> associated with the <code>code</code> provided, or <code>null</code> if not defined.
   * @param code a log level code.
   * @return the associated <code>XML2CSVLogLevel</code>, or <code>null</code>.
   */
  public static XML2CSVLogLevel parse(String code)
  {
    XML2CSVLogLevel result = null;
    XML2CSVLogLevel[] values = XML2CSVLogLevel.values();
    for (int i = 0; i < values.length; i++)
    {
      XML2CSVLogLevel oneLogLvl = values[i];
      if (oneLogLvl.getCode().equals(code))
      {
        result = oneLogLvl;
        break;
      }
    }
    return result;
  }
}
