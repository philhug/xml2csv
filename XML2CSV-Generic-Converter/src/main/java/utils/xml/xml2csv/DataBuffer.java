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

import utils.xml.xml2csv.constants.XML2CSVCardinality;
import utils.xml.xml2csv.constants.XML2CSVLogLevel;
import utils.xml.xml2csv.constants.XML2CSVMisc;
import utils.xml.xml2csv.constants.XML2CSVOptimization;
// import utils.xml.xml2csv.constants.XML2CSVType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.xml.sax.SAXException;

/**
 * This class buffers the data from tracked XML elements read from an XML input file (see method {@link utils.xml.xml2csv.DataBuffer#addContent(int,String) addContent}).<br>
 * Description of the XML tracked elements is represented by the {@link utils.xml.xml2csv.ElementsDescription ElementsDescription} object provided to the class instance
 * constructor, and the buffer adapts automatically in order to fit that description.<br>
 * One new line is added to the buffer for each tracked XML element content added.<br>
 * The class records the opening/closing of the parents of tracked XML elements as well (see methods {@link utils.xml.xml2csv.DataBuffer#recordTrackedParentOpening(String)
 * recordTrackedParentOpening} and {@link utils.xml.xml2csv.DataBuffer#recordTrackedParentClosing(String) recordTrackedParentClosing}).<br>
 * One new line is added to the buffer for each parent reference too.<br>
 * When the buffer is complete and consistent (devised from <i><u>outside</u></i> the class) two special {@link utils.xml.xml2csv.DataBuffer#optimizeV1() optimizationV1} and
 * {@link utils.xml.xml2csv.DataBuffer#optimizeV2() optimizationV2} methods make it possible to have data packed depending on the chosen optimization
 * {@link utils.xml.xml2csv.constants.XML2CSVOptimization flavor}.<br>
 * One final {@link utils.xml.xml2csv.DataBuffer#flush() flush} method makes it possible to send the buffer data to the expected CSV output file through
 * the CSV output writer {@link utils.xml.xml2csv.OutputWriterFacade facade} object provided to the class instance constructor.<br>
 * The buffer is then ready to accumulate and process new data (a buffer flush implies a buffer {@link utils.xml.xml2csv.DataBuffer#reset() reset}).
 * @author L. Popieul (lochrann@rocketmail.com)
 */
class DataBuffer
{
  /** CSV output file facade. */
  private OutputWriterFacade outputWriterFacade = null;

  /** The chosen CSV field separator. */
  private String fieldSeparator = null;

  /** The chosen optimization level. */
  private XML2CSVOptimization level = null;

  /** XML data tracking: the ordered list of tracked leaf element XPaths in the input XML files. */
  private static String[] trackedLeafElementXPaths = null;

  /** XML data tracking: the ordered list of tracked leaf element parent XPaths in the input XML files. */
  private static String[] trackedLeafElementParentXPaths = null;

  /** XML data tracking: the ordered list of tracked leaf element cardinalities in the XML input files. */
  private static XML2CSVCardinality[] trackedLeafElementCardinalities = null;

  /** XML data tracking: the tracked leaf element description from which all tracking information comes from. */
  private ElementsDescription trackedLeafElementsDescription = null;

  // XML data tracking: the ordered list of tracked leaf element types in the XML input files.
  // private static Type[] trackedLeafElementTypes = null;

  /**
   * The actual data buffer shaped as an array list of one dimension string arrays, each of them representing a line. A line will hold as many columns as there are tracked
   * elements plus one column to record tracked element's parent element openings plus plus another one to record tracked element's parent element closings. Before
   * optimization/packing, each line holds one unique non empty cell.
   */
  private ArrayList<String[]> buffer = new ArrayList<String[]>();

  /**
   * <code>DataBuffer</code> constructor.
   * @param outputWriterFacade a non null <code>OutputWriterFacade</code> in charge of actual output, expected {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} when the
   *        actual data buffering starts.
   * @param fieldSeparator the field separator to use in CSV output files, or <code>null</code> to use the default field separator.
   * @param trackedLeafElementsDescription description of the tracked XML elements handled by this buffer.
   * @param level the chosen <code>XML2CSVOptimization</code> level (defaults to <code>STANDARD</code> it left <code>null</code>).
   */
  protected DataBuffer(OutputWriterFacade outputWriterFacade, String fieldSeparator, ElementsDescription trackedLeafElementsDescription, XML2CSVOptimization level)
  {
    super();

    this.outputWriterFacade = outputWriterFacade;

    if (fieldSeparator != null) this.fieldSeparator = fieldSeparator;
    else
      this.fieldSeparator = XML2CSVMisc.DEFAULT_FIELD_SEPARATOR;

    if (level != null) this.level = level;
    else
      this.level = XML2CSVOptimization.STANDARD;

    DataBuffer.trackedLeafElementXPaths = trackedLeafElementsDescription.getElementsXPaths();
    DataBuffer.trackedLeafElementParentXPaths = trackedLeafElementsDescription.getElementsParentXPaths();
    DataBuffer.trackedLeafElementCardinalities = trackedLeafElementsDescription.getElementsCardinalities();
    // DataBuffer.trackedLeafElementTypes = trackedLeafElementsDescription.getElementsTypes();

    this.trackedLeafElementsDescription = trackedLeafElementsDescription;
  }

  // ================================================================================
  // Private helper methods
  // ================================================================================

  /**
   * Builds a CSV output line with the data currently available at the specified buffer <code>index</code> line, or returns <code>null</code> if the line holds no displayable
   * data and should be discarded.
   * @param index the buffer line index.
   * @return a formatted CSV line ready to be sent to the CSV output file, or <code>null</code>.
   */
  private String buildCSVOutputLine(int index)
  {
    // The tracked elements go from index 0 to trackedLeafElementXPaths.length-1. If the field at index trackedLeafElementXPaths.length or trackedLeafElementXPaths.length+1
    // is not empty it means this is a special line dedicated to parent opening/closing reference with no actual tracked element data which should be discarded.
    String[] line = buffer.get(index);
    if ((line[trackedLeafElementXPaths.length] != null) || (line[trackedLeafElementXPaths.length + 1] != null))
    {
      // This is a special line dedicated to parent opening/closing reference which should be discarded.
      return null;
    }
    else
    {
      // This is a regular line which can be shaped for output unless all of its fields are empty (might happen after optimization).
      boolean emptyLine = true;
      String field = null;
      StringBuffer result = new StringBuffer();
      for (int i = 0; i < trackedLeafElementXPaths.length - 1; i++)
      {
        field = line[i];
        if (field != null)
        {
          result.append(field);
          emptyLine = false;
        }
        result.append(fieldSeparator);
      }
      field = line[trackedLeafElementXPaths.length - 1];
      if (field != null)
      {
        result.append(field);
        emptyLine = false;
      }
      if (emptyLine == false) return result.toString();
      else
        return null;
    }
  }

