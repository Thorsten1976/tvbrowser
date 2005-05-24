/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (martin@tvbrowser.org)
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

package printplugin;

import devplugin.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;
import java.awt.*;
import java.util.Properties;
import java.io.*;
import javax.swing.*;
import printplugin.dlgs.MainPrintDialog;
import printplugin.dlgs.SettingsDialog;
import printplugin.dlgs.DialogContent;
import printplugin.dlgs.printfromqueuedialog.PrintFromQueueDialogContent;
import printplugin.dlgs.printdayprogramsdialog.PrintDayProgramsDialogContent;
import printplugin.settings.*;
import printplugin.printer.PrintJob;
import util.ui.UiUtilities;


public class PrintPlugin extends Plugin {

  /** The localizer for this class. */
  private static final util.ui.Localizer mLocalizer
      = util.ui.Localizer.getLocalizerFor(PrintPlugin.class);


  private static final String SCHEME_FILE_DAYPROGRAM = "printplugin.dayprog.schemes";
  private static final String SCHEME_FILE_QUEUE = "printplugin.queue.schemes";

  private static Plugin mInstance;

  public PrintPlugin() {
    mInstance = this;
  }

  public static Plugin getInstance() {
    return mInstance;
  }

  public String getMarkIconName() {
    return "printplugin/imgs/Print16.gif";
  }

  public PluginInfo getInfo() {
    String name = mLocalizer.msg("printProgram" ,"Print program");
    String desc = mLocalizer.msg("printdescription" ,"Allows printing programs.");
    String author = "Martin Oberhauser (martin@tvbrowser.org)";

    return new PluginInfo(name, desc, author, new Version(0, 9));
  }

  public void onActivation() {
    PluginTreeNode root = getRootNode();
    Program[] progs = root.getPrograms();
    for (int i=0; i<progs.length; i++) {
      progs[i].mark(this);
    }
    root.update();
  }

