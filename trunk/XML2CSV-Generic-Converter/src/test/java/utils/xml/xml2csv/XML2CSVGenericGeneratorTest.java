package utils.xml.xml2csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;

import org.junit.rules.TemporaryFolder;

import utils.xml.xml2csv.constants.XML2CSVMisc;
import utils.xml.xml2csv.constants.XML2CSVOptimization;

/**
 * This class is responsible for automated JUnit <code>XML2CSVGenericGenerator</code> tests.<br>
 * JUnit tips about access to local resources might be found <a
 * href="http://devblog.virtage.com/2013/07/how-to-get-file-resource-from-maven-srctestresources-folder-in-junit-test/">here</a>.<br>
 * Other JUnit tips concerning temporary files & directories might be found
 * <a href="http://garygregory.wordpress.com/2010/01/20/junit-tip-use-rules-to-manage-temporary-files-and-folders/">there</code>.
 */
public class XML2CSVGenericGeneratorTest
{
  /** File used for direct output stream test. */
  private File outputFile1 = null;

  /** File used for blend output file test. */
  private File customOutputFile2 = null;

  /** File used for positive output stream test. */
  private File outputPositiveFile3 = null;

  /** File used for negative output stream test. */
  private File outputNegativeFile3 = null;

  /** File used for name space aware negative output stream test. */
  private File outputNegativeFile4 = null;

  /** File used for unleashed maximized optimization test. */
  private File outputFile5 = null;

  /** Test Log4J configuration file. */
  private File customLog4JFile = null;

  /** Directory used for output directory test. */
  private File outputDir = null;

  /** The log file created by Log4J. */
  private File logFile = null;

  /**
   * Preparation of JUnit tests: checks that expected auxiliary files are present in the test resources directory.
   */
  @Test
  public void checkResources()
  {
    Assert.assertNotNull("Test file 1 missing in test resources directory", getClass().getResource("/sample1.xml"));
    Assert.assertNotNull("Test file 2 missing in test resources directory", getClass().getResource("/sample2.xml"));
    Assert.assertNotNull("Test file 3 missing in test resources directory", getClass().getResource("/sample3.xml"));
    Assert.assertNotNull("Test file 4 missing in test resources directory", getClass().getResource("/sample4.xml"));
    Assert.assertNotNull("Test file 5 missing in test resources directory", getClass().getResource("/sample5.xml"));
    Assert.assertNotNull("Filter file 3 missing in test resources directory", getClass().getResource("/filterFile3.txt"));
    Assert.assertNotNull("Filter file 4 missing in test resources directory", getClass().getResource("/filterFile4.txt"));
    Assert.assertNotNull("Result file 'output1.csv' missing in test resources directory", getClass().getResource("/output1.csv"));
    Assert.assertNotNull("Result file 'customOutput2.csv' missing in test resources directory", getClass().getResource("/customOutput2.csv"));
    Assert.assertNotNull("Result file 'output3p.csv' missing in test resources directory", getClass().getResource("/output3p.csv"));
    Assert.assertNotNull("Result file 'output3n.csv' missing in test resources directory", getClass().getResource("/output3n.csv"));
    Assert.assertNotNull("Result file 'output4n.csv' missing in test resources directory", getClass().getResource("/output4n.csv"));
    Assert.assertNotNull("Result file 'output5.csv' missing in test resources directory", getClass().getResource("/output5.csv"));
    Assert.assertNotNull("Log4J configuration file in test resources directory", getClass().getResource("/xml2csvlog4j.properties"));
  }

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  /**
   * Preparation of JUnit tests: temporary directory, Log4J logger construction.
   * @throws Exception in case of error.
   */
  @Before
  public void setUp() throws Exception
  {
    outputFile1 = testFolder.newFile("output1.csv");
    customOutputFile2 = testFolder.newFile("customOutput2.csv");
    outputPositiveFile3 = testFolder.newFile("output3p.csv");
    outputNegativeFile3 = testFolder.newFile("output3n.csv");
    outputNegativeFile4 = testFolder.newFile("output4n.csv");
    outputFile5 = testFolder.newFile("output5.csv");
    outputDir = testFolder.newFolder("outputDir");
    logFile = testFolder.newFile("XML2CSV-Generic-Converter.log"); // Generated at the project's root.

    customLog4JFile = new File(getClass().getResource("/xml2csvlog4j.properties").toURI());
    FileInputStream fis = new FileInputStream(customLog4JFile);
    Properties props = new Properties();
    props.load(fis);
    PropertyConfigurator.configure(props);
    fis.close();
    XML2CSVLoggingFacade.log = LogFactory.getLog("XML2CSV-Generic-Converter");
    XML2CSVLoggingFacade.VERBOSE_MODE = true;
  }

