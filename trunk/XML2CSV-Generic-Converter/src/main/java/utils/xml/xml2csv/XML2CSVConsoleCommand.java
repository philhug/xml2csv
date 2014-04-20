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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import utils.xml.xml2csv.constants.XML2CSVLogLevel;
import utils.xml.xml2csv.constants.XML2CSVOptimization;
import utils.xml.xml2csv.constants.XML2CSVMisc;
import utils.xml.xml2csv.exception.XML2CSVCancelException;
import utils.xml.xml2csv.exception.XML2CSVDataException;
import utils.xml.xml2csv.exception.XML2CSVException;
import utils.xml.xml2csv.exception.XML2CSVFilterException;
import utils.xml.xml2csv.exception.XML2CSVParameterException;
import utils.xml.xml2csv.exception.XML2CSVStructureException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

/**
 * XML2CSV console command.<br>
 * Checks & formats command line parameters and, if OK, triggers the corresponding {@link utils.xml.xml2csv.XML2CSVGenericGenerator XML2CSVGenericGenerator} method.
 * @author L. Popieul (lochrann@rocketmail.com)
 * @version 1.0.0
 */
public class XML2CSVConsoleCommand
{
  /** Raw mode (no optimization) indicator. */
  private static boolean RAW_MODE = false;

  /** Extensive optimization indicator. */
  private static boolean EXTENSIVE_MODE = false;

  /** Blend output indicator. */
  private static boolean BLEND_MODE = false;

  /** Attributes extraction indicator. */
  private static boolean withAttributes = false;;

  /** Log4J malfunction indicator. */
  private static boolean log4jFailure = false;

  /** File cutoff malfunction indicator. */
  private static boolean cutoffFailure = false;

  /** Extensive optimization variant choice failure. */
  private static boolean extensiveVariantFailure = false;

  /** Debug degree choice failure. */
  private static boolean debugDegreeFailure = false;

  /** The chosen name of the CSV output file in blend output mode. */
  private static String singleOutputFileName = null;

  /** The input XML file/directory. */
  private static File inputDirOrFile = null;

  /** The directory where the CSV output file is generated. */
  private static File outputDir = null;

  /** The CSV output file in blend output mode. */
  private static File singleOutputFile = null;

  /** The cutoff limit to use in CSV output file(s) expressed as a number of lines, or -1. */
  private static long cutoff = -1L;

  /** The cutoff limit keyed by the user. */
  private static String cutoffValue = null;

  /** The chosen character encoding to use in CSV output file(s). */
  private static String outputEncoding = null;

  /** The chosen path&name of the positive filter file to use. */
  private static String positiveFilterFile = null;

  /** The chosen path&name of the negative filter file to use. */
  private static String negativeFilterFile = null;

  /** A Log4J configuration file. */
  private static String log4JConfigFile = null;

  /** The chosen CSV field separator to use in CSV output file(s). */
  private static String fieldSeparator = null;

  /** The actual character encoding used in CSV output file(s). */
  private static Charset encoding = null;

  /** The name of the XSL file which should serve as structure template. */
  private static String xmlTemplateFileName = null;

  /** The chosen optimization level. */
  private static XML2CSVOptimization level = null;

  /** The XPath list of expected elements (+) in CSV output file(s). */
  private static String[] expectedElementXpathList = null;

  /** The XPath list of elements to discard (-) in CSV output file(s). */
  private static String[] discardedElementXpathList = null;

  /** The ordered list of XML input files to process. */
  private static File[] xmlInputFiles = null;

