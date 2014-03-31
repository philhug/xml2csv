package utils.xml.xml2csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
  private File outputFile = null;

  /** File used for blend output file test. */
  private File customOutputFile = null;

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
    Assert.assertNotNull("Test file 2 missing  in test resources directory", getClass().getResource("/sample2.xml"));
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
    outputFile = testFolder.newFile("output.csv");
    customOutputFile = testFolder.newFile("customOutput.csv");
    outputDir = testFolder.newFolder("outputDir");
    logFile = testFolder.newFile("XML2CSV-Generic-Converter.log"); // Generated at the project's root.

    customLog4JFile = new File(getClass().getResource("/xml2csvlog4j.properties").toURI());
    FileInputStream fis = new FileInputStream(customLog4JFile);
    Properties props = new Properties();
    props.load(fis);
    PropertyConfigurator.configure(props);
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
    File expectedOutputFile = new File(getClass().getResource("/output.csv").toURI());
    File[] inputs = new File[2];
    inputs[0] = sample1;
    inputs[1] = sample2;

    FileOutputStream fos = new FileOutputStream(outputFile);

    XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(fos);
    generator.setOptimization(XML2CSVOptimization.EXTENSIVE_V1);
    generator.generate(inputs, true);
    generator.setOutputStream(fos);
    generator.generate(inputs, true);
    fos.close();

    Assert.assertEquals(FileUtils.readFileToString(expectedOutputFile, "utf-8"), FileUtils.readFileToString(outputFile, "utf-8"));
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
    File expectedCustomOutputFile = new File(getClass().getResource("/customOutput.csv").toURI());
    File[] inputs = new File[2];
    inputs[0] = sample1;
    inputs[1] = sample2;

    XML2CSVGenericGenerator generator = new XML2CSVGenericGenerator(customOutputFile, null);
    generator.setOptimization(XML2CSVOptimization.EXTENSIVE_V2);
    generator.generate(inputs, true);

    Assert.assertEquals(FileUtils.readFileToString(expectedCustomOutputFile, "utf-8"), FileUtils.readFileToString(customOutputFile, "utf-8"));
  }

  /**
   * Deletion of temporary files & directories after JUnit test.
   * @throws Exception in case of error.
   */
  @After
  public void clean() throws Exception
  {
    outputFile.delete();
    customOutputFile.delete();
    File[] contents = outputDir.listFiles();
    for (int i = 0; i < contents.length; i++)
      contents[i].delete();
    outputDir.delete();
    if (logFile.exists()) logFile.deleteOnExit();
  }
}
