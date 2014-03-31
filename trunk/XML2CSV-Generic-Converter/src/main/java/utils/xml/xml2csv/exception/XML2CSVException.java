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
package utils.xml.xml2csv.exception;

/**
 * Superclass of all XML2CSV generic generation exceptions.
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public class XML2CSVException extends Exception
{
  // For serialization.
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new <code>XML2CSVException</code> instance with the specified detail message.
   * @param message the exception message.
   */
  public XML2CSVException(String message)
  {
    super(message);
  }

  /**
   * Constructs a new <code>XML2CSVException</code> instance with the specified detail message and cause.
   * @param message the exception message.
   * @param cause the exception cause.
   */
  public XML2CSVException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