  /**
   * Displays the online help message.
   */
  private static void displayOnlineHelp()
  {
    System.out.println(XML2CSVMisc.DISPLAY_CLASS_NAME);
    System.out
        .println("Usage: java utils.xml.xml2csv.XML2CSVConsoleCommand [-h] [-m] [-v] [-d[{degree}]] [-a] [-r] [-x[{variant}]] -i {input dir/file} [-t {template filename}] [-o {output dir}] [-b[{blend filename}]] [-e {output encoding}] [-s {output separator}]  [-p {+ filter file}] [-n {- filter file}] [-l[{log4J config}]] [-c{limit}]");
    System.out.println("with:");
    System.out.println("-h or --help: displays this help text, then aborts the program.");
    System.out.println("-m or --mute: mutes the program. In this mode nothing is displayed at runtime. Useful for batch integration where the return code alone matters.");
    System.out.println("-v or --verbose: displays extra progression information at runtime.");
    System.out.println("-d or -d{degree} or --debug{degree}: displays debug information at runtime. An explicit debug degree can be provided (" + XML2CSVLogLevel.DEBUG.getDegree()
        + "," + XML2CSVLogLevel.DEBUG2.getDegree() + " or " + XML2CSVLogLevel.DEBUG3.getDegree() + ") instead of the default <" + XML2CSVLogLevel.DEBUG.getDegree() + ">.");
    System.out.println("-a or --attribute: extracts attributes as well.");
    System.out.println("-r or --raw: raw execution. Inactivates runtime optimization (a default routine which packs data before it is sent to a CSV output file).");
    System.out.println("-x or -x{variant} --extensive{variant}: extensive optimization. Activates enhanced packing behavior. An explicit optimization variant can be provided ("
        + XML2CSVOptimization.EXTENSIVE_V1.getCode() + " or " + XML2CSVOptimization.EXTENSIVE_V2.getCode() + ") instead of the default <"
        + XML2CSVOptimization.EXTENSIVE_V2.getCode() + ">.");
    System.out
        .println("-i {input dir/file} or --input {input dir/file}: relative/absolute path to either a directory containing the XML input files to process, or path to one single XML file to process.");
    System.out
        .println("-t {template filename} or --template {template filename}: name of the XML file from the input directory which should be used as template/model for XML structure analysis. When omitted, defaults automatically to one of the XML input file.");
    System.out
        .println("-o {output dir} or --output {output dir}: path to an existing directory where a CSV output file should be saved. When omitted, defaults to the input directory.");
    System.out
        .println("-b or -b{blend filename} or --blend{blend filename}: blended output. Generates a single CSV output file instead of creating as many CSV output files as XML input files. If a blend filename is provided alongside it will be used instead of the default name <"
            + XML2CSVMisc.DEFAULT_BLEND_OUTPUT_FILENAME + ">.");
    System.out.println("-e {output encoding} or --encoding {output encoding}: the character encoding to use in a CSV output file. When omitted, defaults to: <" + XML2CSVMisc.UTF8
        + ">.");
    System.out.println("-s {output separator} or --separator {output separator}: CSV field separator to use in a CSV output file. When omitted, defaults to: <"
        + XML2CSVMisc.DEFAULT_FIELD_SEPARATOR + ">.");
    System.out
        .println("-p {+ filter file} or --positive {+ filter file}: positive filtering. Path to an <"
            + XML2CSVMisc.UTF8
            + "> encoded text file providing in column an explicit list of element XPaths to extract from the XML files (the rest is discarded). When omitted, all XML leaf elements are automatically extracted.");
    System.out
        .println("-n {- filter file} or --negative {- filter file}: negative filtering. Path to an <"
            + XML2CSVMisc.UTF8
            + "> encoded text file providing in column an explicit list of element XPaths to discard from the XML files (the rest is extracted). When omitted, all XML leaf elements are automatically extracted.");
    System.out
        .println("-l or -l{log4J config} or --log{log4J config}: Log4J logging. If a valid Log4J configuration file is provided alongside it will be used instead of the default built-in <"
            + XML2CSVMisc.DEFAULT_LOG4J_PROPERTY_FILE + ">.");
    System.out.println("-c or -c{limit} or --cutoff{limit}: output file cutoff limit. If a valid number is provided alongside it will be used instead of the default built-in <"
        + XML2CSVMisc.DEFAULT_CUTOFF_LIMIT + "> value to fix a row count threshold for all CSV output files (that is: {limit}x1024 lines).");
    System.out
        .println("Parameter -h overrides any other parameter. Parameter -m overrides -v and -d. Parameter -i is mandatory. Parameters -x and -r are exclusive. Parameters -p and -n are exclusive. Parameter -t is ignored when parameter -i points to one single file.");
    System.out
        .println("Return codes are: 0 - OK; 1 - Execution cancelled - help displayed or no XML file to process; 2 - Execution aborted - bad parameters; 3 - Execution aborted - filter file issue; 4 - Execution aborted - XML structure analysis failure; 5 - Execution aborted - XML data extraction failure; 6 - Execution aborted - Unexpected problem.");
  }