  /**
   * Tests XML to CSV conversion of two files two times in a row using a direct <code>OutputStream</code>.
   * @throws Exception in case of error.
   */
  @Test
  public void testOutputStreamGeneration() throws Exception
  {
    File sample1 = new File(getClass().getResource("/sample1.xml").toURI());
    File sample2 = new File(getClass().getResource("/sample2.xml").toURI());
    File expectedOutputFile1 = new File(getClass().getResource("/output1.csv").toURI());
    File[] inputs = new File[2];
    inputs[0] = sample1;
    inputs[1] = sample2;

    FileOutputStream fos = new FileOutputStream(outputFile1);

    XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(fos);
    generator.setOptimization(XML2CSVOptimization.EXTENSIVE_V1);
    generator.generate(inputs, true);
    generator.setOutputStream(fos);
    generator.generate(inputs, true);
    fos.close();

    Assert.assertEquals(FileUtils.readFileToString(expectedOutputFile1, "utf-8"), FileUtils.readFileToString(outputFile1, "utf-8"));
  }

  /**
   * Tests XML to CSV conversion of two files in an output directory, letting the converter name the output files after the XML input files.
   * @throws Exception in case of error.
   */
  @Test
  public void testFilesGeneration() throws Exception
  {
    File sample1 = new File(getClass().getResource("/sample1.xml").toURI());
    File sample2 = new File(getClass().getResource("/sample2.xml").toURI());
    File[] inputs = new File[2];
    inputs[0] = sample1;
    inputs[1] = sample2;

    XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(null, outputDir);
    generator.setOptimization(XML2CSVOptimization.EXTENSIVE_V2);
    generator.generate(inputs, true);
  }

  /**
   * Tests XML to CSV conversion of two files in a blend output file.
   * @throws Exception in case of error.
   */
  @Test
  public void testBlendFileGeneration() throws Exception
  {
    File sample1 = new File(getClass().getResource("/sample1.xml").toURI());
    File sample2 = new File(getClass().getResource("/sample2.xml").toURI());
    File expectedCustomOutputFile2 = new File(getClass().getResource("/customOutput2.csv").toURI());
    File[] inputs = new File[2];
    inputs[0] = sample1;
    inputs[1] = sample2;

    XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(customOutputFile2, null);
    generator.setOptimization(XML2CSVOptimization.EXTENSIVE_V2);
    generator.generate(inputs, true);

    Assert.assertEquals(FileUtils.readFileToString(expectedCustomOutputFile2, "utf-8"), FileUtils.readFileToString(customOutputFile2, "utf-8"));
  }

  /**
   * Tests XML to CSV conversion of one file containing both elements and attributes against a positive filter file, with an explicit CSV output file name.
   * @throws Exception in case of error.
   */
  @Test
  public void testFilterFilePositiveGeneration() throws Exception
  {
    File expectedPositiveOutputFile3 = new File(getClass().getResource("/output3p.csv").toURI());
    File sample3 = new File(getClass().getResource("/sample3.xml").toURI());
    File[] inputs = new File[1];
    inputs[0] = sample3;
    File filterFile3 = new File(getClass().getResource("/filterFile3.txt").toURI());
    String[] xpaths = readFilterFile(filterFile3);

    XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(outputPositiveFile3, null);
    generator.setOptimization(XML2CSVOptimization.EXTENSIVE_V2);
    generator.generate(inputs, null, xpaths, null, true);

    Assert.assertEquals(FileUtils.readFileToString(expectedPositiveOutputFile3, "utf-8"), FileUtils.readFileToString(outputPositiveFile3, "utf-8"));
  }

