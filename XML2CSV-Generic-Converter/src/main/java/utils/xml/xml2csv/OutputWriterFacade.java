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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import utils.xml.xml2csv.constants.XML2CSVMisc;

/**
 * CSV output file writer facade.<br>
 * This class nests the logic making it possible to shift transparently from one CSV output file to the next when the current CSV output file reaches a certain line count cutoff.<br>
 * This behavior is deactivated when a direct <code>OutputStream</code> is provided.
 * @author L. Popieul (lochrann@rocketmail.com)
 */
class OutputWriterFacade
{
  /** File cutoff indicator. */
  private long cutoff = -1L;

  /** The current CSV output destination. */
  private OutputStreamWriter outputStreamWriter = null;

  /** The output directory. */
  private File csvOutputDir = null;

  /** The output file. */
  private File csvOutputFile = null;

  /** Reference output filename (filename without extension). */
  private String csvReferenceName = null;

  /** The output character encoding. */
  private Charset encoding = null;

  /** Output line counter. */
  private long lineCounter = 0L;

  /** Output file counter. */
  private int fileCounter = 0;

  /** Ready to send output indicator. */
  private boolean ready = false;

  /**
   * <code>OutputWriterFacade</code> constructor.<br>
   * The <code>OutputWriterFacade</code> won't be {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} until the appropriate <code>initialize</code> method is called to
   * configure the output.
   * @throws IOException in case of error.
   */
  public OutputWriterFacade() throws IOException
  {
    super();
    ready = false;
  }

  /**
   * <code>OutputWriterFacade</code> constructor.<br>
   * Plugs the CSV output file writer facade onto a direct <code>OutputStream</code>.<br>
   * The <code>OutputWriterFacade</code> will be {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.<br>
   * Using direct output automatically deactivates line count cutoff behavior.
   * @param outputStream a direct output stream.
   * @param encoding the character encoding to use in outputs.
   * @throws IOException in case of error.
   */
  public OutputWriterFacade(OutputStream outputStream, Charset encoding) throws IOException
  {
    super();
    initialize(outputStream, encoding);
  }

  /**
   * <code>OutputWriterFacade</code> constructor.<br>
   * Plugs the CSV output file writer facade onto the CSV output file provided.<br>
   * The <code>OutputWriterFacade</code> will be {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.
   * @param cutoff any positive long integer to set a line count cutoff accordingly, or <code>-1</code> to deactivate cutoff.
   * @param csvOutputFile the CSV output file.
   * @param encoding the character encoding to use in outputs.
   * @throws IOException in case of error.
   */
  public OutputWriterFacade(long cutoff, File csvOutputFile, Charset encoding) throws IOException
  {
    super();
    initialize(cutoff, csvOutputFile, encoding);
  }

  /**
   * <code>OutputWriterFacade</code> constructor.<br>
   * Lets the CSV output file writer facade manage a CSV output file named after the reference name provided, generated in the CSV output directory provided.<br>
   * The <code>OutputWriterFacade</code> will be {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.
   * @param cutoff any positive long integer to set a line count cutoff accordingly, or <code>-1</code> to deactivate cutoff.
   * @param csvOutputDir the CSV output directory.
   * @param csvReferenceName the CSV reference filename, that is, the expected CSV filename without extension.
   * @param encoding the character encoding to use in outputs.
   * @throws IOException in case of error.
   */
  public OutputWriterFacade(long cutoff, File csvOutputDir, String csvReferenceName, Charset encoding) throws IOException
  {
    super();
    initialize(cutoff, csvOutputDir, csvReferenceName, encoding);
  }

