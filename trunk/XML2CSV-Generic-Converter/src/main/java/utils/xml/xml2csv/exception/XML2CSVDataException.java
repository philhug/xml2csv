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
 * XML2CSV data exception.<br>
 * Exception thrown by an XML2CSV generic generator to notify that data extraction from one of the XML input files failed.
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public class XML2CSVDataException extends XML2CSVException
{
  // For serialization.
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new <code>XML2CSVDataException</code> instance with the specified detail message.
   * @param message the exception message.
   */
  public XML2CSVDataException(String message)
  {
    super(message);
  }

  /**
   * Constructs a new <code>XML2CSVDataException</code> instance with the specified detail message and cause.
   * @param message the exception message.
   * @param cause the exception cause.
   */
  public XML2CSVDataException(String message, Throwable cause)
  {
    super(message, cause);
  }
}