  /**
   * Displays the current buffer content (for debug purpose).
   */
  private void displayBufferContent()
  {
    // Computes the per-column max data lengths for a nice display.
    int[] maxLengths = new int[trackedLeafElementXPaths.length + 2];
    for (int i = 0; i < maxLengths.length; i++)
      maxLengths[i] = 0;
    for (int i = 0; i < buffer.size(); i++)
    {
      String[] line = buffer.get(i);
      for (int j = 0; j < line.length; j++)
      {
        if ((line[j] != null) && (line[j].length() > maxLengths[j])) maxLengths[j] = line[j].length();
      }
    }
    // Displays each buffer line to the console.
    String debugLine = buildDebugBufferLine(-1, maxLengths);
    XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "\t            " + debugLine);
    for (int i = 0; i < buffer.size(); i++)
    {
      debugLine = buildDebugBufferLine(i, maxLengths);
      StringBuffer formattedDebugLine = new StringBuffer();
      formattedDebugLine.append("\tline <");
      String is = Integer.toString(i);
      for (int k = 0; k < (3 - is.length()); k++)
        formattedDebugLine.append(" ");
      formattedDebugLine.append(Integer.toString(i));
      formattedDebugLine.append(">: ");
      formattedDebugLine.append(debugLine);
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, XML2CSVMisc.DISPLAY_CLASS_NAME, formattedDebugLine.toString());
    }
  }

  /**
   * Builds a debug oriented string representation of the <code>index</code>-th buffer line.
   * @param index the buffer line index, or <code>-1</code> to display a header line with the column count instead.
   * @param maxLengths the max data length for each column (useful for indentation).
   * @return a formatted string representation of the <code>index</code>-th buffer line, or a header line if <code>-1</code> was provided for <code>index</code> value.
   */
  private String buildDebugBufferLine(int index, int[] maxLengths)
  {
    StringBuffer result = new StringBuffer();
    if (index == -1)
    {
      // We build a plain header line with the column count for better reading.
      for (int i = 0; i < trackedLeafElementXPaths.length; i++)
      {
        String is = Integer.toString(i);
        // We have (maxLengths[i] + 2) characters to display i (the i-th max length plus one character for "[" plus one for "]").
        if (is.length() <= maxLengths[i] + 2)
        {
          // Enough room to display the column number.
          result.append(is);
          for (int k = 0; k < (maxLengths[i] + 2 - is.length()); k++)
            result.append(" ");
        }
        else
        {
          // Not enough room to display the column number: we display an "X" instead.
          result.append("X");
          for (int k = 0; k < (maxLengths[i] + 2 - 1); k++)
            result.append(" ");
        }
      }
    }
    else
    {
      // The tracked elements go from index 0 to trackedLeafElementXPaths.length-1.
      // The field at index trackedLeafElementXPaths.length, if any, indicates that a tracked element's parent XPath was opened.
      // The field at index trackedLeafElementXPaths.length+1 (the last index of a line), if any, indicates that a tracked element's parent XPath was closed.
      String[] line = buffer.get(index);
      String field = null;
      for (int i = 0; i < line.length; i++)
      {
        field = line[i];
        int fieldLength = (field == null) ? 0 : field.length();
        result.append("[");
        for (int k = 0; k < (maxLengths[i] - fieldLength); k++)
          result.append(" ");
        if (fieldLength != 0) result.append(field);
        result.append("]");
        if (i == trackedLeafElementXPaths.length - 1) result.append("|");
      }
    }
    return result.toString();
  }

  /**
   * Sends a string to the CSV output file.
   * @param s the string to send.
   * @throws <code>SAXException</code> in case of error.
   */
  private void emit(String s) throws SAXException
  {
    try
    {
      outputWriterFacade.write(s);
      outputWriterFacade.flush();
    }
    catch (IOException ioe)
    {
      throw new SAXException("I/O error", ioe);
    }
  }

  /**
   * Sends a line separator to the CSV output file (CR-LF, CR or LF depending on the underneath OS).
   * @throws <code>SAXException</code> in case of error.
   */
  private void ls() throws SAXException
  {
    try
    {
      outputWriterFacade.write(XML2CSVMisc.LINE_SEPARATOR);
    }
    catch (IOException ioe)
    {
      throw new SAXException("I/O error", ioe);
    }
  }

  // ================================================================================
  // Public/protected getters and setters
  // ================================================================================

  /**
   * Adds the <code>content</code> of a tracked XML element to a new buffer line, at the corresponding column <code>index</code>.<br>
   * Data will be packed later on at optimization phase.
   * @param index the column index where the data should be added in the new buffer line.
   * @param content the XML element content to add at the related index.
   */
  public void addContent(int index, String content)
  {
    String[] line = new String[trackedLeafElementXPaths.length + 2]; // trackedLeafElementXPaths.length tracked XML elements plus 2 columns for parent opening/closing.
    line[index] = content;
    buffer.add(line);
  }

  /**
   * Adds the <code>contents</code> of tracked XML elements to a new buffer line, at the corresponding column <code>indexes</code>.<br>
   * Data will be packed later on at optimization phase.
   * @param indexes the column indexes where the data should be added in the new buffer line.
   * @param contents the XML element contents to add at the related indexes.
   */
  public void addContents(int[] indexes, String[] contents)
  {
    String[] line = new String[trackedLeafElementXPaths.length + 2]; // trackedLeafElementXPaths.length tracked XML elements plus 2 columns for parent opening/closing.
    for (int i = 0; i < indexes.length; i++)
    {
      int index = indexes[i];
      String content = contents[i];
      line[index] = content;
    }
    buffer.add(line);
  }

  /**
   * Records the opening of a parent element of a tracked XML element (information needed at optimization phase).
   * @param parentXpath the parent element XPath.
   */
  public void recordTrackedParentOpening(String parentXpath)
  {
    // A tracked element's parent opening is recorded at index trackedLeafElementXPaths.length.
    String[] line = new String[trackedLeafElementXPaths.length + 2];
    line[trackedLeafElementXPaths.length] = parentXpath;
    buffer.add(line);
  }

  /**
   * Records the closing of a parent element of a tracked XML element (information needed at optimization phase).
   * @param parentXpath the parent element XPath.
   */
  public void recordTrackedParentClosing(String parentXpath)
  {
    // A tracked element's parent closing is recorded at index trackedLeafElementXPaths.length+1.
    String[] line = new String[trackedLeafElementXPaths.length + 2];
    line[trackedLeafElementXPaths.length + 1] = parentXpath;
    buffer.add(line);
  }

  /**
   * Resets this data buffer for further use.<br>
   * Pending data which have not yet been {@link utils.xml.xml2csv.DataBuffer#flush() flushed} to the CSV output file will be lost.
   */
  public void reset()
  {
    if (isEmpty() == false) buffer.clear();
  }

  /**
   * Flushes this data buffer to the CSV output file, then {@link utils.xml.xml2csv.DataBuffer#reset() resets} it.
   * @throws <code>SAXException</code> in case of error.
   */
  public void flush() throws SAXException
  {
    if (isEmpty() == false)
    {
      for (int i = 0; i < buffer.size(); i++)
      {
        // Null is returned for each special line recording tracked element's parent opening/closing which should be discarded silently.
        String line = buildCSVOutputLine(i);
        if (line != null)
        {
          emit(line);
          ls();
        }
      }
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "<" + buffer.size() + "> line buffer flushed to CSV output.");
      reset();
    }
  }

  /**
   * Tests if the buffer is empty or not.
   * @return <code>true</code> if the buffer is empty and <code>false</code> otherwise.
   */
  public boolean isEmpty()
  {
    boolean result = true;
    if (buffer.size() > 0) result = false;
    return result;
  }

  /**
   * Optimizes the data currently buffered, expected complete and consistent.
   * Performs an {@link utils.xml.xml2csv.constants.XML2CSVOptimization#EXTENSIVE_V2 EXTENSIVE_V2} buffer line subset optimization, which is both a variant of <code>STANDARD</code>
   * and <code>EXTENSIVE_V1</code>.<br>
   * Optimization ends when the last buffer line subset has been processed (when the last line block enclosed by tracked element's parent opening/closing has been performed).
   * @return true if the optimization changed something in the buffer.
   * @throws <code>SAXException</code> in case of error.
   */
  public boolean optimizeV2() throws SAXException
  {
    boolean atLeastOneStandardPackDone = false; // Set to true if enhanced standard optimization changed something in the buffer.
    boolean atLeastOneExtensivePackingDone = false; // Set to true if extensive optimization changed something in the buffer.
    if ((isEmpty() == false) && (level == XML2CSVOptimization.EXTENSIVE_V2))
    {
      if (XML2CSVLoggingFacade.DEBUG_MODE == true)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "<" + buffer.size() + "> lines in buffer before optimization. Original content:");
        displayBufferContent();
      }
      // The main loop moves through the buffer from the beginning to the end (the inf index is incremented accordingly).
      // Each time a tracked parent opening P is met (in column trackedLeafElementXPaths.length) the next closing index of the same kind is searched
      // (in column trackedLeafElementXPaths.length+1): one optimization sub routine is then triggered between the corresponding inf and sup indices of the buffer,
      // and all mono-occurrence (ZERO_TO_ONE or ONE_TO_ONE) tracked elements which have P as their parent are packed on the same line x (leaving a certain amount of blank
      // lines between line x+1 and sup which are left as is because they will be silently discarded when the buffer will be echoed to the output).
      // In addition, all mono-occurrence (ZERO_TO_ONE or ONE_TO_ONE) tracked elements which have P as their ancestor and are chained by intermediate mono-occurrence elements
      // back to P are treated just like direct mono-occurrence children of P for enhanced standard packing behavior.
      // If an extensive optimization is expected then:
      // - the data of line x is copied back into all non empty lines containing a field in connection with a multi-occurrence (ZERO_TO_MANY or ONE_TO_MANY) tracked element
      // which has P as its parent, plus all non empty lines containing a field in connection with a mono-occurrence (ZERO_TO_ONE or ONE_TO_ONE) or multi-occurrence
      // (ZERO_TO_MANY/ONE_TO_MANY) tracked element which has P as ancestor and is connected to P by at least one intermediate multi-occurrence element.
      // - line x is emptied in order to make it disappear in the output if x has been copied back at least once.
      // When a sub routine ends the main loop resumes at inf+1, and when the buffer end is reached the optimization is phase is finished.
      int inf = 0;
      while (inf < buffer.size())
      {
        boolean packingChangedBuffer = false;
        boolean packingEnoughForExtensivePack = false;
        boolean extensivePackingChangedBuffer = false;
        String[] infLine = buffer.get(inf);
        String trackedParentOpening = infLine[trackedLeafElementXPaths.length];
        if (trackedParentOpening == null) inf++;
        else
        {
          // OK: a tracked parent opening was met. We look for the related tracked parent closing index.
          int sup = inf;
          boolean foundSup = false;
          while ((sup < buffer.size()) && (foundSup == false))
          {
            String[] supLine = buffer.get(sup);
            if (supLine[trackedLeafElementXPaths.length + 1] == null) sup++;
            else
            {
              if (trackedParentOpening.equals(supLine[trackedLeafElementXPaths.length + 1])) foundSup = true;
              else
                sup++;
            }
          }
          if (foundSup == false) // The related tracked parent closing index was not found!
          {
            // This can't be unless a nasty bug occurred. Each tracked parent opening should have its counterpart tracked parent closing unless the buffer is not consistent,
            // and if it happens it is not consistent then optimization is a nonsense. We raise an exception in order to avoid hazardous behavior.
            throw new SAXException("Optimization bug. Bad recording of tracked element's parent in the data buffer.");
          }
          // At this stage an optimization sub routine is triggered dealing with the buffer lines between the indices inf and sup.
          // All ZERO_TO_ONE/ONE_TO_ONE tracked elements which have trackedParentOpening as their parent are searched and if they are two or more
          // they can be packed on the same line (regular standard packing).
          ArrayList<Integer> selectedColums = new ArrayList<Integer>();
          for (int i = 0; i < trackedLeafElementXPaths.length; i++)
          {
            if ((trackedParentOpening.equals(trackedLeafElementParentXPaths[i]))
                && ((trackedLeafElementCardinalities[i] == XML2CSVCardinality.ONE_TO_ONE) || (trackedLeafElementCardinalities[i] == XML2CSVCardinality.ZERO_TO_ONE)))
            {
              // The element associated with column i is concerned.
              selectedColums.add(i);
            }
          }
          // Enhanced standard packing.
          // In addition to the previous regular packing candidates we add mono-occurrence (ZERO_TO_ONE or ONE_TO_ONE) tracked elements which have trackedParentOpening
          // as their ancestor (= which are related to trackedParentOpening) and are chained by intermediate mono-occurrence elements back to P for enhanced packing behavior.
          // Those intermediate mono-occurrence elements are erased from the parent list (columns trackedLeafElementXPaths.length and trackedLeafElementXPaths.length+1)
          // to make sure no standard packing will be attempted later on in another loop (job already done + trying to do it twice would fail selecting a multi-occurrence
          // element line for mono-occurrence x source {because of the copied mono-occurrence elements} and then trying to copy that multi-occurrence element line back
          // into its fellow multi-occurrence line, on a deadly collision course).
          LinkedList<String> eraseFromParentList = null;
          for (int i = 0; i < trackedLeafElementXPaths.length; i++)
          {
            HashMap<String, String[]> intermediateElements = trackedLeafElementsDescription.getIntermediateXPaths(trackedParentOpening, trackedLeafElementXPaths[i]);
            if (intermediateElements != null)
            {
              // The examined tracked element trackedLeafElementXPaths[i] and trackedParentOpening are related that is, trackedLeafElementXPaths[i] is a sub XPath
              // of trackedParentOpening. If it happens that all intermediary elements between them are mono-occurrence then we have a new candidate for enhanced standard packing.
              boolean extraPackCandidate = true;
              if (intermediateElements.keySet().isEmpty())
              {
                // Element trackedLeafElementXPaths[i] is a direct child of trackedParentOpening and was handled a few lines before during regular standard packing.
                // Enhanced packing deals only with elements which have trackedParentOpening as a real ancestor.
                extraPackCandidate = false;
                eraseFromParentList = null;
              }
              else
              {
                eraseFromParentList = new LinkedList<String>();
                Iterator<String> intermediateElementsIterator = intermediateElements.keySet().iterator();
                while (intermediateElementsIterator.hasNext())
                {
                  String intermediateElementXPath = intermediateElementsIterator.next();
                  eraseFromParentList.add(intermediateElementXPath);
                  String[] props = intermediateElements.get(intermediateElementXPath);
                  if (XML2CSVCardinality.isUnbounded(props[0]))
                  {
                    extraPackCandidate = false;
                    break;
                  }
                }
              }
              if (extraPackCandidate == true)
              {
                // All intermediate element XPaths between trackedLeafElementXPaths[i] and trackedParentOpening are mono-occurrence.
                // The element associated with column i is concerned by enhanced packing... If it is a mono-occurrence element itself.
                if ((trackedLeafElementCardinalities[i] == XML2CSVCardinality.ONE_TO_ONE) || (trackedLeafElementCardinalities[i] == XML2CSVCardinality.ZERO_TO_ONE)) selectedColums
                    .add(i);
                // Time to erase those intermediate element XPaths from the books (that is from the columns at index trackedLeafElementXPaths.length and
                // trackedLeafElementXPaths.length+1 in the buffer, between inf+1 and sup-1) in order to prevent loops based on them which would really do us no good.
                if ((eraseFromParentList != null) && (eraseFromParentList.size() > 0))
                {
                  for (int j = 0; j < eraseFromParentList.size(); j++)
                  {
                    String eraseMe = eraseFromParentList.get(j);
                    for (int k = inf + 1; k < sup + 1; k++)
                    {
                      String[] line = buffer.get(k);
                      String oneParentOpening = line[trackedLeafElementXPaths.length];
                      if ((oneParentOpening != null) && (oneParentOpening.equals(eraseMe))) line[trackedLeafElementXPaths.length] = null;
                      String oneParentClosing = line[trackedLeafElementXPaths.length + 1];
                      if ((oneParentClosing != null) && (oneParentClosing.equals(eraseMe))) line[trackedLeafElementXPaths.length + 1] = null;
                    }
                  }
                }
              }
            }
            else
            {
              // The examined tracked element trackedLeafElementXPaths[i] and trackedParentOpening are not related, that is, trackedLeafElementXPaths[i] is not a sub XPath
              // of trackedParentOpening. No way for trackedLeafElementXPaths[i] to be candidate for enhanced packing.
            }
          }
          // We go on if selectedColums.size() >= 1, not >=2, to let extensive optimization happen (= downward copy) even for a single mono-occurrence element in a parent block.
          if (selectedColums.size() >= 1)
          {
            // This is pack time, ladies and gentlemen.
            // An optimization might be performed for the selected columns between the indices inf and sup in the buffer.
            // The first line x which has a non empty field in one of the selected columns will be chosen as the packing destination.
            // Then, each time another line y between x+1 and sup which has a non empty field in one of the selected columns is met
            // (by construction: another field/selected column than the one of x because we deal with mono-occurrence fields each of them
            // on its own line) its field will be moved to x.
            // If selectedColums.size() = 1 it means x represents the only mono-occurrence element in the parent block and no candidate y line will
            // be found between x+1 and sup, but for the sake of simplicity the case selectedColums.size() == 1 was not separated from the rest.
            int x = inf;
            boolean foundX = false;
            while ((x <= sup) && (foundX == false))
            {
              for (int i = 0; i < selectedColums.size(); i++)
              {
                int oneSelectedColumnIndex = selectedColums.get(i);
                if (buffer.get(x)[oneSelectedColumnIndex] != null)
                {
                  foundX = true; // Line x has been found.
                  break;
                }
              }
              if (foundX == false) x++;
            }
            if (foundX == true)
            {
              // From now on line x will be the standard + enhanced pack destination.
              // If selectedColums.size() = 1 then line x will be alone and no candidate y line will exist by construction between x+1 and sup.
              // If selectedColums.size() >=2 then at least one candidate y line might exist.
              packingEnoughForExtensivePack = true;
              int y = x + 1;
              while (y <= sup)
              {
                for (int i = 0; i < selectedColums.size(); i++)
                {
                  int oneSelectedColumnIndex = selectedColums.get(i);
                  if (buffer.get(y)[oneSelectedColumnIndex] != null)
                  {
                    if (buffer.get(x)[oneSelectedColumnIndex] == null)
                    {
                      // OK: the destination field in line x is empty as expected. Time to move the field content from line y to line x.
                      // If this is the 1st optimization loop line y becomes empty, but if several loops have already been performed
                      // on the same buffer it might not be the case and the other fields might contain copies of previous fields.
                      // By security the rest of line y is emptied alongside.
                      buffer.get(x)[oneSelectedColumnIndex] = buffer.get(y)[oneSelectedColumnIndex];
                      buffer.get(y)[oneSelectedColumnIndex] = null;
                      for (int k = 0; k < trackedLeafElementXPaths.length; k++)
                        buffer.get(y)[k] = null;
                      atLeastOneStandardPackDone = true;
                      packingChangedBuffer = true;
                    }
                    else if ((buffer.get(y)[oneSelectedColumnIndex].equals(buffer.get(x)[oneSelectedColumnIndex])))
                    {
                      // OK: might happen when several regular&enhanced+extensive loops are made on the same buffer.
                      // This is at least the second loop and line x is a mono valued element which was in the scope of the previous
                      // ZERO_TO_ONE/ONE_TO_ONE tracked elements downward copy, and all of the remnant candidate y lines between x+1
                      // and sup were also in that scope and received the same fields for the same reason.
                      // This shallow copy is not seen as an actual error just a borderline effect of optimization flip-flops.
                      // Of course there is a slight possibility that it be an actual bug just like the next case but the probability
                      // remains very low.
                      // If this is the 1st optimization loop line y becomes empty, but if several loops have already been performed
                      // on the same buffer it might not be the case and the other fields might contain copies of previous fields.
                      // By security the rest of line y is emptied alongside.
                      buffer.get(y)[oneSelectedColumnIndex] = null;
                      for (int k = 0; k < trackedLeafElementXPaths.length; k++)
                        buffer.get(y)[k] = null;
                      atLeastOneStandardPackDone = true;
                      packingChangedBuffer = true;
                    }
                    else
                    {
                      // Nasty bug. The destination field should be empty (we deal with mono-occurrence fields in different columns).
                      // We raise an exception in order to avoid hazardous behavior.
                      throw new SAXException("Optimization bug. Mono-occurrence fields collision during <" + trackedParentOpening + ">[<" + x + ">-<" + y + ">-<"
                          + oneSelectedColumnIndex + ">]-based standard & enhanced packing loop.");
                    }
                  }
                }
                y++;
              }
              // OK: some regular & enhanced packing optimization was attempted on the line set between indices inf and sup consisting in stuffing data in line x
              // and emptying some lines between line x+1 and sup.
              // Depending on what was actually done (packingChangedBuffer = true/false) the regular & enhanced packing attempt had an effect or not on the
              // buffer, but in order to perform an extensive optimization the only thing that matters is that line x has been defined
              // (packingEnoughForExtensivePack = true) and might be copied back downwards, even left unchanged by the (enhanced) standard optimization.
              // If an extensive optimization has been planned it's time to examine back the lines between x+1 and sup.
              if (packingEnoughForExtensivePack == true)
              {
                // ArrayList selectedColumns is recycled.
                // All ZERO_TO_MANY/ONE_TO_MANY tracked elements which have trackedParentOpening as their parent are searched
                // between lines x+1 and sup and if there is at least one non empty line satisfying such conditions the content of x will
                // disappear, copied in all non empty lines satisfying the conditions (regular extensive propagation).
                selectedColums.clear();
                for (int i = 0; i < trackedLeafElementXPaths.length; i++)
                {
                  if (trackedLeafElementParentXPaths[i].equals(trackedParentOpening)) // Equals, not contains => does not include sub XPaths of trackedParentOpening.
                  {
                    if (((trackedLeafElementCardinalities[i] == XML2CSVCardinality.ONE_TO_MANY) || (trackedLeafElementCardinalities[i] == XML2CSVCardinality.ZERO_TO_MANY)))
                    {
                      // The element associated with column i is concerned by extensive optimization.
                      selectedColums.add(i);
                    }
                  }
                }
                // Extra extensive propagation.
                // In addition to the previous regular extensive propagation candidates we add mono-occurrence (ZERO_TO_ONE or ONE_TO_ONE) tracked elements or
                // ZERO_TO_MANY/ONE_TO_MANY tracked elements which have trackedParentOpening as their ancestor (= which are related to trackedParentOpening)
                // and are chained by intermediate elements back to P from which at least one is multi-occurrence for extra extensive propagation.
                for (int i = 0; i < trackedLeafElementXPaths.length; i++)
                {
                  HashMap<String, String[]> intermediateElements = trackedLeafElementsDescription.getIntermediateXPaths(trackedParentOpening, trackedLeafElementXPaths[i]);
                  if (intermediateElements != null)
                  {
                    // The examined tracked element trackedLeafElementXPaths[i] and trackedParentOpening are related that is, trackedLeafElementXPaths[i] is a sub XPath
                    // of trackedParentOpening. If it happens that at least one intermediary element between them is multi-occurrence then we have a new candidate for
                    // extra-extensive propagation.
                    boolean extraPropagationCandidate = true;
                    if (intermediateElements.keySet().isEmpty())
                    {
                      // Element trackedLeafElementXPaths[i] is a direct child of trackedParentOpening and was handled a few lines before during the selection of
                      // regular extensive propagation candidates.
                      // Extra-extensive propagation deals only with elements which have trackedParentOpening as a real ancestor.
                      extraPropagationCandidate = false;
                    }
                    else
                    {
                      Iterator<String> intermediateElementsIterator = intermediateElements.keySet().iterator();
                      while (intermediateElementsIterator.hasNext())
                      {
                        String intermediateElementXPath = intermediateElementsIterator.next();
                        String[] props = intermediateElements.get(intermediateElementXPath);
                        if (XML2CSVCardinality.isUnbounded(props[0]))
                        {
                          extraPropagationCandidate = true;
                          break;
                        }
                      }
                    }
                    if (extraPropagationCandidate == true)
                    {
                      // At least one intermediate element XPaths between trackedLeafElementXPaths[i] and trackedParentOpening is multi-occurrence.
                      // The element associated with column i is concerned by extra extensive propagation.
                      // if ((trackedLeafElementCardinalities[i] == XML2CSVCardinality.ONE_TO_ONE) || (trackedLeafElementCardinalities[i] == XML2CSVCardinality.ZERO_TO_ONE))
                      selectedColums.add(i);
                    }
                  }
                  else
                  {
                    // The examined tracked element trackedLeafElementXPaths[i] and trackedParentOpening are not related, that is, trackedLeafElementXPaths[i] is not a sub XPath
                    // of trackedParentOpening. No way for trackedLeafElementXPaths[i] to be candidate for extra extensive propagation.
                  }
                }
                if (selectedColums.size() >= 1)
                {
                  // At least one column candidate for extensive optimization.
                  y = x + 1;
                  while (y <= sup)
                  {
                    for (int i = 0; i < selectedColums.size(); i++)
                    {
                      int oneSelectedColumnIndex = selectedColums.get(i);
                      // The field at index oneSelectedColumnIndex only is checked in order to decide if it is an empty line or not because
                      // all the other fields are either empty (default situation) or have been stuffed by other mono valued fields from a
                      // previous optimization loop which are not relevant.
                      if (buffer.get(y)[oneSelectedColumnIndex] != null) // Line y is a non empty line satisfying the extensive packing conditions.
                      {
                        for (int j = 0; j < trackedLeafElementXPaths.length; j++)
                        {
                          // The content of line x is copied back in line y.
                          if (buffer.get(x)[j] != null)
                          {
                            if (buffer.get(y)[j] == null)
                            {
                              // OK: the destination field in line y is empty as expected.
                              // Time to copy the field content from line x to line y (line x will be emptied later on).
                              buffer.get(y)[j] = buffer.get(x)[j];
                              atLeastOneStandardPackDone = true;
                              extensivePackingChangedBuffer = true;
                            }
                            else if ((buffer.get(y)[j].equals(buffer.get(x)[j])))
                            {
                              // OK: might happen when several regular&enhanced+extensive loops are made on the same buffer.
                              // This is at least the second loop and line x is a mono valued element which was in the scope of the previous
                              // ZERO_TO_ONE/ONE_TO_ONE tracked elements downward copy, and all of the remnant candidate y lines between x+1
                              // and sup were also in that scope and received the same fields for the same reason.
                              // This shallow copy is not seen as an actual error just a borderline effect of optimization flip-flops.
                              // Of course there is a slight possibility that it be an actual bug just like the next case but the probability
                              // remains very low.
                              atLeastOneStandardPackDone = true;
                              extensivePackingChangedBuffer = true;
                            }
                            else
                            {
                              // Nasty bug. The destination fields should be empty (we deal with copy of mono-occurrence fields
                              // in multi-occurrence lines in separate columns. We raise an exception in order to avoid hazardous behavior.
                              throw new SAXException("Optimization bug. Mono/multi-occurrence fields collision during <" + trackedParentOpening + ">[<" + x + ">-<" + y + ">-<"
                                  + oneSelectedColumnIndex + ">-<" + j + ">]-based extensive optimization loop.");
                            }
                          }
                        }
                      }
                    }
                    y++;
                  }
                  if (extensivePackingChangedBuffer == true)
                  {
                    // The content of line x was copied at least once in a line y between x+1 and sup. Line x is emptied (cleared).
                    for (int j = 0; j < trackedLeafElementXPaths.length; j++)
                      buffer.get(x)[j] = null;
                  }
                }
                else
                {
                  // No columns candidate for extensive optimization. There is nothing to optimize any further.
                }
              }
              else
              {
                // No enhanced standard optimization performed or no extensive optimization required after successful enhanced standard optimization. There is nothing more to do.
              }
              if (XML2CSVLoggingFacade.DEBUG_MODE == true)
              {
                if (extensivePackingChangedBuffer == true)
                {
                  XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "<" + buffer.size() + "> lines in buffer after <" + trackedParentOpening
                      + ">-based enhanced standard+extensive optimization loop. Intermediary buffer content: ");
                }
                else if (packingChangedBuffer == true)
                {
                  XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "<" + buffer.size() + "> lines in buffer after <" + trackedParentOpening
                      + ">-based enhanced standard optimization loop. Intermediary buffer content: ");
                }
                if ((packingChangedBuffer == true) || (extensivePackingChangedBuffer == true)) displayBufferContent();
              }
            }
            else
            {
              // All the selected columns between indices inf and sup in the buffer were empty. There is nothing to optimize.
            }
          }
          // When the optimization sub routine is finished the main loop resumes.
          inf++;
        }
      }
      if (XML2CSVLoggingFacade.DEBUG_MODE == true)
      {
        if ((atLeastOneStandardPackDone == true) || (atLeastOneExtensivePackingDone == true))
        {
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "<" + buffer.size() + "> lines in buffer after optimization. Final content:");
          displayBufferContent();
        }
        else
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "buffer left unchanged after optimization.");
      }
    }
    return (atLeastOneStandardPackDone || atLeastOneExtensivePackingDone);
  }

  /**
   * Optimizes the data currently buffered, expected complete and consistent.<br>
   * Performs a {@link utils.xml.xml2csv.constants.XML2CSVOptimization#STANDARD STANDARD} or an {@link utils.xml.xml2csv.constants.XML2CSVOptimization#EXTENSIVE_V1 EXTENSIVE_V1}
   * optimization depending on the current level.
   * A {@link utils.xml.xml2csv.constants.XML2CSVOptimization#STANDARD STANDARD} optimization is performed on each buffer line subset LS going from one tracked element's parent P
   * opening index PO to the next closing index PC and implies the columns/tracked elements which have this parent P and which are mono-occurrence.<br>
   * A {@link utils.xml.xml2csv.constants.XML2CSVOptimization#STANDARD STANDARD} optimization consists in packing the related mono-occurrence lines in one unique line X with each
   * tracked element at its corresponding index, before going on with the next buffer line subset.<br>
   * An {@link utils.xml.xml2csv.constants.XML2CSVOptimization#EXTENSIVE_V1 EXTENSIVE_V1} buffer line subset optimization is a
   * {@link utils.xml.xml2csv.constants.XML2CSVOptimization#STANDARD STANDARD} one plus back copy of the packed line X into each multi-occurrence element line of LS provided that
   * its parent block be either P or a sub block of P, plus mono-occurrence element line of LS whose parent is a sub block of P.<br>
   * Optimization ends when the last buffer line subset has been processed (when the last line block enclosed by tracked element's parent opening/closing has been performed).
   * @return true if the optimization changed something in the buffer.
   * @throws <code>SAXException</code> in case of error.
   */
  public boolean optimizeV1() throws SAXException
  {
    boolean atLeastOneStandardPackDone = false; // Set to true if standard packing changed something in the buffer
    boolean atLeastOneExtensivePackingDone = false; // Set to true if extensive optimization changed something in the buffer.
    if ((isEmpty() == false) && ((level == XML2CSVOptimization.STANDARD) || (level == XML2CSVOptimization.EXTENSIVE_V1)))
    {
      if (XML2CSVLoggingFacade.DEBUG_MODE == true)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "<" + buffer.size() + "> lines in buffer before optimization. Original content:");
        displayBufferContent();
      }
      // The main loop moves through the buffer from the beginning to the end (the inf index is incremented accordingly).
      // Each time a tracked parent opening P is met (in column trackedLeafElementXPaths.length) the next closing index of the same kind is searched
      // (in column trackedLeafElementXPaths.length+1): one optimization sub routine is then triggered between the corresponding inf and sup indices of the buffer,
      // and all mono-occurrence (ZERO_TO_ONE or ONE_TO_ONE) tracked elements which have P as their parent are packed on the same line x (leaving a certain amount of blank
      // lines between line x+1 and sup which are left as is because they will be silently discarded when the buffer will be echoed to the output).
      // If an extensive optimization is expected then:
      // - the data of line x is copied back into all non empty lines containing a field in connection with a multi-occurrence (ZERO_TO_MANY or ONE_TO_MANY) tracked element
      // which has either P as its parent or a sub element of P as its parent, plus lines containing a field in connection with a mono-occurrence (ZERO_TO_ONE or ONE_TO_ONE)
      // tracked element which has a sub element of P as parent.
      // - line x is emptied in order to make it disappear in the output if x has been copied back at least once.
      // When a sub routine ends the main loop resumes at inf+1, and when the buffer end is reached the optimization is phase is finished.
      int inf = 0;
      int loopCount = 0;
      while (inf < buffer.size())
      {
        boolean standardPackingChangedBuffer = false;
        boolean standardPackingEnoughForExtensivePack = false;
        boolean extensivePackingChangedBuffer = false;
        String[] infLine = buffer.get(inf);
        String trackedParentOpening = infLine[trackedLeafElementXPaths.length];
        if (trackedParentOpening == null) inf++;
        else
        {
          // OK: a tracked parent opening was met. We look for the related tracked parent closing index.
          int sup = inf;
          boolean foundSup = false;
          while ((sup < buffer.size()) && (foundSup == false))
          {
            String[] supLine = buffer.get(sup);
            if (supLine[trackedLeafElementXPaths.length + 1] == null) sup++;
            else
            {
              if (trackedParentOpening.equals(supLine[trackedLeafElementXPaths.length + 1])) foundSup = true;
              else
                sup++;
            }
          }
          if (foundSup == false) // The related tracked parent closing index was not found!
          {
            // This can't be unless a nasty bug occurred. Each tracked parent opening should have its counterpart tracked parent closing.
            // We raise an exception in order to avoid hazardous behavior.
            throw new SAXException("Optimization bug. Bad recording of tracked element's parent in the data buffer.");
          }
          // At this stage a standard packing sub routine is triggered dealing with the buffer lines between the indices inf and sup.
          // All ZERO_TO_ONE/ONE_TO_ONE tracked elements which have trackedParentOpening as their parent are searched and if they are two or more
          // they can be packed on the same line.
          ArrayList<Integer> selectedColums = new ArrayList<Integer>();
          for (int i = 0; i < trackedLeafElementXPaths.length; i++)
          {
            if ((trackedParentOpening.equals(trackedLeafElementParentXPaths[i]))
                && ((trackedLeafElementCardinalities[i] == XML2CSVCardinality.ONE_TO_ONE) || (trackedLeafElementCardinalities[i] == XML2CSVCardinality.ZERO_TO_ONE)))
            {
              // The element associated with column i is concerned.
              selectedColums.add(i);
            }
          }
          // We go on if selectedColums.size() >= 1, not >=2, to let extensive optimization happen (= downward copy) even for a single mono-occurrence element in a parent block.
          if (selectedColums.size() >= 1)
          {
            // Standard packing might be performed for the selected columns between the indices inf and sup in the buffer.
            // The first line x which has a non empty field in one of the selected columns will be chosen as the pack destination.
            // Then, each time another line y between x+1 and sup which has a non empty field in one of the selected columns is met
            // (by construction: another field/selected column than the one of x because we deal with mono-occurrence fields each of them
            // on its own line) its field will be moved to x.
            // If selectedColums.size() = 1 it means x represents the only mono-occurrence element in the parent block and no candidate y line will
            // be found between x+1 and sup, but for the sake of simplicity the case selectedColums.size() == 1 was not separated from the rest.
            int x = inf;
            boolean foundX = false;
            while ((x <= sup) && (foundX == false))
            {
              for (int i = 0; i < selectedColums.size(); i++)
              {
                int oneSelectedColumnIndex = selectedColums.get(i);
                if (buffer.get(x)[oneSelectedColumnIndex] != null)
                {
                  foundX = true; // Line x has been found.
                  break;
                }
              }
              if (foundX == false) x++;
            }
            if (foundX == true)
            {
              // From now on line x will be the standard pack destination.
              // If selectedColums.size() = 1 then line x will be alone and no candidate y line will exist by construction between x+1 and sup.
              // If selectedColums.size() >=2 then at least one candidate y line might exist.
              standardPackingEnoughForExtensivePack = true;
              int y = x + 1;
              while (y <= sup)
              {
                for (int i = 0; i < selectedColums.size(); i++)
                {
                  int oneSelectedColumnIndex = selectedColums.get(i);
                  if (buffer.get(y)[oneSelectedColumnIndex] != null)
                  {
                    if (buffer.get(x)[oneSelectedColumnIndex] == null)
                    {
                      // OK: the destination field in line x is empty as expected. Time to move the field content from line y to line x.
                      // If this is the 1st optimization loop line y becomes empty, but if several loops have already been performed
                      // on the same buffer it might not be the case and the other fields might contain copies of previous fields.
                      // By security the rest of line y is emptied alongside.
                      buffer.get(x)[oneSelectedColumnIndex] = buffer.get(y)[oneSelectedColumnIndex];
                      buffer.get(y)[oneSelectedColumnIndex] = null;
                      for (int k = 0; k < trackedLeafElementXPaths.length; k++)
                        buffer.get(y)[k] = null;
                      atLeastOneStandardPackDone = true;
                      standardPackingChangedBuffer = true;
                    }
                    else if ((buffer.get(y)[oneSelectedColumnIndex].equals(buffer.get(x)[oneSelectedColumnIndex])) && (loopCount > 1))
                    {
                      // OK: might happen when several standard+extensive loops are made on the same buffer.
                      // This is at least the second loop and line x is a mono valued element which was in the scope of the previous
                      // ZERO_TO_ONE/ONE_TO_ONE tracked elements downward copy, and all of the remnant candidate y lines between x+1
                      // and sup were also in that scope and received the same fields for the same reason.
                      // This shallow copy is not seen as an actual error just a borderline effect of optimization flip-flops.
                      // Of course there is always a slight possibility of an actual bug just like the next case but with a carefully
                      // tested program odds remain in our favor.
                      // If this is the 1st optimization loop line y becomes empty when field at index oneSelectedColumnIndex is set
                      // to null, but if several loops have already been performed on the same buffer it might not be the case and the
                      // other fields might contain copies of previous fields. By security the rest of line y is emptied alongside.
                      buffer.get(y)[oneSelectedColumnIndex] = null;
                      for (int k = 0; k < trackedLeafElementXPaths.length; k++)
                        buffer.get(y)[k] = null;
                      atLeastOneStandardPackDone = true;
                      standardPackingChangedBuffer = true;
                    }
                    else
                    {
                      // Nasty bug. The destination field should be empty (we deal with mono-occurrence fields in different columns).
                      // We raise an exception in order to avoid hazardous behavior.
                      throw new SAXException("Optimization bug. Mono-occurrence fields collision during <" + trackedParentOpening + ">-based standard packing loop.");
                    }
                  }
                }
                y++;
              }
              // A count of the actual optimization loops is made incremented by convention each time a new candidate x line has been selected.
              if (standardPackingEnoughForExtensivePack == true) loopCount++;
              // OK: some standard optimization was attempted on the line set between indices inf and sup consisting in stuffing data in line x
              // and emptying some lines between line x+1 and sup.
              // Depending on what was actually done (standardPackingChangedBuffer = true/false) the packing attempt had an effect or not on the
              // buffer, but in order to perform an extensive optimization the only thing that matters is that line x has been defined
              // (standardPackingEnoughForExtensivePack = true) and might be copied back downwards, even left unchanged by the standard optimization.
              // If an extensive optimization has been planned it's time to examine back the lines between x+1 and sup.
              if ((standardPackingEnoughForExtensivePack == true) && (level == XML2CSVOptimization.EXTENSIVE_V1))
              {
                // ArrayList selectedColumns is recycled.
                // All ZERO_TO_MANY/ONE_TO_MANY tracked elements which have trackedParentOpening or a sub XPath as their parent are searched
                // between lines x+1 and sup and if there is at least one non empty line satisfying such conditions the content of x will
                // disappear, copied in all non empty lines satisfying the conditions.
                // By extension, ZERO_TO_ONE/ONE_TO_ONE tracked elements which have a real sub XPath of trackedParentOpening as their parent
                // are searched too.
                selectedColums.clear();
                for (int i = 0; i < trackedLeafElementXPaths.length; i++)
                {
                  if (trackedLeafElementParentXPaths[i].contains(trackedParentOpening)) // Contains, not equals => includes sub XPaths of trackedParentOpening.
                  {
                    if (((trackedLeafElementCardinalities[i] == XML2CSVCardinality.ONE_TO_MANY) || (trackedLeafElementCardinalities[i] == XML2CSVCardinality.ZERO_TO_MANY)))
                    {
                      // The element associated with column i is concerned by extensive optimization.
                      selectedColums.add(i);
                    }
                    else if ((trackedLeafElementParentXPaths[i].length() != trackedParentOpening.length()) // Real sub XPath of trackedParentOpening.
                        && ((trackedLeafElementCardinalities[i] == XML2CSVCardinality.ONE_TO_ONE) || (trackedLeafElementCardinalities[i] == XML2CSVCardinality.ZERO_TO_ONE)))
                    {
                      // The element associated with column i is concerned by extensive optimization.
                      selectedColums.add(i);
                    }
                  }
                }
                if (selectedColums.size() >= 1)
                {
                  // At least one column candidate for extensive optimization.
                  y = x + 1;
                  while (y <= sup)
                  {
                    for (int i = 0; i < selectedColums.size(); i++)
                    {
                      int oneSelectedColumnIndex = selectedColums.get(i);
                      // The field at index oneSelectedColumnIndex only is checked in order to decide if it is an empty line or not.
                      // Indeed, all the other fields are either empty (default situation) or have been stuffed by other mono valued fields from a
                      // previous optimization loop, and are not relevant.
                      if (buffer.get(y)[oneSelectedColumnIndex] != null) // Line y is a non empty line satisfying the extensive packing conditions.
                      {
                        for (int j = 0; j < trackedLeafElementXPaths.length; j++)
                        {
                          // The content of line x is copied back in line y.
                          if (buffer.get(x)[j] != null)
                          {
                            if (buffer.get(y)[j] == null)
                            {
                              // OK: the destination field in line y is empty as expected.
                              // Time to copy the field content from line x to line y (line x will be emptied later on).
                              buffer.get(y)[j] = buffer.get(x)[j];
                              atLeastOneStandardPackDone = true;
                              extensivePackingChangedBuffer = true;
                            }
                            else if ((buffer.get(y)[j].equals(buffer.get(x)[j])) && (loopCount > 1))
                            {
                              // OK: might happen when several standard+extensive loops are made on the same buffer.
                              // This is at least the second loop and line x is a mono valued element which was in the scope of the previous
                              // ZERO_TO_ONE/ONE_TO_ONE tracked elements downward copy, and all of the remnant candidate y lines between x+1
                              // and sup were also in that scope and received the same fields for the same reason.
                              // This shallow copy is not seen as an actual error just a borderline effect of optimization flip-flops.
                              // Of course there is always a slight possibility of an actual bug just like the next case but with a carefully
                              // tested program odds remain in our favor.
                              atLeastOneStandardPackDone = true;
                              extensivePackingChangedBuffer = true;
                            }
                            else
                            {
                              // Nasty bug. The destination fields should be empty (we deal with copy of mono-occurrence fields
                              // in multi-occurrence lines in separate columns. We raise an exception in order to avoid hazardous behavior.
                              throw new SAXException("Optimization bug. Mono/multi-occurrence fields collision during <" + trackedParentOpening
                                  + ">-based extensive optimization loop.");
                            }
                          }
                        }
                      }
                    }
                    y++;
                  }
                  if (extensivePackingChangedBuffer == true)
                  {
                    // The content of line x was copied at least once in a line y between x+1 and sup.
                    // Line x is emptied (cleared).
                    for (int j = 0; j < trackedLeafElementXPaths.length; j++)
                      buffer.get(x)[j] = null;
                  }
                }
                else
                {
                  // No columns candidate for extensive optimization. There is nothing to optimize any more.
                }
              }
              else
              {
                // No standard optimization performed or no extensive optimization required after successful standard optimization. There is nothing more to do.
              }
              if (XML2CSVLoggingFacade.DEBUG_MODE == true)
              {
                if (extensivePackingChangedBuffer == true)
                {
                  XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "<" + buffer.size() + "> lines in buffer after one <" + trackedParentOpening
                      + ">-based standard+extensive optimization loop. Intermediary buffer content: ");
                }
                else if (standardPackingChangedBuffer == true)
                {
                  XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "<" + buffer.size() + "> lines in buffer after one <" + trackedParentOpening
                      + ">-based standard optimization loop. Intermediary buffer content: ");
                }
                if ((standardPackingChangedBuffer == true) || (extensivePackingChangedBuffer == true)) displayBufferContent();
              }
            }
            else
            {
              // All the selected columns between indices inf and sup in the buffer were empty. There is nothing to optimize.
            }
          }
          // When the optimization sub routine is finished the main loop resumes.
          inf++;
        }
      }
      if (XML2CSVLoggingFacade.DEBUG_MODE == true)
      {
        if ((atLeastOneStandardPackDone == true) || (atLeastOneExtensivePackingDone == true))
        {
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "<" + buffer.size() + "> lines in buffer after optimization. Final content:");
          displayBufferContent();
        }
        else
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.DEBUG, "buffer left unchanged after optimization.");
      }
    }
    return (atLeastOneStandardPackDone || atLeastOneExtensivePackingDone);
  }
}