  /**
   * Writes a string to the output, that is, the current CSV output file or, if a cutoff limit is set and the current CSV file is full, to the next CSV file.<br>
   * @param str the string to write to the output.
   * @throws IOException in case of error, or if the <code>OutputWriterFacade</code> instance is not {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.
   */
  public void write(String str) throws IOException
  {
    if (ready == false) throw new IOException("Not ready.");
    if (cutoff <= 0)
    {
      // No cutoff limit set: everything goes to the current CSV file.
      outputStreamWriter.write(str);
    }
    else
    {
      if (lineCounter <= cutoff)
      {
        // Cutoff limit set, not reached yet; the string goes to the current CSV output file.
        outputStreamWriter.write(str);
        if (str.equals(XML2CSVMisc.LINE_SEPARATOR)) lineCounter++; // Counting the line separators = counting the lines.
      }
      else
      {
        // Cutoff limit set, and reached.
        nextFile();
        outputStreamWriter.write(str);
        lineCounter = 1;
      }
    }
  }

  /**
   * CSV output file shift, when the previous one is full.
   * @throws IOException in case of error, or if the <code>OutputWriterFacade</code> instance is not {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.
   */
  private void nextFile() throws IOException
  {
    if (ready == false) throw new IOException("Not ready.");
    outputStreamWriter.flush();
    outputStreamWriter.close();
    fileCounter++;
    File csvOutputFile = new File(csvOutputDir.getAbsolutePath() + XML2CSVMisc.FILE_SEPARATOR + csvReferenceName + "-" + fileCounter + "." + XML2CSVMisc.CSV);
    if (csvOutputFile.exists()) csvOutputFile.delete();
    FileOutputStream fos = new FileOutputStream(csvOutputFile, true);
    outputStreamWriter = new OutputStreamWriter(fos, encoding);
  }

  /**
   * Flushes the output.
   * @throws IOException in case of error, or if the <code>OutputWriterFacade</code> instance is not {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.
   */
  public void flush() throws IOException
  {
    if (ready == false) throw new IOException("Not ready.");
    outputStreamWriter.flush();
  }

  /**
   * Closes the output.
   * @throws IOException in case of error, or if the <code>OutputWriterFacade</code> instance is not {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.
   */
  public void close() throws IOException
  {
    if (ready == false) throw new IOException("Not ready.");
    if (outputStreamWriter != null) outputStreamWriter.close();
  }

  /**
   * Resets the facade for further use.<br>
   * The <code>OutputWriterFacade</code> won't be {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} until the appropriate <code>initialize</code> method is called to
   * configure the output.
   * @throws IOException in case of error.
   */
  public void reset() throws IOException
  {
    ready = false;

    cutoff = -1L;
    outputStreamWriter = null;
    csvOutputDir = null;
    csvReferenceName = null;
    lineCounter = 0L;
    fileCounter = 0;
  }

  /**
   * Initializes this <code>OutputWriterFacade</code> for use with a direct <code>OutputStream</code>.<br>
   * The <code>OutputWriterFacade</code> will be {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.<br>
   * Deactivates the line count cutoff.
   * @param outputStream a direct output stream.
   * @param encoding the character encoding to use in outputs.
   * @throws IOException in case of error.
   */
  public void initialize(OutputStream outputStream, Charset encoding) throws IOException
  {
    reset();

    this.encoding = encoding;
    if (encoding == null) throw new IOException("No encoding.");

    if (outputStream == null) throw new IOException("No output stream.");
    else
    {
      // When a direct output stream is provided cutoff is deactivated and everything goes to that direct stream.
      cutoff = -1L;
      outputStreamWriter = new OutputStreamWriter(outputStream, encoding);
    }

    fileCounter++;
    ready = true;
  }