  public ActionMenu getContextMenuActions(final Program program) {
    final Plugin thisPlugin = this;
    ContextMenuAction action = new ContextMenuAction();
    action.setSmallIcon(createImageIcon("printplugin/imgs/Print16.gif"));
    if (getRootNode().contains(program)) {
      action.setText(mLocalizer.msg("removeFromPrinterQueue","Aus der Druckerwarteschlange loeschen"));
      action.setActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent e) {
          getRootNode().removeProgram(program);
          getRootNode().update();
          program.unmark(thisPlugin);
        }
      });
    }
    else {
      action.setText(mLocalizer.msg("addToPrinterQueue","Zur Druckerwarteschlange hinzufuegen"));
      action.setActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent event) {
          getRootNode().addProgram(program);
          getRootNode().update();
          program.mark(thisPlugin);
        }
      });
    }
    return new ActionMenu(action);
  }


  public ActionMenu getButtonAction() {
    AbstractAction action = new AbstractAction() {
      public void actionPerformed(ActionEvent evt) {
        MainPrintDialog mainDialog = new MainPrintDialog(getParentFrame());
        UiUtilities.centerAndShow(mainDialog);
        int result = mainDialog.getResult();

        if (result == MainPrintDialog.PRINT_DAYPROGRAMS) {
          SettingsDialog dlg = showPrintDialog(new PrintDayProgramsDialogContent(getParentFrame()), loadDayProgramSchemes());
          storeDayProgramSchemes(dlg.getSchemes());
        }
        else if (result == MainPrintDialog.PRINT_QUEUE) {
          SettingsDialog dlg = showPrintDialog(new PrintFromQueueDialogContent(getRootNode(), getParentFrame()), loadQueueSchemes());
          storeQueueSchemes(dlg.getSchemes());
        }
      }
    };

    action.putValue(Action.NAME, mLocalizer.msg("print","Print"));
    action.putValue(Action.SMALL_ICON, createImageIcon("printplugin/imgs/Print16.gif"));
    action.putValue(BIG_ICON, createImageIcon("printplugin/imgs/Print24.gif"));
    action.putValue(Action.SHORT_DESCRIPTION, getInfo().getDescription());

    return new ActionMenu(action);
  }


  private SettingsDialog showPrintDialog(DialogContent content, Scheme[] schemes) {
    PrinterJob printerJob = PrinterJob.getPrinterJob();
    SettingsDialog settingsDialog = new SettingsDialog(getParentFrame(), printerJob, schemes, content);
    UiUtilities.centerAndShow(settingsDialog);

    if (settingsDialog.getResult() == SettingsDialog.OK) {
      PrintJob job = settingsDialog.getPrintJob();
      try {
        printerJob.setPrintable(job.getPrintable());
        printerJob.print();
        settingsDialog.printingDone();
      } catch (PrinterException e) {
        util.exc.ErrorHandler.handle("Could not print pages: "+e.getLocalizedMessage(), e);
      }
    }
    return settingsDialog;
  }


  private void storeDayProgramSchemes(Scheme[] schemes) {
    String home = Plugin.getPluginManager().getTvBrowserSettings().getTvBrowserUserHome();
    File schemeFile = new File(home,SCHEME_FILE_DAYPROGRAM);
    ObjectOutputStream out=null;
    try {
      out = new ObjectOutputStream(new FileOutputStream(schemeFile));
      out.writeInt(1);  // version
      out.writeInt(schemes.length);
      for (int i=0; i<schemes.length; i++) {
        out.writeObject(schemes[i].getName());
        ((DayProgramScheme)schemes[i]).store(out);
      }
      out.close();
    }catch(IOException e) {
      util.exc.ErrorHandler.handle("Could not store settings.",e);
    }
  }

  private void storeQueueSchemes(Scheme[] schemes) {
    String home = Plugin.getPluginManager().getTvBrowserSettings().getTvBrowserUserHome();
    File schemeFile = new File(home,SCHEME_FILE_QUEUE);
    ObjectOutputStream out=null;
    try {
      out = new ObjectOutputStream(new FileOutputStream(schemeFile));
      out.writeInt(1);  // version
      out.writeInt(schemes.length);
      for (int i=0; i<schemes.length; i++) {
        out.writeObject(schemes[i].getName());
        ((QueueScheme)schemes[i]).store(out);
      }
      out.close();
    }catch(IOException e) {
      util.exc.ErrorHandler.handle("Could not store settings.",e);
    }
  }

  public void receivePrograms(Program[] programArr) {
    PluginTreeNode rootNode = getRootNode();
    for (int i=0; i<programArr.length; i++) {
      if (!rootNode.contains(programArr[i])) {
        rootNode.addProgram(programArr[i]);
        programArr[i].mark(this);
      }
    }
    rootNode.update();
  }


  public boolean canReceivePrograms() {
    return true;
  }

  public boolean canUseProgramTree() {
    return true;
  }


  private DayProgramScheme[] readDayProgramSchemesFromStream(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.readInt();  // read version
    int cnt = in.readInt();
    DayProgramScheme[] schemes = new DayProgramScheme[cnt];
    for (int i=0; i<cnt; i++) {
      String name = (String)in.readObject();
      schemes[i] = new DayProgramScheme(name);
      schemes[i].read(in);
    }
    return schemes;
  }

  private DayProgramScheme[] loadDayProgramSchemes() {
    String home = Plugin.getPluginManager().getTvBrowserSettings().getTvBrowserUserHome();
    File schemeFile = new File(home,SCHEME_FILE_DAYPROGRAM);
    ObjectInputStream in=null;
    try {
      in = new ObjectInputStream(new FileInputStream(schemeFile));
      DayProgramScheme[] schemes = readDayProgramSchemesFromStream(in);
      in.close();
      return schemes;
    }catch(Exception e) {
      if (in != null) {
        try { in.close(); } catch(IOException exc) {}
      }
      DayProgramScheme scheme = new DayProgramScheme(mLocalizer.msg("defaultScheme","DefaultScheme"));
      scheme.setSettings(new DayProgramPrinterSettings(
              new Date(),
              3,
              null,
              6,
              24+3,
              5,
              2,
              PrinterProgramIconSettings.create(
                  new ProgramFieldType[]{
                    ProgramFieldType.EPISODE_TYPE,
                    ProgramFieldType.ORIGIN_TYPE,
                    ProgramFieldType.PRODUCTION_YEAR_TYPE,
                    ProgramFieldType.SHORT_DESCRIPTION_TYPE
                  }, false)));
      return new DayProgramScheme[]{scheme};
    }
  }


  private QueueScheme[] readQueueSchemesFromStream(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.readInt();  // read version
    int cnt = in.readInt();
    QueueScheme[] schemes = new QueueScheme[cnt];
    for (int i=0; i<cnt; i++) {
      String name = (String)in.readObject();
      schemes[i] = new QueueScheme(name);
      schemes[i].read(in);
    }
    return schemes;
  }


  private QueueScheme[] loadQueueSchemes() {
    String home = Plugin.getPluginManager().getTvBrowserSettings().getTvBrowserUserHome();
    File schemeFile = new File(home,SCHEME_FILE_QUEUE);
    ObjectInputStream in=null;
    try {
      in = new ObjectInputStream(new FileInputStream(schemeFile));
      QueueScheme[] schemes = readQueueSchemesFromStream(in);
      in.close();
      return schemes;
    }catch(Exception e) {
      if (in != null) {
        try { in.close(); } catch(IOException exc) {}
      }
      QueueScheme scheme = new QueueScheme(mLocalizer.msg("defaultScheme","DefaultScheme"));
      scheme.setSettings(new QueuePrinterSettings(
              true,
              1,
              PrinterProgramIconSettings.create(
                  new ProgramFieldType[]{
                    ProgramFieldType.EPISODE_TYPE,
                    ProgramFieldType.ORIGIN_TYPE,
                    ProgramFieldType.PRODUCTION_YEAR_TYPE,
                    ProgramFieldType.SHORT_DESCRIPTION_TYPE
                  }, false),
              new Font("Dialog", Font.ITALIC, 12)
      ));

      return new QueueScheme[]{scheme};
    }
  }

  public void loadSettings(Properties settings) {

  }

  public void writeData(ObjectOutputStream out) throws IOException {
    storeRootNode();
  }


}
