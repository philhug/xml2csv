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

import org.apache.commons.logging.Log;

import utils.xml.xml2csv.constants.XML2CSVLogLevel;
import utils.xml.xml2csv.constants.XML2CSVMisc;

/**
 * This internal logging class is responsible for message logging/display for all other XML2CSV classes.<br>
 * Its purpose is to make it possible to format and display properly log messages to the console even if no standard Apache/Jakarta
 * {@link utils.xml.xml2csv.XML2CSVLoggingFacade#log log} instance (Log4J, SLF4J, ...) was defined.<br>
 * The XML2CSV logging behavior is tailored to the current {@link utils.xml.xml2csv.XML2CSVLoggingFacade#MUTE_MODE}, {@link utils.xml.xml2csv.XML2CSVLoggingFacade#DEBUG_MODE} and
 * {@link utils.xml.xml2csv.XML2CSVLoggingFacade#VERBOSE_MODE} values.<br>
 * When {@link utils.xml.xml2csv.XML2CSVLoggingFacade#DEBUG_MODE} is activated the {@link utils.xml.xml2csv.XML2CSVLoggingFacade#debugDegree debug degree} might be set to a higher
 * degree (<code>2</code> or <code>3</code>) instead of the default (<code>1</code>) to have extra debug messages displayed.<br>
 * If an Apache/Jakarta {@link utils.xml.xml2csv.XML2CSVLoggingFacade#log log} object is provided it will drain all messages but if left <code>null</code> (its initial value)
 * all messages are sent to the console.<br>
 * <i>Please note that from a genuine Log4J <a href="http://juliusdavies.ca/logging.html">point of view</a> it is generally considered bad practice, but, because this program had
 * to compose with the fact that Apache/Jakarta logging is just an option, I had to make sure that some kind of basic console logging would always be available.</i>
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public abstract class XML2CSVLoggingFacade
{
  /**
   * MUTE mode indicator. When set to <code>true</code>, deactivates message logging (active muting overrides {@link utils.xml.xml2csv.XML2CSVLoggingFacade#DEBUG_MODE} and
   * {@link utils.xml.xml2csv.XML2CSVLoggingFacade#VERBOSE_MODE}).
   */
  public static boolean MUTE_MODE = false;

  /** DEBUG mode indicator. When set to <code>true</code>, activates debug message logging. */
  public static boolean DEBUG_MODE = false;

  /** VERBOSE mode indicator. When set to <code>true</code>, activates verbose message logging. */
  public static boolean VERBOSE_MODE = false;

  /** DEBUG degree. A higher value than <code>1</code> (the default) makes it possible to display additional debug messages in debug mode. */
  public static int debugDegree = 1;

  /**
   * Jakarta/Apache commons logging log instance. All messages are sent to the console when left <code>null</code> (initial value), and to the Jakarta/Apache log object when not
   * <code>null</code>.
   */
  public static Log log = null;

  /** Indicates if the disclaimer message was displayed or not. */
  private static boolean isDisclaimerDisplayed = false;

  /**
   * Logs a <code>message</code> prefixed by both the program display name and the <code>level</code> code provided, unless {@link utils.xml.xml2csv.XML2CSVLoggingFacade#MUTE_MODE}
   * is set or unless it is a {@link utils.xml.xml2csv.constants.XML2CSVLogLevel#VERBOSE}/{@link utils.xml.xml2csv.constants.XML2CSVLogLevel#DEBUG} message and the corresponding
   * mode has not been activated.<br>
   * Sends a terminal <i>line feed</i> (or the local equivalent OS sequence) at the end of the message as well.
   * @param level the log level associated with the message.
   * @param message the message to display.
   */
  public static void log(XML2CSVLogLevel level, String message)
  {
    log(level, XML2CSVMisc.DISPLAY_CLASS_NAME, message, true);
  }

  /**
   * Logs a <code>message</code> prefixed by both the <code>prefix</code> and the <code>level</code> code provided, unless {@link utils.xml.xml2csv.XML2CSVLoggingFacade#MUTE_MODE}
   * is set or unless it is a {@link utils.xml.xml2csv.constants.XML2CSVLogLevel#VERBOSE}/{@link utils.xml.xml2csv.constants.XML2CSVLogLevel#DEBUG} message and the corresponding
   * mode has not been activated.<br>
   * Sends a terminal <i>line feed</i> (or the local equivalent OS sequence) at the end of the message as well.
   * @param level the log level associated with the message.
   * @param prefix a message prefix, or <code>null</code> to log a message without prefix.
   * @param message the message to display.
   */
  public static void log(XML2CSVLogLevel level, String prefix, String message)
  {
    log(level, prefix, message, true);
  }

  /**
   * Logs a <code>message</code> prefixed by both the <code>prefix</code> and the <code>level</code> code provided, unless {@link utils.xml.xml2csv.XML2CSVLoggingFacade#MUTE_MODE}
   * is set or unless it is a {@link utils.xml.xml2csv.constants.XML2CSVLogLevel#VERBOSE}/{@link utils.xml.xml2csv.constants.XML2CSVLogLevel#DEBUG} message and the corresponding
   * mode has not been activated.<br>
   * Sends a terminal <i>line feed</i> (or the local equivalent OS sequence) at the end of the message if parameter <code>lf</code> is set to <code>true</code>.
   * @param level the log level associated with the message.
   * @param prefix a message prefix, or <code>null</code> to log a message without prefix.
   * @param message the message to display.
   * @param lf <code>true</code> to step to the next line.
   */
  public static void log(XML2CSVLogLevel level, String prefix, String message, boolean lf)
  {
    if (MUTE_MODE == false)
    {
      if (message != null)
      {
        if (isDisclaimerDisplayed == false)
        {
          isDisclaimerDisplayed = true;
          log(null, null, XML2CSVMisc.EMPTY_STRING, true);
          log(null, null, XML2CSVMisc.DISCLAIMER1, true);
          log(null, null, XML2CSVMisc.DISCLAIMER2, true);
          log(null, null, XML2CSVMisc.EMPTY_STRING, true);
          log(null, XML2CSVMisc.DISPLAY_CLASS_NAME, "work started.", true);
        }
        boolean display = true;
        if ((level == XML2CSVLogLevel.VERBOSE) && (VERBOSE_MODE == false)) display = false;
        if ((level == XML2CSVLogLevel.DEBUG) && (DEBUG_MODE == false)) display = false;
        if ((level == XML2CSVLogLevel.DEBUG2) && ((DEBUG_MODE == false) || (debugDegree < 2))) display = false;
        if ((level == XML2CSVLogLevel.DEBUG3) && ((DEBUG_MODE == false) || (debugDegree < 3))) display = false;
        if (display == true)
        {
          StringBuffer msg = new StringBuffer();
          if (prefix != null)
          {
            msg.append(prefix);
            msg.append(": ");
          }
          if ((level != null) && (log == null)) // The level, if any, is not appended to the message itself if a Log4J logger is used because it will have its own gravity level.
          {
            msg.append(level.getCode());
            msg.append(": ");
          }
          msg.append(message);
          if (log == null)
          {
            // Default built-in console logging.
            if (lf == true) System.out.println(msg.toString());
            else
              System.out.print(msg.toString());
          }
          else
          {
            // Standard Apache Commons logging.
            if (level == null) log.info(msg.toString());
            else if (level == XML2CSVLogLevel.DEBUG3) log.trace(msg.toString()); // The finest debug degrees correspond to the Log4J trace level.
            else if (level == XML2CSVLogLevel.DEBUG2) log.trace(msg.toString());
            else if (level == XML2CSVLogLevel.DEBUG) log.debug(msg.toString());
            else if (level == XML2CSVLogLevel.INFO) log.info(msg.toString());
            else if (level == XML2CSVLogLevel.VERBOSE) log.info(msg.toString());
            else if (level == XML2CSVLogLevel.WARNING) log.warn(msg.toString());
            else if (level == XML2CSVLogLevel.ERROR) log.error(msg.toString());
            else if (level == XML2CSVLogLevel.FATAL) log.fatal(msg.toString());
          }
        }
      }
    }
  }
}