  /**
   * Initializes this <code>OutputWriterFacade</code> for use with a CSV output file.<br>
   * Plugs the CSV output file writer facade onto the CSV output file provided.<br>
   * The <code>OutputWriterFacade</code> will be {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.
   * @param cutoff any positive long integer to set a line count cutoff accordingly, or <code>-1</code> to deactivate cutoff.
   * @param csvOutputFile the CSV output file.
   * @param encoding the character encoding to use in outputs.
   * @throws IOException in case of error.
   */
  public void initialize(long cutoff, File csvOutputFile, Charset encoding) throws IOException
  {
    reset();

    this.cutoff = cutoff;
    this.csvOutputFile = csvOutputFile;
    this.encoding = encoding;

    if (csvOutputFile == null) throw new IOException("No output file.");
    if (encoding == null) throw new IOException("No encoding.");

    csvOutputDir = csvOutputFile.getParentFile();
    String filename = csvOutputFile.getName();
    int idx = filename.lastIndexOf(".");
    if (idx != -1) csvReferenceName = filename.substring(0, idx) + "." + XML2CSVMisc.CSV;
    else
      csvReferenceName = filename + "." + XML2CSVMisc.CSV;

    if (csvOutputFile.exists()) csvOutputFile.delete();
    FileOutputStream fos = new FileOutputStream(csvOutputFile, true);
    outputStreamWriter = new OutputStreamWriter(fos, encoding);

    fileCounter++;
    ready = true;
  }

  /**
   * Initializes this <code>OutputWriterFacade</code> for use with an output directory and a reference output filename.<br>
   * Lets the CSV output file writer facade manage a CSV output file named after the reference name provided, generated in the CSV output directory provided.<br>
   * The <code>OutputWriterFacade</code> will be {@link utils.xml.xml2csv.OutputWriterFacade#isReady() ready} to use.
   * @param cutoff any positive long integer to set a line count cutoff accordingly, or <code>-1</code> to deactivate cutoff.
   * @param csvOutputDir the CSV output directory.
   * @param csvReferenceName the CSV reference filename, that is, the expected CSV filename without extension.
   * @param encoding the character encoding to use in outputs.
   * @throws IOException in case of error.
   */
  public void initialize(long cutoff, File csvOutputDir, String csvReferenceName, Charset encoding) throws IOException
  {
    this.cutoff = cutoff;
    this.csvOutputDir = csvOutputDir;
    this.csvReferenceName = csvReferenceName;
    this.encoding = encoding;

    if (csvOutputDir == null) throw new IOException("No output directory.");
    if (csvReferenceName == null) throw new IOException("No reference CSV file name.");
    if (encoding == null) throw new IOException("No encoding.");

    csvOutputFile = new File(csvOutputDir.getAbsolutePath() + XML2CSVMisc.FILE_SEPARATOR + csvReferenceName + "." + XML2CSVMisc.CSV);
    if (csvOutputFile.exists()) csvOutputFile.delete();
    FileOutputStream fos = new FileOutputStream(csvOutputFile, true);
    outputStreamWriter = new OutputStreamWriter(fos, encoding);

    fileCounter++;
    ready = true;
  }

  /**
   * Returns the current CSV output filename, of <code>null</code> when not undefined.
   * @return the current CSV output filename, of <code>null</code>.
   */
  public String getOuputFileName()
  {
    String result = null;
    if (csvOutputFile != null) result = csvOutputFile.getName();
    else
      result = "current output";
    return result;
  }

  /**
   * Returns the current CSV output absolute path, of <code>null</code> when not undefined.
   * @return the current CSV output absolute path, of <code>null</code>.
   */
  public String getOutputAbsolutePath()
  {
    String result = null;
    if (csvOutputFile != null) result = csvOutputFile.getAbsolutePath();
    return result;
  }

  /**
   * Returns a string URI pointing to the current CSV output directory, of <code>null</code> when not undefined.
   * @return a string URI pointing to the current CSV output directory, of <code>null</code>.
   */
  public String getOutputDirURI()
  {
    String result = null;
    if (csvOutputDir != null) result = csvOutputDir.toURI().toString();
    return result;
  }

  /**
   * Returns <code>true</code> if this <code>OutputWriterFacade</code> is ready and <code>false</code> otherwise.
   * @return <code>true</code> if this <code>OutputWriterFacade</code> is ready and <code>false</code> otherwise.
   */
  public boolean isReady()
  {
    return ready;
  }
}
