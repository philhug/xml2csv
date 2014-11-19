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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import utils.xml.xml2csv.constants.XML2CSVLogLevel;
import utils.xml.xml2csv.constants.XML2CSVMisc;
import utils.xml.xml2csv.constants.XML2CSVOptimization;
import utils.xml.xml2csv.exception.XML2CSVCancelException;
import utils.xml.xml2csv.exception.XML2CSVDataException;
import utils.xml.xml2csv.exception.XML2CSVException;
import utils.xml.xml2csv.exception.XML2CSVFilterException;
import utils.xml.xml2csv.exception.XML2CSVParameterException;
import utils.xml.xml2csv.exception.XML2CSVStructureException;

/**
 * XML2CSV generic generator.<br>
 * Method {@link utils.xml.xml2csv.XML2CSVGenericGenerator#generate(File[], String, String[], String[], boolean) generate} will extract part or all data from similar XML input
 * files to a CSV output (file(s) or direct <code>OutputStream</code>) depending on the constructor used.<br>
 * It's the caller's responsibility to close a direct <code>OutputStream</code> which will be left open at the end of CSV generation.<br>
 * Additional methods such as {@link utils.xml.xml2csv.XML2CSVGenericGenerator#setOutputFile(File) setOutputFile} make it possible to change the initial configuration afterwards.<br>
 * The runtime logging behavior is inherited from the current logging {@link utils.xml.xml2csv.XML2CSVLoggingFacade facade} configuration.
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public class XML2CSVGenericGenerator
{
  /** Active output indicator. */
  private boolean activeOutput = false;

  /** Blend output indicator. */
  private boolean BLEND_MODE = false;

  /** Direct OutputStream. */
  private OutputStream output = null;

  /** The CSV output file. */
  private File csvOutputFile = null;

  /** The CSV output directory. */
  private File csvOutputDir = null;

  /** The CSV field separator to use. */
  private String csvFieldSeparator = null;

  /** The character encoding to use for the output. */
  private Charset encoding = null;

  /** The chosen optimization level. */
  private XML2CSVOptimization level = null;

  /** The chosen output cutoff limit. */
  private long cutoff = -1L;

  /** Name space awareness indicator. */
  private boolean withNamespaces = false;

  /** Unleashed optimization indicator. */
  private boolean unleashed = false;

  /**
   * <code>XML2CSVGenericGenerator</code> constructor.<br>
   * Data extracted from XML input files will be sent to the given <code>OutputStream</code>.<br>
   * Equivalent to a <code>XML2CSVGenericGenerator(output, null, null, null)</code> constructor call.<br>
   * The instance constructed is marked <i>output ready</i>, that is, its method
   * {@link utils.xml.xml2csv.XML2CSVGenericGenerator#generate(File[], String, String[], String[], boolean) generate} might be called immediately afterwards.
   * @param output the <code>OutputStream</code>; <code>null</code> value not allowed.
   * @throws XML2CSVException in case of error.
   */
  public XML2CSVGenericGenerator(OutputStream output) throws XML2CSVException
  {
    this(output, null, null, null);
  }

  /**
   * <code>XML2CSVGenericGenerator</code> constructor.<br>
   * Data extracted from XML input files will be sent to the given <code>OutputStream</code>.<br>
   * The CSV field separator will be the one provided (character <code>;</code> if left <code>null</code>), and the <i>character encoding</i> will be the one provided (
   * {@link utils.xml.xml2csv.constants.XML2CSVMisc#UTF8 UTF8} if left <code>null</code>).<br>
   * CSV data presentation will depend on the chosen optimization level ({@link utils.xml.xml2csv.constants.XML2CSVOptimization#STANDARD STANDARD} if left <code>null</code>).<br>
   * The actual logging behavior will be inherited from the current logging {@link utils.xml.xml2csv.XML2CSVLoggingFacade facade} configuration.<br>
   * The instance constructed is marked <i>output ready</i>, that is, its method
   * {@link utils.xml.xml2csv.XML2CSVGenericGenerator#generate(File[], String, String[], String[], boolean) generate} might be called immediately afterwards.
   * @param output the <code>OutputStream</code>; <code>null</code> value not allowed.
   * @param csvFieldSeparator the CSV field separator, or <code>null</code>.
   * @param encoding the encoding to use in the CSV output file, or <code>null</code>.
   * @param level the chosen <code>XML2CSVOptimization</code> level, or <code>null</code>.
   * @throws XML2CSVException in case of error.
   */
  public XML2CSVGenericGenerator(OutputStream output, String csvFieldSeparator, Charset encoding, XML2CSVOptimization level) throws XML2CSVException
  {
    super();
    setOutputStream(output);
    setFieldSeparator(csvFieldSeparator);
    setEncoding(encoding);
    setOptimization(level);
    setCutoff(-1L); // No cutoff when a direct output is used.
  }

  /**
   * <code>XML2CSVGenericGenerator</code> constructor.<br>
   * Data extracted from XML input files will be sent, depending on which parameter is not <code>null</code>:<br>
   * <ul>
   * <li>to the given CSV output file, or
   * <li>to as many CSV output files as there are XML input files (keeping names, extension changed to CSV) in the given CSV output directory.
   * </ul>
   * When both are filled, <code>csvOutputFile</code> takes precedence over <code>csvOutputDir</code>, that, is, a single CSV output file over multiple CSV output files.<br>
   * Equivalent to a <code>XML2CSVGenericGenerator(csvOutputFile, csvOutputDir, null, null, null, -1L)</code> constructor call.
   * A <code>XML2CSVParameterException</code> will be thrown if neither of parameters <code>csvOutputFile</code> and <code>csvOutputDir</code> are filled or left <code>null</code>.<br>
   * The instance constructed is marked <i>output ready</i>, that is, its method
   * {@link utils.xml.xml2csv.XML2CSVGenericGenerator#generate(File[], String, String[], String[], boolean) generate} might be called immediately afterwards.
   * @param csvOutputFile the CSV output file; cannot be <code>null</code> if the next parameter is <code>null</code>.
   * @param csvOutputDir the output directory where CSV files are written; cannot be <code>null</code> if the previous parameter is <code>null</code>.
   * @throws XML2CSVException in case of error.
   */
  public XML2CSVGenericGenerator(File csvOutputFile, File csvOutputDir) throws XML2CSVException
  {
    this(csvOutputFile, csvOutputDir, null, null, null, -1L);
  }

  /**
   * <code>XML2CSVGenericGenerator</code> constructor.<br>
   * Data extracted from XML input files will be sent, depending on which parameter is not <code>null</code>:<br>
   * <ul>
   * <li>to the given CSV output file, or
   * <li>to as many CSV output files as there are XML input files (same names, extension changed to CSV) in the given CSV output directory.
   * </ul>
   * A <code>XML2CSVParameterException</code> will be thrown if neither of parameters <code>csvOutputFile</code> and <code>csvOutputDir</code> are filled or left <code>null</code>;
   * when both are filled, <code>csvOutputFile</code> takes precedence over <code>csvOutputDir</code>, that, is, a single CSV output file over multiple CSV output files.<br>
   * The CSV field separator will be the one provided (character <code>;</code> if left <code>null</code>), and the <i>character encoding</i> will be the one provided (
   * {@link utils.xml.xml2csv.constants.XML2CSVMisc#UTF8 UTF8} if left <code>null</code>).<br>
   * CSV data presentation will depend on the chosen optimization level ({@link utils.xml.xml2csv.constants.XML2CSVOptimization#STANDARD STANDARD} if left <code>null</code>).<br>
   * The actual logging behavior will be inherited from the current logging {@link utils.xml.xml2csv.XML2CSVLoggingFacade facade} configuration.<br>
   * The cutoff value provided will trigger an appropriate output file cutoff behavior:<br>
   * <ul>
   * <li>no output file will hold more lines than the cutoff value provided when strictly positive (automatic output file slicing will be done when needed).
   * <li>cutoff behavior will be deactivated if the cutoff value provided is below or equal to zero.
   * </ul>
   * The instance constructed is marked <i>output ready</i>, that is, its method
   * {@link utils.xml.xml2csv.XML2CSVGenericGenerator#generate(File[], String, String[], String[], boolean) generate} might be called immediately afterwards.
   * @param csvOutputFile the CSV output file; cannot be <code>null</code> if the next parameter is <code>null</code>.
   * @param csvOutputDir the output directory where CSV files are written; cannot be <code>null</code> if the previous parameter is <code>null</code>.
   * @param csvFieldSeparator the CSV field separator, or <code>null</code>.
   * @param encoding the encoding to use in the CSV output file, or <code>null</code>.
   * @param level the chosen <code>XML2CSVOptimization</code> level, or <code>null</code>.
   * @param cutoff a positive number to activate the appropriate output file cutoff behavior; zero or a number below zero to deactivate cutoff logic.
   * @throws XML2CSVException in case of error.
   */
  public XML2CSVGenericGenerator(File csvOutputFile, File csvOutputDir, String csvFieldSeparator, Charset encoding, XML2CSVOptimization level, long cutoff) throws XML2CSVException
  {
    super();
    if (csvOutputFile != null) setOutputFile(csvOutputFile);
    else if (csvOutputDir != null) setOutputDir(csvOutputDir);
    else
      throw new XML2CSVParameterException("No output file or directory.");

    setFieldSeparator(csvFieldSeparator);
    setEncoding(encoding);
    setOptimization(level);
    setCutoff(cutoff);
  }

  /**
   * XML2CSV conversion of the XML input file(s) provided.<br>
   * All the XML files are expected to have a similar structure.<br>
   * Equivalent to a <code>generate(xmlInputFiles, null, null, null, withAttributes)</code> call.
   * @param xmlInputFiles the XML input files to process.
   * @param withAttributes <code>true</code> if element attributes should be extracted as well or <code>false</code> otherwise.
   * @throws XML2CSVException in case of error.
   */
  public void generate(File[] xmlInputFiles, boolean withAttributes) throws XML2CSVException
  {
    generate(xmlInputFiles, null, null, null, withAttributes);
  }

  /**
   * XML2CSV conversion of the XML input file(s) provided.<br>
   * All the XML files are expected to have a similar structure.<br>
   * This XML structure is computed first, using one of the XML input files as template:<br>
   * <ul>
   * <li>one of the XML input files at random if parameter <code>xmlTemplateFileName</code> was left <code>null</code>;
   * <li>when not <code>null</code>, the XML input file named <code>xmlTemplateFileName</code>.
   * </ul>
   * Then the XML input files are processed one by one, in order to extract:
   * <ul>
   * <li>all XML leaf elements defined in the structure if parameters <code>xmlExpectedElementsXPaths</code> and <code>xmlDiscardedElementsXPaths</code> were left <code>null</code>;
   * <li>when not <code>null</code>, only the XML leaf elements explicitly listed in <code>xmlExpectedElementsXPaths</code>;
   * <li>when not <code>null</code>, all the XML leaf elements but those explicitly listed in <code>xmlDiscardedElementsXPaths</code>.
   * </ul>
   * The actual output where CSV contents are generated (one or several file(s) or one direct output stream) depends on the class constructor invoked in the first place.<br>
   * A direct output stream won't be closed when method <code>generate</code> terminates (caller's responsibility to close it properly) but regular files will.<br>
   * The instance is marked <i>inactive</i> when <code>generate</code> terminates, and one of the {@link utils.xml.xml2csv.XML2CSVGenericGenerator#setOutputDir(File) setOutputDir},
   * {@link utils.xml.xml2csv.XML2CSVGenericGenerator#setOutputFile(File) setOutputFile} or {@link utils.xml.xml2csv.XML2CSVGenericGenerator#setOutputStream(OutputStream)
   * setOutputStream} methods has to be called in order to be marked <i>output ready</i> again.
   * @param xmlInputFiles the XML input files to process.
   * @param xmlTemplateFileName the name of the XML input file to use for XML structure analysis, or <code>null</code> to let the generator pick one at random.
   * @param xmlExpectedElementsXPaths a list of leaf element XPaths to extract from the XML files, or <code>null</code> to have all XML leaf elements extracted.
   * @param xmlDiscardedElementsXPaths a list of leaf element XPaths to discard from the XML files, or <code>null</code> to have all XML leaf elements extracted.
   * @param withAttributes <code>true</code> if element attributes should be extracted as well or <code>false</code> otherwise.
   * @throws XML2CSVException in case of error.
   */
  public void generate(File[] xmlInputFiles, String xmlTemplateFileName, String[] xmlExpectedElementsXPaths, String[] xmlDiscardedElementsXPaths, boolean withAttributes)
      throws XML2CSVException
  {
    // The structure analysis result (the ordered description list of leaf elements in the input XML files).
    ElementsDescription leafElementsDescription = null;

    // Parameters check.
    if (xmlInputFiles == null) throw new XML2CSVParameterException("Null XML input list.");
    if (xmlInputFiles.length == 0) throw new XML2CSVCancelException("Empty XML input list.");
    if (activeOutput == false) throw new XML2CSVCancelException("No active output.");
    for (int i = 0; i < xmlInputFiles.length; i++)
    {
      if ((xmlInputFiles[i] != null) && (xmlInputFiles[i].isFile() == false)) throw new XML2CSVParameterException("XML input file at index <" + i + "> is not a regular file.");
    }
    if ((xmlExpectedElementsXPaths != null) && (xmlDiscardedElementsXPaths != null)) throw new XML2CSVParameterException(
        "Cannot provide both a list of expected elements and a list of elements to discard.");

    // If one particular XML input file has been explicitly chosen for XML structure analysis (maybe because it's the most complete?) it is used in priority.
    // If not, the first selected XML input file will do the trick (all XML input files are expected to have the same structure so any of them will fit in).
    File fileToUseForStructureAnalysis = null;
    if (xmlTemplateFileName != null)
    {
      for (int i = 0; i < xmlInputFiles.length; i++)
      {
        if (xmlInputFiles[i].getName().equals(xmlTemplateFileName))
        {
          fileToUseForStructureAnalysis = xmlInputFiles[i];
          break;
        }
      }
      if (fileToUseForStructureAnalysis == null) throw new XML2CSVParameterException("Chosen template XML file <" + xmlTemplateFileName + "> not found among XML input files.");
    }
    else
    {
      for (int i = 0; i < xmlInputFiles.length; i++)
      {
        if (xmlInputFiles[i] != null)
        {
          fileToUseForStructureAnalysis = xmlInputFiles[i];
          break;
        }
      }
      if (fileToUseForStructureAnalysis == null) throw new XML2CSVParameterException("XML input list with only null entries.");
    }

    // The XML structure analysis computes the ordered XPath of leaf XML elements (kind of XML schema reverse engineering).
    if (XML2CSVLoggingFacade.VERBOSE_MODE == true)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, "computing XML structure...");
      XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.LINE);
    }
    StructureHandler structureHandler = new StructureHandler(level, withAttributes, withNamespaces); // XML structure analysis handler initialization.
    try
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, "using XML input file <" + fileToUseForStructureAnalysis.getName() + "> for template.");
      leafElementsDescription = computeXMLStructure(fileToUseForStructureAnalysis, structureHandler);
    }
    catch (Exception e)
    {
      throw new XML2CSVStructureException(e.getMessage(), e);
    }

    // If a list of expected elements has been provided we ensure the element XPaths provided were met in the XML structure.
    // XPaths not found in the structure trigger warning messages and are plainly ignored (technically: set to null in the String[]).
    // Partial XPaths are tolerated as a convenient representation of all leaf elements which have this XPath as ancestor, plus
    // all attributes which have this XPath as ancestor.
    // The same test is made for the list of elements to discard.
    if (xmlExpectedElementsXPaths != null)
    {
      LinkedHashMap<String, String> accumulator = new LinkedHashMap<String, String>(); // A LinkedHashMap eliminates duplicates and keeps order too.
      for (int i = 0; i < xmlExpectedElementsXPaths.length; i++)
      {
        if (structureHandler.getIndexOfLeafElement(xmlExpectedElementsXPaths[i]) == -1)
        {
          // Could be a partial XPath, or a junk XPath.
          String[] descendants = structureHandler.getDescendantLeafElements(xmlExpectedElementsXPaths[i]);
          if (descendants != null)
          {
            // Partial XPath.
            for (int j = 0; j < descendants.length; j++)
              accumulator.put(descendants[j], null);
          }
          else
          {
            // Junk XPath.
            XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, "positive filter XPath <" + xmlExpectedElementsXPaths[i]
                + "> was not found in the XML structure and will be ignored.");
            xmlExpectedElementsXPaths[i] = null;
          }
        }
        else
        {
          // Regular leaf element.
          accumulator.put(xmlExpectedElementsXPaths[i], null);
        }
      }
      if (accumulator.size() == 0) throw new XML2CSVFilterException("Expected element list did not contain any correct filter XPath.");
      else
      {
        String[] temp = new String[accumulator.size()];
        xmlExpectedElementsXPaths = accumulator.keySet().toArray(temp);
      }
    }
    if (xmlDiscardedElementsXPaths != null)
    {
      LinkedHashMap<String, String> accumulator = new LinkedHashMap<String, String>(); // A LinkedHashMap eliminates duplicates and keeps order too.
      for (int i = 0; i < xmlDiscardedElementsXPaths.length; i++)
      {
        if (structureHandler.getIndexOfLeafElement(xmlDiscardedElementsXPaths[i]) == -1)
        {
          // Could be a partial XPath, or a junk XPath.
          String[] descendants = structureHandler.getDescendantLeafElements(xmlDiscardedElementsXPaths[i]);
          if (descendants != null)
          {
            // Partial XPath.
            for (int j = 0; j < descendants.length; j++)
              accumulator.put(descendants[j], null);
          }
          else
          {
            // Junk XPath.
            XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, "negative filter XPath <" + xmlDiscardedElementsXPaths[i]
                + "> was not found in the XML structure and will be ignored.");
            xmlDiscardedElementsXPaths[i] = null;
          }
        }
        else
        {
          // Regular leaf element.
          accumulator.put(xmlDiscardedElementsXPaths[i], null);
        }
      }
      if (accumulator.size() == 0) throw new XML2CSVFilterException("Discarded element list did not contain any correct filter XPath.");
      else
      {
        String[] temp = new String[accumulator.size()];
        xmlDiscardedElementsXPaths = accumulator.keySet().toArray(temp);
      }
    }
    if (XML2CSVLoggingFacade.VERBOSE_MODE == true)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, "done computing XML structure.");
      XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.LINE);
    }

    String errorMessage = null; // Trick in order to have the finally block executed before the method returns in case of exception.
    int step = 0; // Trick to display the correct error message in case of error.
    File oneXmlinputFile = null; // Reference to one XML input file.
    OutputWriterFacade outputWriterFacade = null;
    try
    {
      step = 1;

      if (BLEND_MODE == true)
      {
        if (output != null)
        {
          // A direct OutputStream was provided.
          outputWriterFacade = new OutputWriterFacade(output, encoding);
        }
        else
        {
          // An explicit CSV output file was provided. It is opened (append mode = true).
          outputWriterFacade = new OutputWriterFacade(cutoff, csvOutputFile, encoding);
        }
      }
      else
        outputWriterFacade = new OutputWriterFacade(); // Writer facade initialization delayed.

      step = 2;

      // The XML data handler is initialized. It will be reset/reused for each XML input file.
      DataHandler dataHandler = new DataHandler(outputWriterFacade, csvFieldSeparator, encoding, leafElementsDescription, xmlExpectedElementsXPaths, xmlDiscardedElementsXPaths,
          level, BLEND_MODE, withAttributes, withNamespaces, unleashed);

      // Each of the XML input files are processed in turn.
      if (XML2CSVLoggingFacade.VERBOSE_MODE == true)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, "processing now each XML input file in turn...");
        XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.LINE);
      }
      for (int i = 0; i < xmlInputFiles.length; i++)
      {
        oneXmlinputFile = xmlInputFiles[i];
        if (oneXmlinputFile != null)
        {
          step = 3;

          if (BLEND_MODE == false)
          {
            String oneCsvOutputReferenceName = null;
            int idx = oneXmlinputFile.getName().lastIndexOf(".");
            if (idx != -1) oneCsvOutputReferenceName = oneXmlinputFile.getName().substring(0, idx);
            else
              oneCsvOutputReferenceName = oneXmlinputFile.getName();
            outputWriterFacade.reset();
            outputWriterFacade.initialize(cutoff, csvOutputDir, oneCsvOutputReferenceName, encoding); // Writer facade initialization.

            step = 4;
          }
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, "processing <" + oneXmlinputFile.getName() + "> --|> <" + outputWriterFacade.getOuputFileName() + ">.");
          processOneXMLFile(oneXmlinputFile, dataHandler, outputWriterFacade.getOuputFileName(), outputWriterFacade.getOutputDirURI());
          if (BLEND_MODE == false) outputWriterFacade.close();
        }
        dataHandler.reset();
      }
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "XML --|> CSV complete.");
      XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.LINE);
    }
    catch (Exception e)
    {
      String outputFilename = null;
      if (outputWriterFacade != null) outputFilename = outputWriterFacade.getOuputFileName(); // Null if the OutputWriterFacade constructor failed.
      else if (csvOutputFile != null) outputFilename = csvOutputFile.getName(); // Exists only in case of blend CSV output.
      if (step == 1)
      {
        if (outputFilename != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "could not initialize CSV output associated with CSV output file <" + outputFilename
            + ">. Cause: " + e.getMessage());
        else
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "could not initialize CSV output. Cause: " + e.getMessage());
        errorMessage = "CSV output unreachable."; // Delayed exit until the finally block termination.
      }
      else if (step == 2)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "could not initialize properly the XML data handler. Cause: " + e.getMessage());
        errorMessage = "XML data handler inoperable."; // Delayed exit until the finally block termination.
      }
      else if (step == 3)
      {
        if (outputFilename != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "could not reach CSV output <" + outputFilename + ">. Cause: " + e.getMessage());
        else
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "could not reach CSV output. Cause: " + e.getMessage());
        errorMessage = "CSV output unreachable."; // Delayed exit until the finally block termination.
      }
      else
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "could not process the XML input file <" + oneXmlinputFile.getName() + ">. Cause: " + e.getMessage());
        errorMessage = "XML input file not parsable."; // Delayed exit until the finally block termination.
      }
    }
    finally
    {
      try
      {
        activeOutput = false;
        // Final CSV output closing, unless it is a direct output (if so, left to the caller's responsibility).
        if ((outputWriterFacade != null) && (output == null)) outputWriterFacade.close(); // If the OutputWriterFacade constructor failed then outputWriterFacade is null.
      }
      catch (IOException ioe)
      {
        /* Ignored. */
      }
      if (errorMessage != null) throw new XML2CSVDataException(errorMessage);
    }

    // Program termination.
    if (BLEND_MODE == true)
    {
      String outputAbsolutePath = outputWriterFacade.getOutputAbsolutePath();
      if (outputAbsolutePath != null) XML2CSVLoggingFacade.log(null, "work is done, output file <" + outputAbsolutePath + "> generated - bye.");
      else
        XML2CSVLoggingFacade.log(null, "work is done, output generated - bye.");
    }
    else
      XML2CSVLoggingFacade.log(null, "work is done, all output files generated - bye.");
    XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.EMPTY_STRING);
  }

  /**
   * Computes the XML structure from one XML input file using the ready-to-use {@link utils.xml.xml2csv.StructureHandler StructureHandler} object received for parameter.
   * @param xmlInputFile the XML input file which is analyzed in order to compute the XML structure.
   * @param handler the XML analysis handler to use to compute the XML structure.
   * @return the XML structure, that is, an <code>ElementsDescription</code> instance providing a description of the ordered list of leaf elements in the input XML files.
   * @throws Exception in case of error.
   */
  private ElementsDescription computeXMLStructure(File xmlInputFile, StructureHandler handler) throws Exception
  {
    ElementsDescription result = null;

    FileInputStream fis = null; // Input stream plugged onto the XML input file.
    try
    {
      // Validation without Document Object Model (DOM) for an optimized memory usage.
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);

      SAXParser parser = null;
      try
      {
        parser = factory.newSAXParser();
      }
      catch (SAXException se)
      {
        XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.EMPTY_STRING);
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "problem with the xml file itself: " + se.getMessage()); // Problem with the XML file itself.
        throw se;
      }

      // Plugs the XML reader onto the structure analysis handler.
      XMLReader reader = parser.getXMLReader();
      reader.setContentHandler(handler);
      // "Entity References" parsing issue (example: &lt; , ...).
      // Please refer to http://www.coderanch.com/t/545548/XML/Entities-attribute-values-Sax-parser , http://cafeconleche.org/books/xmljava/chapters/ch07s04.html ,
      // http://xerces.apache.org/xerces2-j/features.html , http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML
      // and to a lesser extent the Java API doc of the LexicalHandler (see http://www.saxproject.org/apidoc/org/xml/sax/ext/LexicalHandler.html, method startEntity).
      // When set to false, entity references (see http://www.w3schools.com/xml/xml_syntax.asp § "Entity References") are not transformed while an input file is read,
      // that is &gt; is not transformed into the > character, which is the exact behavior needed in order avoid generating output files with misplaced XML control
      // characters.
      // Note: should work without extra coding in a declarative way, but it doesn't. Instead, the underneath data handler were updated in order to call a dedicated
      // EscapeUtils method in charge of the the issue (re-transforms XML control characters in text back to neutral "Entity References").
      reader.setFeature("http://xml.org/sax/features/lexical-handler/parameter-entities", false);
      reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

      reader.setErrorHandler(new ErrorHandler()
      {
        public void warning(SAXParseException e) throws SAXException
        {
          XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.EMPTY_STRING);
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, e.getMessage());
        }

        public void error(SAXParseException e) throws SAXException
        {
          XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.EMPTY_STRING);
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, e.getMessage());
          throw e;
        }

        public void fatalError(SAXParseException e) throws SAXException
        {
          XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.EMPTY_STRING);
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.FATAL, e.getMessage());
          throw e;
        }
      });

      fis = new FileInputStream(xmlInputFile);
      reader.parse(new InputSource(fis)); // Parses the XML input file and builds an internal XPath structure for all leaf elements.

      // When the parsing is done the actual XML structure is read from the handler (through convenience getters methods).
      result = handler.getLeafElementsDescription();
      int leafElementCount = result.getElementCount();
      if ((XML2CSVLoggingFacade.DEBUG_MODE == true) && (XML2CSVLoggingFacade.debugDegree >= XML2CSVLogLevel.DEBUG2.getDegree()))
      {
        for (int i = 0; i < leafElementCount; i++)
        {
          XML2CSVLoggingFacade.log(
              XML2CSVLogLevel.DEBUG2,
              "computeXMLStructure: leaf element <" + i + ">: XPath=<" + result.getElementsXPaths()[i] + "> parent XPath=<" + result.getElementsParentXPaths()[i]
                  + "> short name=<" + result.getElementsShortNames()[i] + "> cardinality=<" + result.getElementsCardinalities()[i].getCode() + "> type=<"
                  + result.getElementsTypes()[i].getCode() + ">");
        }
      }
      return result;
    }
    catch (SAXException exc)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "Process of file <" + xmlInputFile.toURI() + "> interrupted.");
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "SAXException error: " + exc.getMessage());
      exc.printStackTrace();
      synchronized (this)
      {
        // Waiting for several seconds so that printStackTrace terminates (this method seems to return before everything is sent to the console).
        try
        {
          this.wait(1000);
        }
        catch (InterruptedException ie)
        {
          /* Ignored. */
        }
      }
      throw exc;
    }
    catch (FileNotFoundException e)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "process of file <" + xmlInputFile.toURI() + "> interrupted due to FileNotFoundException. Cause: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
    catch (IOException e)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "process of file <" + xmlInputFile.toURI() + "> interrupted due to IOException. Cause: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
    catch (Exception e)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "process of file <" + xmlInputFile.toURI() + "> interrupted due to unexpected exception. Cause: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
    finally
    {
      try
      {
        // Neatly closes the XML input file no matter what happened in this method.
        if (fis != null) fis.close();
      }
      catch (Exception e)
      {
        /* Ignored */
      }
    }
  }

  /**
   * Extracts data from one XML input file and writes it to output using the ready-to-use {@link utils.xml.xml2csv.DataHandler DataHandler} object received in parameter.
   * @param xmlInputFile the XML input file to process.
   * @param handler the XML data handler to use to grab data from the XML input file.
   * @param outputFilename the current CSV output filename, if defined, or <code>null</code>.
   * @param outputDir the current output directory, if defined, or <code>null</code>.
   * @throws Exception in case of error.
   */
  private void processOneXMLFile(File xmlInputFile, DataHandler handler, String outputFilename, String outputDir) throws Exception
  {
    FileInputStream fis = null; // Input stream plugged onto the XML input file.
    try
    {
      if ((outputFilename != null) && (outputDir != null)) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "extracting data from <" + xmlInputFile.toURI()
          + "> and appending it to <" + outputFilename + "> in <" + outputDir + "> ... ");
      else if (outputFilename != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "extracting data from <" + xmlInputFile.toURI() + "> and appending it to <" + outputFilename
          + "> ... ");
      else
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "extracting data from <" + xmlInputFile.toURI() + "> and appending it to output ... ");

      // Validation without Document Object Model (DOM) for an optimized memory usage.
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);

      SAXParser parser = null;
      try
      {
        parser = factory.newSAXParser();
      }
      catch (SAXException se)
      {
        XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.EMPTY_STRING);
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "problem with the xml file itself: " + se.getMessage()); // Problem in the XML itself.
        throw se;
      }

      // Plugs the XML reader onto the data process handler.
      XMLReader reader = parser.getXMLReader();
      reader.setContentHandler(handler);
      // "Entity References" parsing issue (example: &lt; , ...).
      // Please refer to http://www.coderanch.com/t/545548/XML/Entities-attribute-values-Sax-parser , http://cafeconleche.org/books/xmljava/chapters/ch07s04.html ,
      // http://xerces.apache.org/xerces2-j/features.html , http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML
      // and to a lesser extent the Java API doc of the LexicalHandler (see http://www.saxproject.org/apidoc/org/xml/sax/ext/LexicalHandler.html, method startEntity).
      // When set to false, entity references (see http://www.w3schools.com/xml/xml_syntax.asp § "Entity References") are not transformed while an input file is read,
      // that is &gt; is not transformed into the > character, which is the exact behavior needed in order avoid generating output files with misplaced XML control characters.
      // Note: should work without extra coding in a declarative way, but it doesn't. Instead, the underneath data handler was updated in order to call a dedicated
      // EscapeUtils method in charge of the the issue (that is: the re-transforming of XML control characters in text read back to neutral "Entity References").
      reader.setFeature("http://xml.org/sax/features/lexical-handler/parameter-entities", false);
      reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

      reader.setErrorHandler(new ErrorHandler()
      {
        public void warning(SAXParseException e) throws SAXException
        {
          XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.EMPTY_STRING);
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, e.getMessage());
        }

        public void error(SAXParseException e) throws SAXException
        {
          XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.EMPTY_STRING);
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, e.getMessage());
          throw e;
        }

        public void fatalError(SAXParseException e) throws SAXException
        {
          XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.EMPTY_STRING);
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.FATAL, e.getMessage());
          throw e;
        }
      });

      fis = new FileInputStream(xmlInputFile);
      reader.parse(new InputSource(fis)); // Parses the XML input file and writes to the CSV output at the same time.

      if (outputFilename != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "done appending data to <" + outputFilename + ">.");
      else
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "done appending data to the output.");
      XML2CSVLoggingFacade.log(null, null, XML2CSVMisc.LINE);
    }
    catch (SAXException exc)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "process of file <" + xmlInputFile.toURI() + "> interrupted.");
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "SAXException error: " + exc.getMessage());
      exc.printStackTrace();
      synchronized (this)
      {
        // Waiting for several seconds so that printStackTrace terminates (this method seems to return before everything is sent to the console).
        try
        {
          this.wait(1000);
        }
        catch (InterruptedException ie)
        {
          /* Ignored. */
        }
      }
      throw exc;
    }
    catch (FileNotFoundException e)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "process of file <" + xmlInputFile.toURI() + "> interrupted due to FileNotFoundException. Cause: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
    catch (IOException e)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "process of file <" + xmlInputFile.toURI() + "> interrupted due to IOException. Cause: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
    catch (Exception e)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "process of file <" + xmlInputFile.toURI() + "> interrupted due to unexpected exception. Cause: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
    finally
    {
      try
      {
        // Neatly closes the XML input file no matter what happened in this method.
        if (fis != null) fis.close();
      }
      catch (IOException ioe)
      {
        /* Ignored. */
      }
    }
  }

  /**
   * Uses the <code>OutputStream</code> provided.<br>
   * The instance will be marked <i>output ready</i>, if not already.
   * @param output an <code>OutputStream</code>.
   * @throws XML2CSVParameterException if parameter <code>output</code> is <code>null</code>.
   */
  public void setOutputStream(OutputStream output) throws XML2CSVParameterException
  {
    this.output = output;
    this.csvOutputFile = null;
    this.csvOutputDir = null;
    if (output != null) BLEND_MODE = true;
    else
      throw new XML2CSVParameterException("No output stream.");
    setCutoff(-1L); // No cutoff when a direct output is used.
    activeOutput = true;
  }

  /**
   * Uses the CSV output file provided.<br>
   * The instance will be marked <i>output ready</i>, if not already.
   * @param csvOutputFile a <code>File</code>.
   * @throws XML2CSVParameterException if parameter <code>csvOutputFile</code> is <code>null</code>.
   */
  public void setOutputFile(File csvOutputFile) throws XML2CSVParameterException
  {
    this.output = null;
    this.csvOutputFile = csvOutputFile;
    this.csvOutputDir = null;
    if (csvOutputFile != null) BLEND_MODE = true;
    else
      throw new XML2CSVParameterException("No output file.");
    activeOutput = true;
  }

  /**
   * Uses the CSV output directory provided.<br>
   * The instance will be marked <i>output ready</i>, if not already.
   * @param csvOutputDir the <code>File</code> representing the directory.
   * @throws XML2CSVParameterException if parameter <code>csvOutputDir</code> is <code>null</code>.
   */
  public void setOutputDir(File csvOutputDir) throws XML2CSVParameterException
  {
    this.output = null;
    this.csvOutputFile = null;
    this.csvOutputDir = csvOutputDir;
    if (csvOutputDir != null) BLEND_MODE = false;
    else
      throw new XML2CSVParameterException("No output directory.");
    activeOutput = true;
  }

  /**
   * Uses the CSV field separator provided.<br>
   * Defaults to character <code>;</code> if left <code>null</code>.
   * @param csvFieldSeparator the field separator to use, or <code>null</code>.
   */
  public void setFieldSeparator(String csvFieldSeparator)
  {
    if (csvFieldSeparator == null) this.csvFieldSeparator = XML2CSVMisc.DEFAULT_FIELD_SEPARATOR;
    else
      this.csvFieldSeparator = csvFieldSeparator;
  }

  /**
   * Uses the character encoding provided.<br>
   * Defaults to {@link utils.xml.xml2csv.constants.XML2CSVMisc#UTF8 UTF8} if left <code>null</code>.
   * @param encoding the character encoding, or <code>null</code>.
   */
  public void setEncoding(Charset encoding)
  {
    if (encoding == null) this.encoding = Charset.forName(XML2CSVMisc.UTF8);
    else
      this.encoding = encoding;
  }

  /**
   * Uses the optimization level provided.<br>
   * Defaults to {@link utils.xml.xml2csv.constants.XML2CSVOptimization#STANDARD STANDARD} if left <code>null</code>.
   * @param level the optimization level, or <code>null</code>.
   */
  public void setOptimization(XML2CSVOptimization level)
  {
    if (level != null) this.level = level;
    else
      this.level = XML2CSVOptimization.STANDARD;
  }

  /**
   * Uses the output file cutoff provided.
   * @param cutoff a positive value activates cutoff with the value provided, and zero or a value below zero deactivates cutoff.
   */
  public void setCutoff(long cutoff)
  {
    this.cutoff = cutoff;
  }

  /**
   * Activates or deactivates name space aware parsing.
   * @param warding <code>true</code> to perform name space aware parsing, and <code>false</code> otherwise.
   */
  public void setWarding(boolean warding)
  {
    this.withNamespaces = warding;
  }

  /**
   * Activates or deactivates root-inclusive optimization.
   * @param unleashing <code>true</code> to perform unleashed optimization which will be root tag inclusive, and <code>false</code> otherwise.
   */
  public void setUnleashing(boolean unleashing)
  {
    this.unleashed = unleashing;
  }
}