  /**
   * Main class.
   * @param args runtime program arguments.
   */
  public static void main(String[] args)
  {
    try
    {
      // We read & check the input parameters.
      boolean commandlineError = false;
      LongOpt[] longopts = new LongOpt[17];
      longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h');
      longopts[1] = new LongOpt("mute", LongOpt.NO_ARGUMENT, null, 'm');
      longopts[2] = new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v');
      longopts[3] = new LongOpt("debug", LongOpt.OPTIONAL_ARGUMENT, null, 'd');
      longopts[4] = new LongOpt("attribute", LongOpt.NO_ARGUMENT, null, 'a');
      longopts[5] = new LongOpt("raw", LongOpt.NO_ARGUMENT, null, 'r');
      longopts[6] = new LongOpt("extensive", LongOpt.OPTIONAL_ARGUMENT, null, 'x');
      longopts[7] = new LongOpt("input", LongOpt.REQUIRED_ARGUMENT, null, 'i');
      longopts[8] = new LongOpt("template", LongOpt.REQUIRED_ARGUMENT, null, 't');
      longopts[9] = new LongOpt("output", LongOpt.REQUIRED_ARGUMENT, null, 'o');
      longopts[10] = new LongOpt("blend", LongOpt.OPTIONAL_ARGUMENT, null, 'b');
      longopts[11] = new LongOpt("encoding", LongOpt.REQUIRED_ARGUMENT, null, 'e');
      longopts[12] = new LongOpt("separator", LongOpt.REQUIRED_ARGUMENT, null, 's');
      longopts[13] = new LongOpt("positive", LongOpt.REQUIRED_ARGUMENT, null, 'p');
      longopts[14] = new LongOpt("negative", LongOpt.REQUIRED_ARGUMENT, null, 'n');
      longopts[15] = new LongOpt("log", LongOpt.OPTIONAL_ARGUMENT, null, 'l');
      longopts[16] = new LongOpt("cutoff", LongOpt.OPTIONAL_ARGUMENT, null, 'c');

      // An option with optional argument is followed by ::, an option with mandatory argument is followed by : and an option without argument is followed by nothing.
      Getopt g = new Getopt(XML2CSVMisc.DISPLAY_CLASS_NAME, args, "hmvd::arx::i:t:o:b::e:s:p:n:l::c::", longopts, true);
      int option = 0;

      while ((option = g.getopt()) != -1)
      {
        switch (option)
        {
          case 'h':
            displayOnlineHelp();
            System.exit(1);
          case 'm':
            XML2CSVLoggingFacade.MUTE_MODE = true;
            break;
          case 'v':
            XML2CSVLoggingFacade.VERBOSE_MODE = true;
            break;
          case 'd':
            XML2CSVLoggingFacade.DEBUG_MODE = true;
            String degree = g.getOptarg();
            if (degree == null) XML2CSVLoggingFacade.debugDegree = XML2CSVLogLevel.DEBUG.getDegree();
            else if (degree.trim().equals(String.valueOf(XML2CSVLogLevel.DEBUG.getDegree()))) XML2CSVLoggingFacade.debugDegree = XML2CSVLogLevel.DEBUG.getDegree();
            else if (degree.trim().equals(String.valueOf(XML2CSVLogLevel.DEBUG2.getDegree()))) XML2CSVLoggingFacade.debugDegree = XML2CSVLogLevel.DEBUG2.getDegree();
            else if (degree.trim().equals(String.valueOf(XML2CSVLogLevel.DEBUG3.getDegree()))) XML2CSVLoggingFacade.debugDegree = XML2CSVLogLevel.DEBUG3.getDegree();
            else
              debugDegreeFailure = true;
            break;
          case 'a':
            withAttributes = true;
            break;
          case 'r':
            RAW_MODE = true;
            level = XML2CSVOptimization.NONE;
            break;
          case 'x':
            EXTENSIVE_MODE = true;
            String variant = g.getOptarg();
            if (variant == null) level = XML2CSVOptimization.EXTENSIVE_V2;
            else
              level = XML2CSVOptimization.parse(variant);
            if (level == null) extensiveVariantFailure = true;
            break;
          case 'i':
            String pathToInputDirOrFile = g.getOptarg();
            inputDirOrFile = new File(pathToInputDirOrFile);
            break;
          case 't':
            xmlTemplateFileName = g.getOptarg();
            break;
          case 'o':
            String pathToOutputDir = g.getOptarg();
            outputDir = new File(pathToOutputDir);
            break;
          case 'b':
            BLEND_MODE = true;
            singleOutputFileName = g.getOptarg();
            break;
          case 'e':
            outputEncoding = g.getOptarg();
            break;
          case 's':
            fieldSeparator = g.getOptarg();
            break;
          case 'p':
            positiveFilterFile = g.getOptarg();
            break;
          case 'n':
            negativeFilterFile = g.getOptarg();
            break;
          case 'l':
            log4JConfigFile = g.getOptarg();
            if (activateLog4JLog(log4JConfigFile) == false) log4jFailure = true; // Activates the Log4J Log as soon as possible before messages are sent to the logger.
            break;
          case 'c':
            cutoffValue = g.getOptarg();
            if (computeCutoff(cutoffValue) == false) cutoffFailure = true;
            break;
          case '?':
            commandlineError = true;
            break;
          default:
            commandlineError = true;
            break;
        }
      }

      if (XML2CSVLoggingFacade.VERBOSE_MODE == true) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "verbose mode activated. Extra progression messages will be displayed.");
      if (XML2CSVLoggingFacade.DEBUG_MODE == true) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "debug mode activated. Debug messages will be displayed (degree <"
          + XML2CSVLoggingFacade.debugDegree + ">).");
      if (withAttributes == true) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "attributes will be extracted as well.");
      if (RAW_MODE == true) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "raw mode activated. Output file won't be optimized.");
      if ((EXTENSIVE_MODE == true) && (level != null)) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO,
          "extensive mode activated. Extended output optimization variant <" + level.getCode() + "> will be performed.");
      if (BLEND_MODE == true) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "blend mode activated. A single CSV output file will be generated.");
      if (outputEncoding != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "using output encoding <" + outputEncoding + ">.");
      if (fieldSeparator != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "using field separator <" + fieldSeparator + ">.");
      if (positiveFilterFile != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "using positive filter file <" + positiveFilterFile + ">.");
      if (negativeFilterFile != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "using negative filter file <" + negativeFilterFile + ">.");
      if (log4JConfigFile != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "using Log4J with custom configuration file <" + log4JConfigFile + ">.");
      if (cutoff != -1) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "cutoff limit activated. No CSV output file will hold more than <" + cutoff + "> lines.");

      if (log4jFailure == true)
      {
        if (log4JConfigFile != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "failed to activate Log4J with custom configuration file <" + log4JConfigFile + ">.");
        else
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "failed to activate Log4J with default built-in configuration file <" + XML2CSVMisc.DEFAULT_LOG4J_PROPERTY_FILE + ">.");
        commandlineError = true;
      }

      if (cutoffFailure == true)
      {
        if (cutoffValue != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "failed to activate cutoff using the limit <" + cutoffValue + ">.");
        else
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "failed to activate cutoff using the default limit <" + XML2CSVMisc.DEFAULT_CUTOFF_LIMIT + ">.");
        commandlineError = true;
      }

      if (extensiveVariantFailure == true)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "unknown extensive optimization variant.");
        commandlineError = true;
      }

      if (debugDegreeFailure == true)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "unknown debug degree.");
        commandlineError = true;
      }

      if ((RAW_MODE == true) && (EXTENSIVE_MODE == true))
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, "parameters -r and -x are exclusive.");
        commandlineError = true;
      }
      if ((positiveFilterFile != null) && (negativeFilterFile != null))
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, "parameters -p and -n are exclusive.");
        commandlineError = true;
      }
      if (inputDirOrFile == null)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, "parameter -i is missing.");
        commandlineError = true;
      }
      else if ((inputDirOrFile.isFile() == false) && (inputDirOrFile.isDirectory() == false))
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, "parameter -i must be followed by the path to a regular file or directory.");
        commandlineError = true;
      }
      if ((outputDir != null) && (outputDir.isDirectory() == false))
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, "parameter -o must be followed by the path to a regular directory.");
        commandlineError = true;
      }
      if ((xmlTemplateFileName != null) && (inputDirOrFile != null) && (inputDirOrFile.isFile() == true))
      {
        if (xmlTemplateFileName.equals(inputDirOrFile.getName()) == false)
        {
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, "parameter -t is inconsistent with parameter -i and will be ignored.");
          xmlTemplateFileName = null;
        }
      }
      if (commandlineError == true)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "bad parameters.");
        XML2CSVLoggingFacade.log(null, null, "Run the program with -h or --help to get the online help.");
        System.exit(2);
      }
    }
    catch (Exception e)
    {
      if (e.getMessage() != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "error while reading the parameters. Cause: " + e.getMessage());
      else
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "error while reading the parameters.");
      XML2CSVLoggingFacade.log(null, null, "Execution aborted.");
      System.exit(2);
    }

    // We devise the output character encoding.
    if (outputEncoding == null) outputEncoding = XML2CSVMisc.UTF8;
    try
    {
      encoding = Charset.forName(outputEncoding);
    }
    catch (Exception e)
    {
      encoding = Charset.forName(XML2CSVMisc.UTF8);
      Iterator<String> iterator = Charset.availableCharsets().keySet().iterator();
      StringBuffer sb = new StringBuffer();
      sb.append("{");
      while (iterator.hasNext())
      {
        sb.append(iterator.next());
        if (iterator.hasNext()) sb.append(",");
      }
      sb.append("}");
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, "encoding <" + outputEncoding + "> provided unknown/not supported. Using <" + XML2CSVMisc.UTF8
          + "> instead. Supported encodings are " + sb.toString() + ".");
    }
    // Changing the global VM file encoding according to the caller's choice is not a bad idea even though it's not very useful here.
    System.setProperty("file.encoding", encoding.name());

    // We devise the initial list of XML input files to process out of the runtime parameters provided.
    if (inputDirOrFile.isDirectory())
    {
      xmlInputFiles = inputDirOrFile.listFiles(new InputFilenameFilter(XML2CSVMisc.XML));
      if (xmlInputFiles.length == 0)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, null, "no XML files to process in the directory <" + inputDirOrFile.getAbsolutePath() + ">.");
        XML2CSVLoggingFacade.log(null, null, "Execution cancelled.");
        System.exit(1);
      }
    }
    else
    {
      // A single XML file was provided.
      xmlInputFiles = new File[1];
      xmlInputFiles[0] = inputDirOrFile;
    }

    // We choose the default optimization, if explicitly chosen.
    if (level == null) level = XML2CSVOptimization.STANDARD;

    // We device the output directory which, if not already known, is set to the default - that is input directory.
    if (outputDir == null) outputDir = xmlInputFiles[0].getParentFile();

    // We devise the field separator that will be used in the output file which, if not already known, is set to the default - that is ";".
    if (fieldSeparator == null) fieldSeparator = XML2CSVMisc.DEFAULT_FIELD_SEPARATOR;

    // We read the filter file, if any. No filter file means no filter (that is, the CSV output file will contain all XML leaf elements).
    // When a filter file is provided each of its lines provides the XPath to a selected leaf element (example: Root.Row.Data.Amount) and leaf XML elements
    // not explicitly listed will be discarded/filtered in the CSV output file.
    try
    {
      if (positiveFilterFile != null)
      {
        expectedElementXpathList = readFilterFile(positiveFilterFile);
        if (XML2CSVLoggingFacade.VERBOSE_MODE == true)
        {
          StringBuffer sb = new StringBuffer();
          sb.append("the following XML elements will be echoed to the CSV output file:" + XML2CSVMisc.LINE_SEPARATOR);
          for (int i = 0; i < expectedElementXpathList.length - 1; i++)
            sb.append("\t- <" + expectedElementXpathList[i] + ">" + XML2CSVMisc.LINE_SEPARATOR);
          sb.append("\t- <" + expectedElementXpathList[expectedElementXpathList.length - 1] + ">");
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, sb.toString());
        }
      }
      else if (negativeFilterFile != null)
      {
        discardedElementXpathList = readFilterFile(negativeFilterFile);
        if (XML2CSVLoggingFacade.VERBOSE_MODE == true)
        {
          StringBuffer sb = new StringBuffer();
          sb.append("the following XML elements wont be echoed to the CSV output file:" + XML2CSVMisc.LINE_SEPARATOR);
          for (int i = 0; i < discardedElementXpathList.length - 1; i++)
            sb.append("\t- <" + discardedElementXpathList[i] + ">" + XML2CSVMisc.LINE_SEPARATOR);
          sb.append("\t- <" + discardedElementXpathList[discardedElementXpathList.length - 1] + ">");
          XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, sb.toString());
        }
      }
      else
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.VERBOSE, "no filter set. All XML leaf elements will be echoed to the CSV output.");
      }
    }
    catch (IOException ioe)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "error while reading positive filter file <" + positiveFilterFile + ">. Cause: " + ioe.getMessage());
      XML2CSVLoggingFacade.log(null, null, "execution aborted.");
      System.exit(3);
    }

    // We devise the output file in blend output mode.
    if (BLEND_MODE == true)
    {
      if (singleOutputFileName == null)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "using default output file name <" + XML2CSVMisc.DEFAULT_BLEND_OUTPUT_FILENAME + ">.");
        singleOutputFileName = XML2CSVMisc.DEFAULT_BLEND_OUTPUT_FILENAME;
      }
      else
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "using custom output file name <" + singleOutputFileName + "> instead of the default <"
            + XML2CSVMisc.DEFAULT_BLEND_OUTPUT_FILENAME + ">.");
      }
      singleOutputFile = new File(outputDir.getAbsolutePath() + XML2CSVMisc.FILE_SEPARATOR + singleOutputFileName);
      // Idiot proof in all circumstances. We make sure the blend output file does not belong to the input XML list (which might occur with a custom blend CSV output filename
      // awkwardly named like one of the XML input files).
      boolean collision = false;
      for (int i = 0; i < xmlInputFiles.length; i++)
      {
        String oneInputFileName = xmlInputFiles[i].getName();
        if (oneInputFileName.equals(singleOutputFile.getName()))
        {
          collision = true;
          break;
        }
      }
      if (collision == true)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "collision between the blend filename <" + singleOutputFile.getName()
            + "> provided and the name of one of the XML input files.");
        XML2CSVLoggingFacade.log(null, null, "execution aborted.");
        System.exit(2);
      }
      if (singleOutputFile.exists())
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "output file <" + singleOutputFileName + "> already exists and will be replaced.");
        singleOutputFile.delete();
      }
    }

    // If one file has been explicitly chosen for XML structure analysis we make sure it belongs to the list of XML input file to process.
    if (xmlTemplateFileName != null)
    {
      boolean found = false;
      for (int i = 0; i < xmlInputFiles.length; i++)
      {
        String oneInputFileName = xmlInputFiles[i].getName();
        if (oneInputFileName.equals(xmlTemplateFileName))
        {
          found = true;
          break;
        }
      }
      if (found == false)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "filter file <" + xmlTemplateFileName + "> not among selected XML files to process.");
        XML2CSVLoggingFacade.log(null, null, "execution aborted.");
        System.exit(3);
      }
    }

    // XML2CSV generic generator invocation.
    try
    {
      XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(singleOutputFile, outputDir, fieldSeparator, encoding, level, cutoff);
      generator.generate(xmlInputFiles, xmlTemplateFileName, expectedElementXpathList, discardedElementXpathList, withAttributes);
    }
    catch (XML2CSVException e)
    {
      if (e instanceof XML2CSVCancelException)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.WARNING, null, "no XML file to process in directory <" + inputDirOrFile.getAbsolutePath() + ">.");
        XML2CSVLoggingFacade.log(null, null, "Execution cancelled.");
        System.exit(1);
      }
      else if (e instanceof XML2CSVParameterException)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "bad parameters. Cause: " + e.getMessage());
        XML2CSVLoggingFacade.log(null, null, "execution aborted.");
        System.exit(2);
      }
      else if (e instanceof XML2CSVFilterException)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "filter file issue. Cause: " + e.getMessage());
        XML2CSVLoggingFacade.log(null, null, "execution aborted.");
        System.exit(3);
      }
      else if (e instanceof XML2CSVStructureException)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "XML structure analysis failure. Cause: " + e.getMessage());
        XML2CSVLoggingFacade.log(null, null, "execution aborted.");
        System.exit(4);
      }
      else if (e instanceof XML2CSVDataException)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "XML data extraction failure. Cause: " + e.getMessage());
        XML2CSVLoggingFacade.log(null, null, "execution aborted.");
        System.exit(5);
      }
      else
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "Unexpected problem. Cause: " + e.getMessage());
        XML2CSVLoggingFacade.log(null, null, "execution aborted.");
        System.exit(6);
      }
    }
    catch (Throwable t)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "Unexpected problem. Cause: " + t.getMessage());
      XML2CSVLoggingFacade.log(null, null, "execution aborted.");
      System.exit(6);
    }
  }

  /**
   * Reads a filter file containing element XPaths and returns them as a <code>String</code> array.
   * @param filterFile the path & name of the filter file to read.
   * @return the element XPaths read.
   * @throws IOException in case of error.
   */
  private static String[] readFilterFile(String filterFile) throws IOException
  {
    String[] result = null;

    FileInputStream fis = new FileInputStream(filterFile);
    InputStreamReader isr = new InputStreamReader(fis, Charset.forName(XML2CSVMisc.UTF8));
    BufferedReader br = new BufferedReader(isr);
    // A LinkedHashMap is used instead of an ArrayList so that both duplicate entries be automatically eliminated and the entries order be kept.
    // ArrayList<String> l = new ArrayList<String>();
    LinkedHashMap<String, String> l = new LinkedHashMap<String, String>();
    String oneLine = br.readLine();
    while (oneLine != null)
    {
      if ((!oneLine.trim().isEmpty()) && (!oneLine.startsWith("--")))
      {
        // l.add(oneLine);
        l.put(oneLine.trim(), null);
      }
      oneLine = br.readLine();
    }
    br.close();
    if (l.size() == 0) throw new IOException("Filter file does not contain any leaf element XPath.");
    String[] temp = new String[l.size()];
    // expectedNodesList = l.toArray(junk);
    result = l.keySet().toArray(temp);
    return result;
  }

  /**
   * Adds a directory to the current VM <code>CLASSPATH</code>.
   * @param dir a <code>File</code> representing the directory to add.
   * @throws IOException in case of problem.
   */
  private static void addDirToClasspath(File dir) throws IOException
  {
    Class<?>[] parameters = new Class[] { URL.class };
    URI uri = dir.toURI();
    URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    Class<?> sysclass = URLClassLoader.class;
    try
    {
      Method method = sysclass.getDeclaredMethod("addURL", parameters);
      method.setAccessible(true);
      method.invoke(sysloader, new Object[] { uri.toURL() });
    }
    catch (Throwable t)
    {
      throw new IOException("Could not add directory <" + dir.getName() + "> to the CLASSPATH.");
    }
  }

  /**
   * Computes the actual line cutoff to use, expressed as a line count:<br>
   * <ul>
   * <li>if positive, multiplies the number keyed by the end user by <code>1024</code>.<br>
   * <li>if left <code>null</code>, uses the default built-in cutoff value.<br>
   * <li>if negative or impossible to parse as a number, deactivates cutoff by setting the actual limit to <code>-1</code>.
   * </ul>
   * @param cutoffValue the cutoff value keyed by the end user as a string, or <code>null</code>.
   * @return <code>true</code> if a valid cutoff limit was computed from the the value keyed by the user or if the default cutoff limit was used, and <code>false</code> otherwise.
   */
  private static boolean computeCutoff(String cutoffValue)
  {
    boolean result = true;
    if (cutoffValue == null) cutoff = XML2CSVMisc.DEFAULT_CUTOFF_LIMIT * 1024L;
    else
    {
      try
      {
        cutoff = Long.parseLong(cutoffValue) * 1024L;
      }
      catch (Exception e)
      {
        cutoff = -1L;
        result = false;
      }
      if (cutoff <= 0)
      {
        cutoff = -1L;
        result = false;
      }
    }
    return result;
  }

  /**
   * Activates the Log4J logger.
   * @param log4JConfigFile the Log4J configuration file (expected in <code>UTF-8</code>), or <code>null</code> to load the default built-in Log4J configuration file.
   * @return <code>true</code> if the Log4J Log was successfully activated, and <code>false</code> otherwise.
   */
  private static boolean activateLog4JLog(String log4JConfigFile)
  {
    String configFileName = null;
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    // The Log4J library configuration is reset.
    BasicConfigurator.resetConfiguration();

    // A custom Log4J configuration file is added to the CLASSPATH to make the resource available just like the built-in Log4J property file.
    if (log4JConfigFile != null)
    {
      File customLog4JFile = new File(log4JConfigFile);
      File customFileDir = customLog4JFile.getParentFile();
      if (customFileDir == null)
      {
        // If a junk custom Log4J configuration file was provided we will end up with no parent directory.
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "incorrect custom Log4J config file.");
        return false;
      }
      try
      {
        addDirToClasspath(customFileDir);
      }
      catch (IOException ioe)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "error while reading the custom Log4J config file. Cause: " + ioe.getMessage());
        return false;
      }
      configFileName = customLog4JFile.getName();
    }
    else
    {
      configFileName = XML2CSVMisc.DEFAULT_LOG4J_PROPERTY_FILE;
    }

    // The custom Log4J configuration file or the built-in Log4J configuration file is reached through the CLASSPATH and opened.
    InputStream inputStream = classLoader.getResourceAsStream(configFileName);

    // The custom Log4J configuration file is loaded as a Properties instance.
    Properties configFile = new Properties();
    if (inputStream != null)
    {
      try
      {
        // The custom Log4J property file is expected UTF-8 encoded.
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName(XML2CSVMisc.UTF8));
        configFile.load(inputStreamReader);
      }
      catch (IOException e)
      {
        XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "error while reading the Log4J config file. Cause: " + e.getMessage());
        return false;
      }
    }
    else
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "could not read the Log4J config file.");
      return false;
    }

    // The Log4J library is configured according to the custom Log4J configuration file.
    try
    {
      PropertyConfigurator.configure(configFile);
      Log log = LogFactory.getLog(XML2CSVMisc.LOG4J_LOGGER);
      XML2CSVLoggingFacade.log = log;
    }
    catch (Exception e)
    {
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.ERROR, "could find/load the Log4J <" + XML2CSVMisc.LOG4J_LOGGER + "> logger.");
      return false;
    }
    if (log4JConfigFile != null) XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "Log4J activated with custom configuration file <" + log4JConfigFile + ">.");
    else
      XML2CSVLoggingFacade.log(XML2CSVLogLevel.INFO, "Log4J activated with built-in configuration file <" + configFileName + ">.");
    return true;
  }
}
