/*
 * PDA-Plugin
 * Copyright (C) 2004 gilson laurent pumpkin@gmx.de
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
 */

package microtvbrowserplugin;

import devplugin.Program;
import devplugin.Date;
import devplugin.Channel;
import java.util.Vector;
import java.util.Date;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import javax.swing.JOptionPane;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;





public class NanoMIDletCreator implements util.ui.progress.Progress{
  
  private util.ui.progress.ProgressWindow progress;
  private MicroTvBrowserPlugin mtbp;
  private File dir;
  private StringBuffer USER_LOG = new StringBuffer("");
  private java.util.Hashtable title_id;
  
  public NanoMIDletCreator(MicroTvBrowserPlugin MTBP, File Dir, util.ui.progress.ProgressWindow Progress){
    mtbp = MTBP;
    progress = Progress;
    dir = Dir;
  }
  
  private int pipe(InputStream In, java.io.OutputStream Out) throws Exception{
    int counter = 0;
    int copy = In.read();
    while (copy !=-1){
      counter++;
      Out.write(copy);
      copy = In.read();
    }
    return counter;
  }
  
  
  private void copyFile(JarOutputStream jar, String s) throws Exception {
    try {
      java.util.jar.JarFile JA = mtbp.getJarFilePublic();
      JarEntry JAE = JA.getJarEntry("microtvbrowserplugin/data_nano/"+s);
      InputStream In = JA.getInputStream(JAE);
      
      ByteArrayOutputStream BOUT = new ByteArrayOutputStream();
      pipe(In,BOUT);
      
      JarEntry ZIP = new JarEntry(s);
      jar.putNextEntry(ZIP);
      DataOutputStream DOUT = new DataOutputStream(jar);
      DOUT.write(BOUT.toByteArray());
      DOUT.flush();
    } catch (Exception E){
    }
  }
  
  int GLOBALSIZE = 0;
  
  private void writeString(DataOutputStream dout, String s) throws Exception{
    byte[] temp = s.getBytes("ISO8859_1");
    dout.writeByte((byte)temp.length);
    dout.write(temp);
  }
  
