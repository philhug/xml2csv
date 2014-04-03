/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * Contributors:
 * IBM Corporation - initial API and implementation
 * L. Popieul - minor source adaptations and additional comments
 *******************************************************************************/
package utils.xml.xml2csv;

import utils.xml.xml2csv.constants.XML2CSVMisc;

/**
 * Utility class which prepares strings by escaping the necessary characters for proper XML/CSV handling:<br>
 * <ul>
 * <li>XML: escape of XML 1.0 "<i>character entities</i>" in strings;
 * <li>CSV: escape by means of surrounding double quotes.
 * </ul>
 */
class EscapeUtils
{
  /**
   * Replaces in a input <code>String</code> every occurrence of the XML 1.0 "<i>character entities</i>" (namely: &, <, >, ',
   * " - see <a href="http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML">here</a>) by an escape character sequence.<br>
   * Replaces tabs and non breaking spaces with spaces as well.
   * @param value the original <code>String</code>, which may not be <code>null</code>.
   * @return the escaped <code>String</code>.
   */
  public static String escapeXML10SpecialChars(String value)
  {
    return escapeXML10Chars(value, false, false, false, false);
  }

  /**
   * Replaces in a input <code>String</code> every occurrence of the XML 1.0 "<i>character entities</i>" (namely: &, <, >, ',
   * " - see <a href="http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML">here</a>) by an escape character sequence.<br>
   * Leaves tabs and non breaking spaces unchanged.
   * @param value the original <code>String</code>, which may not be <code>null</code>.
   * @return the escaped <code>String</code>.
   */
  public static String escapeXML10Chars(String value)
  {
    return escapeXML10Chars(value, false, false, true, true);
  }

  /**
   * Escapes ampersands in an input <code>String</code>.
   * @param value the original <code>String</code>, which may not be <code>null</code>.
   * @return the escaped <code>String</code>.
   */
  public static String escapeAmpersand(String value)
  {
    return value.replaceAll("&", "&amp;"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Replaces in a input <code>String</code> every occurrence of the XML 1.0 "<i>character entities</i>" (namely: &, <, >, ',
   * " - see <a href="http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML">here</a>) by an escape character sequence.<br>
   * Might leave ' and " unchanged depending on the invocation parameters.<br>
   * Replaces tabs and non breaking spaces with spaces or leave them unchanged, depending on the invocation parameters.
   * @param input the original <code>String</code>, which may not be <code>null</code>.
   * @param leaveApos <code>true</code> to leave simple quotes (apostrophes) unchanged (<code>'</code> instead of <code>&</code><code>apos;</code>).
   * @param leaveQuot <code>true</code> to leave double quotes unchanged (<code>"</code> instead of <code>&</code><code>quot;</code>).
   * @param leaveTab <code>true</code> to leave tabs unchanged (a tab is replaced by default by a blank character).
   * @param leaveNbsp <code>true</code> to leave non breaking spaces unchanged (a non breaking space is replaced by default by a blank character).
   * @return the escaped <code>String</code>.
   */
  public static String escapeXML10Chars(String input, boolean leaveApos, boolean leaveQuot, boolean leaveTab, boolean leaveNbsp)
  {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < input.length(); i++)
    {
      char c = input.charAt(i);

      switch (c)
      {
        case '&':
          buf.append("&amp;"); //$NON-NLS-1$
          break;
        case '<':
          buf.append("&lt;"); //$NON-NLS-1$
          break;
        case '>':
          buf.append("&gt;"); //$NON-NLS-1$
          break;
        case '\'':
          if (leaveApos == true) buf.append(c);
          else
            buf.append("&apos;"); //$NON-NLS-1$
          break;
        case '\"':
          if (leaveQuot == true) buf.append(c);
          else
            buf.append("&quot;"); //$NON-NLS-1$
          break;
        case 160:
          if (leaveNbsp == true) buf.append(c);
          else
            buf.append(" "); //$NON-NLS-1$
          break;
        case '\t':
          if (leaveTab == true) buf.append(c);
          else
            buf.append(' ');
          break;
        default:
          buf.append(c);
          break;
      }
    }
    return buf.toString();
  }

  /**
   * Puts surrounding double quotes at the beginning and the end of an input <code>String</code> for secure CSV output if:<br>
   * <ul>
   * <li>the input string contains the current CSV field separator;
   * <li>the input string contains a double quote (which has to be explicitly doubled);
   * <li>the input string contains a CR, a LF, or the current OS line separator.
   * <ul>
   * Leaves the input <code>String</code> unchanged otherwise.
   * @param input the original <code>String</code>, which may not be <code>null</code>.
   * @param fieldSeparator the current CSV field separator.
   * @return the input <code>String</code> surrounded by double quotes, or left unchanged.
   */
  public static String escapeCSVChars(String input, String fieldSeparator)
  {
    String result = input;
    boolean surround = false;
    if (input.indexOf(fieldSeparator) != -1) surround = true;
    if (input.indexOf(XML2CSVMisc.LINE_SEPARATOR) != -1) surround = true;
    if (input.indexOf("\r") != -1) surround = true;
    if (input.indexOf("\n") != -1) surround = true;
    if (surround == true)
    {
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < input.length(); i++)
      {
        char c = input.charAt(i);
        if (c == '"')
        {
          buf.append(c); // A double quote in the input string is doubled to be interpreted as a plain character and not a delimiter.
          buf.append(c);
        }
        else
          buf.append(c);
      }
      result = "\"" + buf.toString() + "\"";
    }
    return result;
  }
}
