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

import java.io.File;
import java.io.FilenameFilter;

/**
 * Custom file filter which selects files depending on their names.
 * @author L. Popieul (lochrann@rocketmail.com)
 */
class InputFilenameFilter implements FilenameFilter
{
  /** Selected file extension. */
  private String extension = null;

  /**
   * <code>InputFilenameFilter</code> private constructor.<br>
   * Private default constructor; forces usage of a constructor with at least one parameter.
   */
  private InputFilenameFilter()
  {
  }

  /**
   * <code>InputFilenameFilter</code> constructor.<br>
   * Builds a filename filter which selects files with a certain extension.
   * @param extension the selected file extension.
   */
  public InputFilenameFilter(String extension)
  {
    this();
    if (extension.startsWith(".") == false) this.extension = "." + extension.toLowerCase();
    else
      this.extension = extension.toLowerCase();
  }

  @Override
  public boolean accept(File dir, String name)
  {
    boolean result = false;
    if (name.toLowerCase().endsWith(extension)) result = true;
    return result;
  }
}