  public void run(){
    try {
      progress.setMessage("collecting channels");
      devplugin.PluginManager PM = mtbp.getPluginManager();
      Channel[] CH = PM.getSubscribedChannels();
      Vector Export = new Vector();
      //LOG.append ("\nMatching:\n");
      String[] channel_list = mtbp.getChannelList();
      for (int i =0;i<channel_list.length;i++){
        //LOG.append ("searching "+chanel_list[i]+"\n");
        for (int j=0;j<CH.length;j++){
          if (CH[j].getName().equals(channel_list[i])){
            if (!Export.contains(CH[j])){
              Export.add(CH[j]);
            }
            break;
          }
        }
      }
      
      Channel[] CH_to_export = (Channel[]) Export.toArray(new Channel[Export.size()]);
      
      progress.setMessage("creating jar and config");
      //zuerst die MANIFEST-Datei:
      java.util.jar.JarFile JA = mtbp.getJarFilePublic();
      
      JarEntry JAE = JA.getJarEntry("microtvbrowserplugin/data_nano/MIDletMANI.MF");
      InputStream In = JA.getInputStream(JAE);
      System.out.println("MIDledMANI.MF "+(JAE!=null));
      
      java.io.BufferedReader BUR = new java.io.BufferedReader(new java.io.InputStreamReader(In));
      try {
        String st = BUR.readLine();
        while (st != null){
          System.out.println(st);
          st = BUR.readLine();
        }
      } catch (Exception E){
      }
      System.out.println("MIDletMANI END");
      
      JAE = JA.getJarEntry("microtvbrowserplugin/data_nano/MIDletMANI.MF");
      In = JA.getInputStream(JAE);
      
      java.util.jar.Manifest MA = new java.util.jar.Manifest(In);
      System.out.println("MANIFEST");
      MA.write(System.out);
      System.out.println("MANIFEST END");
      
      File MircoTvBrowser = new File(dir, "NanoTvBrowser.jar");
      JarOutputStream jar = new JarOutputStream(new java.io.FileOutputStream(MircoTvBrowser),MA);
      
      //die .class-Dateien:
      copyFile(jar,"tv.class");
      copyFile(jar,"exec.class");
      
      
      
      //LOG.append ("\nBasic.C:\n");
      jar.putNextEntry(new JarEntry("conf"));
      DataOutputStream basic = new DataOutputStream(jar);
      
      basic.writeUTF("Error");
      basic.writeUTF("Data too old");
      basic.writeUTF("show");
      basic.writeUTF("no data found");
      basic.writeUTF("exit");
      basic.writeUTF("back");
      basic.writeUTF("channels");
      basic.writeUTF("dates");
      basic.writeUTF("please wait");
      basic.writeUTF("working...");
      
      Date now = new Date();
      Date now2 = new Date(now.getYear(),now.getMonth(),now.getDate(),0,1);
      basic.writeLong(now2.getTime());
      int data_length = mtbp.getDaysToExport();
      basic.writeByte(data_length);
      basic.writeByte(CH_to_export.length);
      for (int i =0;i<CH_to_export.length;i++){
        basic.writeUTF(CH_to_export[i].getName());
      }
      basic.flush();
      
      Date today = new Date();
      progress.setMessage("converting basic data");
      GLOBALSIZE = 0;
      
      for (int j =0;j<CH_to_export.length;j++){
        for (int i=0;i<data_length;i++){
          Date D = new Date(today);
          D = D.addDays(i);
          java.util.Iterator it = PM.getChannelDayProgram(D,CH_to_export[j]);
          
          Vector[] dayprog_parts = new Vector[6];
          for (int k =0;k<dayprog_parts.length;k++){
            dayprog_parts[k] = new Vector();
          }
          
          int part = 0;
          
          if (it!=null){
            Vector to_use = dayprog_parts[0];
            int limithour = 4;
            while (it.hasNext()){
              Program toSave = (Program) it.next();
              while (toSave.getHours()>=limithour){
                limithour +=4;
                part++;
                to_use = dayprog_parts[part];
              }
              ByteArrayOutputStream temp_out = new ByteArrayOutputStream();
              DataOutputStream prog_out = new DataOutputStream(temp_out);
              String title = toSave.getTitle();
              if (title == null){
                USER_LOG.append(CH_to_export[j]+": Entry without title ?\n");
                title = "unknow";
              }
              
              prog_out.write(toSave.getHours());
              prog_out.write(toSave.getMinutes());
              writeString(prog_out,title);
              
              prog_out.flush();
              to_use.add(temp_out.toByteArray());
            }
          } else {
            USER_LOG.append("Can't export "+CH_to_export[j]+" on "+D.getDayOfMonth()+":"+(D.getMonth()+1)+" : No valid data found\n");
          }
          
          jar.putNextEntry(new JarEntry(j+"."+i));
          DataOutputStream dayprog = new DataOutputStream(jar);
          
          int[] size_of_prog = new int[6];
          int[] number_of_prog = new int[6];
          for (int k=0;k<size_of_prog.length;k++){
            number_of_prog[k] = dayprog_parts[k].size();
            for (int kk=0;kk<dayprog_parts[k].size();kk++){
              size_of_prog[k] += ((byte[])dayprog_parts[k].get(kk)).length;
            }
            dayprog.write(number_of_prog[k]);
            dayprog.writeShort(size_of_prog[k]);
          }
          for (int k=0;k<size_of_prog.length;k++){
            for (int kk=0;kk<dayprog_parts[k].size();kk++){
              dayprog.write((byte[])dayprog_parts[k].get(kk));
            }
          }
          dayprog.flush();
        }
      }
      
      jar.flush();
      jar.close();
      
      File JAD = new File(dir,"NanoTvBrowser.jad");
      
      java.io.BufferedWriter BUF = new java.io.BufferedWriter(new java.io.FileWriter(JAD));
      BUF.write(
      "MIDlet-1: NanoTvBrowser, MicroTvBrowser.png, tv\n"
      +"MIDlet-Jar-Size: "+MircoTvBrowser.length()+"\n"
      +"MIDlet-Jar-URL: NanoTvBrowser.jar\n"
      +"MIDlet-Name: NanoTvBrowser\n"
      +"MIDlet-Vendor: Unknown\n"
      +"MIDlet-Version: 1.0\n"
      +"MicroEdition-Configuration: CLDC-1.0\n"
      +"MicroEdition-Profile: MIDP-1.0\n"
      );
      BUF.flush();
      BUF.close();
      progress.setMessage("done");
      try {
        String user_log = USER_LOG.toString();
        if (user_log.length()!=0){
          JOptionPane.showMessageDialog(mtbp.getParentFramePublic(),"Nano MIDlet created, but with problems:\n"+user_log,"done", JOptionPane.WARNING_MESSAGE);
        } else {
          JOptionPane.showMessageDialog(mtbp.getParentFramePublic(),"Nano MIDlet created.\n Please install now \n"+JAD.toString()+"\n on your mobile device.","done", JOptionPane.INFORMATION_MESSAGE);
        }
      } catch (Exception E1){
        System.out.println("caught getParentFrame-Bug");
        E1.printStackTrace(System.out);
      }
      
    }catch (Exception E){
      java.io.CharArrayWriter BOUT = new java.io.CharArrayWriter();
      E.printStackTrace(new java.io.PrintWriter(BOUT));
      //LOG.append (BOUT.toString());
      E.printStackTrace();
      JOptionPane.showMessageDialog(mtbp.getParentFramePublic(),"MIDlet NOT created because: \n"+BOUT.toString(),"ERROR", JOptionPane.ERROR_MESSAGE);
    }
  }
  
}
