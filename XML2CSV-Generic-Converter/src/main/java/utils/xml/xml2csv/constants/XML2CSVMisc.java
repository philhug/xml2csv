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
 * A class regrouping miscellaneous static constants.
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public abstract class XML2CSVMisc
{
  /** The official name of the UTF8 character encoding. */
  public static final String UTF8 = "UTF-8";

  /** An empty string. */
  public static final String EMPTY_STRING = "";

  /** The initial disclaimer message 1/2. */
  public static final String DISCLAIMER1 = "XML2CSV-Generic-Converter - Copyright 2014 Laurent Popieul.";

  /** The initial disclaimer message 2/2. */
  public static final String DISCLAIMER2 = "This program comes with ABSOLUTELY NO WARRANTY. This is free software, and you are welcome to redistribute it under certain conditions.";

  /** A beautiful line of dashes. */
  public static final String LINE = "-------------------------------------------------------------------------------------------";

  /** The extension of XML files. */
  public static final String XML = "xml";

  /** The extension of CSV files. */
  public static final String CSV = "csv";

  /** The CSV default field separator. */
  public static final String DEFAULT_FIELD_SEPARATOR = ";";

  /** The default cutoff limit. */
  public static final long DEFAULT_CUTOFF_LIMIT = 1024L;

  /** The class name to display in messages. */
  public static final String DISPLAY_CLASS_NAME = "XML2CSV";

  /** The expected Log4J logger. */
  public static final String LOG4J_LOGGER = "XML2CSV-Generic-Converter";

  /** The default Log4J configuration file. */
  public static final String DEFAULT_LOG4J_PROPERTY_FILE = "xml2csvlog4j.properties";

  /** The character sequence, which, on the OS, stands for a "Next line" (on DOS CRLF, on UNIX LF). */
  public static String LINE_SEPARATOR = System.getProperty("line.separator");

  /** The character sequence, which, on the OS, stands for a file separator (on DOS "\", on UNIX "/"). */
  public static String FILE_SEPARATOR = System.getProperty("file.separator");

  /** The starting directory. */
  public static String START_DIRECTORY = System.getProperty("user.dir");

  /** The default blend CSV output file name. */
  public static final String DEFAULT_BLEND_OUTPUT_FILENAME = "output.csv";

  /** The displayed alias name of the default name space. */
  public static final String DEFAULT_NAMESPACE_ALIAS = "{default}";
}