  /**
   * Tests XML to CSV conversion of one file containing both elements and attributes against a negative filter file, with an explicit CSV output file name.
   * @throws Exception in case of error.
   */
  @Test
  public void testFilterFileNegativeGeneration() throws Exception
  {
    File expectedNegativeOutputFile3 = new File(getClass().getResource("/output3n.csv").toURI());
    File sample3 = new File(getClass().getResource("/sample3.xml").toURI());
    File[] inputs = new File[1];
    inputs[0] = sample3;
    File filterFile3 = new File(getClass().getResource("/filterFile3.txt").toURI());
    String[] xpaths = readFilterFile(filterFile3);

    XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(outputNegativeFile3, null);
    generator.setOptimization(XML2CSVOptimization.EXTENSIVE_V2);
    generator.generate(inputs, null, null, xpaths, true);

    Assert.assertEquals(FileUtils.readFileToString(expectedNegativeOutputFile3, "utf-8"), FileUtils.readFileToString(outputNegativeFile3, "utf-8"));
  }

  /**
   * Tests name space aware XML to CSV conversion of one file containing both elements and attributes against a negative filter file, with an explicit CSV output file name.
   * @throws Exception in case of error.
   */
  @Test
  public void testNamespacesFilterFileNegativeGeneration() throws Exception
  {
    File expectedNegativeOutputFile4 = new File(getClass().getResource("/output4n.csv").toURI());
    File sample4 = new File(getClass().getResource("/sample4.xml").toURI());
    File[] inputs = new File[1];
    inputs[0] = sample4;
    File filterFile4 = new File(getClass().getResource("/filterFile4.txt").toURI());
    String[] xpaths = readFilterFile(filterFile4);

    XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(outputNegativeFile4, null);
    generator.setOptimization(XML2CSVOptimization.EXTENSIVE_V2);
    generator.setWarding(true);
    generator.generate(inputs, null, null, xpaths, true);

    Assert.assertEquals(FileUtils.readFileToString(expectedNegativeOutputFile4, "utf-8"), FileUtils.readFileToString(outputNegativeFile4, "utf-8"));
  }

  /**
   * Tests unleashed maximized XML to CSV conversion of one file containing both elements and attributes.
   * @throws Exception in case of error.
   */
  @Test
  public void testUnleashedGeneration() throws Exception
  {
    File expectedOutputFile5 = new File(getClass().getResource("/output5.csv").toURI());
    File sample5 = new File(getClass().getResource("/sample5.xml").toURI());
    File[] inputs = new File[1];
    inputs[0] = sample5;

    XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(outputFile5, null);
    generator.setOptimization(XML2CSVOptimization.EXTENSIVE_V3);
    generator.setUnleashing(true);
    generator.generate(inputs, true);

    Assert.assertEquals(FileUtils.readFileToString(expectedOutputFile5, "utf-8"), FileUtils.readFileToString(outputFile5, "utf-8"));
  }

  /**
   * Deletion of temporary files & directories after the JUnit test runs.
   * @throws Exception in case of error.
   */
  @After
  public void clean() throws Exception
  {
    outputFile1.delete();
    customOutputFile2.delete();
    outputPositiveFile3.delete();
    outputNegativeFile3.delete();
    outputNegativeFile4.delete();
    outputFile5.delete();
    File[] contents = outputDir.listFiles();
    for (int i = 0; i < contents.length; i++)
      contents[i].delete();
    outputDir.delete();
    LogFactory.releaseAll();
    if (logFile.exists()) logFile.deleteOnExit();
  }

  /**
   * Reads a filter file containing element XPaths and returns them as a <code>String</code> array.
   * @param filterFile the filter file to read, provided as a <code>File</code>.
   * @return the element XPaths read.
   * @throws IOException in case of error.
   */
  private static String[] readFilterFile(File filterFile) throws IOException
  {
    String[] result = null;

    FileInputStream fis = new FileInputStream(filterFile);
    InputStreamReader isr = new InputStreamReader(fis, Charset.forName(XML2CSVMisc.UTF8));
    BufferedReader br = new BufferedReader(isr);
    LinkedHashMap<String, String> l = new LinkedHashMap<String, String>();
    String oneLine = br.readLine();
    while (oneLine != null)
    {
      if ((!oneLine.trim().isEmpty()) && (!oneLine.startsWith("--")))
      {
        l.put(oneLine.trim(), null);
      }
      oneLine = br.readLine();
    }
    br.close();
    if (l.size() == 0) throw new IOException("Filter file does not contain any leaf element XPath.");
    String[] temp = new String[l.size()];
    result = l.keySet().toArray(temp);
    return result;
  }
}
