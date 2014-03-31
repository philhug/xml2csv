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

/**
 * Utility class for preparing strings for display by escaping the necessary characters.<br>
 * Comes in handy in order to escape XML 1.0 "<i>character entities</i>" in strings properly.
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
}
