/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (darras@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * CVS information:
 *  $RCSfile$
 *   $Source$
 *     $Date$
 *   $Author$
 * $Revision$
 */
package primarydatamanager.mirrorupdater.data;

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import primarydatamanager.mirrorupdater.UpdateException;
import util.io.IOUtilities;

/**
 * 
 * 
 * @author Til Schneider, www.murfman.de
 */
public class FileDataTarget implements DataTarget {
  
  private File mDir;
  
  

  /**
   * @param dir
   */
  public FileDataTarget(File dir) {
    mDir = dir;
  }
  
  
  
  public String[] listFiles() throws UpdateException {
    if (! mDir.exists()) {
      throw new UpdateException("Directory does not exist: "
        + mDir.getAbsolutePath());
    }
    
    return mDir.list();
  }



  public void deleteFile(String fileName) throws UpdateException {
    File file = new File(mDir, fileName);
    if (! file.delete()) {
      throw new UpdateException("Deleting file failed: " + fileName);
    }
  }


  public byte[] loadFile(String fileName) throws UpdateException {
    File file = new File(mDir, fileName);
    
    FileInputStream in = null;
    try {
      in = new FileInputStream(file);
      ByteArrayOutputStream out = new ByteArrayOutputStream((int) file.length());
      IOUtilities.pipeStreams(in, out);
      out.close();
      return out.toByteArray();
    }
    catch (IOException exc) {
      throw new UpdateException("Loading file failed: " + fileName, exc);
    }
    finally {
      if (in != null) {
        try { in.close(); } catch (IOException exc) {}
      }
    }
  }



  public void writeFile(String fileName, byte[] data) throws UpdateException {
    File file = new File(mDir, fileName);
    
    FileOutputStream stream = null;
    try {
      stream = new FileOutputStream(file);
      stream.write(data);
    }
    catch (IOException exc) {
      // Try to delete the corrupt file
      if (stream != null) {
        try { stream.close(); } catch (IOException exc2) {}
      }
      file.delete();
      
      throw new UpdateException("Writing file failed: " + fileName, exc);
    }
    finally {
      if (stream != null) {
        try { stream.close(); } catch (IOException exc) {}
      }
    }
  }



  public void close() throws UpdateException {
  }

}
