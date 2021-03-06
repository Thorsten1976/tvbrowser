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

package tvbrowser.extras.reminderplugin;

import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import javax.sound.midi.Sequencer;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.apache.commons.lang3.StringUtils;

import tvbrowser.core.icontheme.IconLoader;
import tvbrowser.ui.mainframe.MainFrame;
import util.misc.PropertyDefaults;
import util.ui.DefaultMarkingPrioritySelectionPanel;
import util.ui.ExecuteSettingsDialog;
import util.ui.ExtensionFileFilter;
import util.ui.FileCheckBox;
import util.ui.PluginChooserDlg;
import util.ui.ScrollableJPanel;
import util.ui.UiUtilities;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import devplugin.ProgramReceiveIf;
import devplugin.ProgramReceiveTarget;
import devplugin.SettingsTab;


/**
 *
 * @author Martin Oberhauser
 */
public class ReminderSettingsTab implements SettingsTab {

  /** The localizer for this class. */
  private static final util.ui.Localizer mLocalizer
      = util.ui.Localizer.getLocalizerFor(ReminderSettingsTab.class);

  private Properties mSettings;

  private JCheckBox mReminderWindowChB;
  private JRadioButton[] mReminderWindowPosition;  
  private FileCheckBox mSoundFileChB;
  private JCheckBox mBeep;
  private JCheckBox mExecChB;
  private JCheckBox mShowTimeSelectionDlg;
  private JCheckBox mShowRemovedDlg;
  private JCheckBox mShowTimeCounter;
  private JCheckBox mProvideTab;
  private JCheckBox mShowDateSeparators;
  
  private JButton mExecFileDialogBtn;
  private JCheckBox mShowAlwaysOnTop;
  private JSpinner mAutoCloseReminderTimeSp;
  private JRadioButton mCloseOnEnd, mCloseNever, mCloseOnTime;
  private JRadioButton mScrollTimeToNext, mScrollTimeOnDay;

  private JComboBox mDefaultReminderEntryList;

  private String mExecFileStr, mExecParamStr;
  private Object mTestSound;

  private JLabel mPluginLabel;
  private ProgramReceiveTarget[] mClientPluginTargets;
  
  private DefaultMarkingPrioritySelectionPanel mMarkingsPanel;

  /**
   * Constructor.
   */
  public ReminderSettingsTab() {
    mSettings = ReminderPlugin.getInstance().getSettings();
  }

  /**
   * Creates the settings panel for this tab.
   */
  public JPanel createSettingsPanel() {
    PropertyDefaults propDefaults = ReminderPropertyDefaults.getPropertyDefaults();
    propDefaults.setProperties(mSettings);
    
    FormLayout layout = new FormLayout("5dlu,pref,5dlu,pref,pref:grow,3dlu,pref,3dlu,pref,5dlu",
        "pref,5dlu,pref,1dlu,pref,1dlu,pref,1dlu,pref,10dlu," +
        "pref,5dlu,pref,10dlu,pref,5dlu,pref,10dlu,pref,5dlu," +
        "pref,10dlu,pref,5dlu,pref,3dlu,pref,3dlu,default,3dlu," +
        "default,default,10dlu,default,5dlu,pref");
    layout.setColumnGroups(new int[][] {{7,9}});
    PanelBuilder pb = new PanelBuilder(layout, new ScrollableJPanel());
    pb.border(Borders.DIALOG);

    final String[] extArr = { ".wav", ".aif", ".rmf", ".au", ".mid" };
    String soundFName = propDefaults.getValueFromProperties(ReminderPropertyDefaults.SOUNDFILE_KEY);
    String msg = mLocalizer.msg("soundFileFilter", "Sound file ({0})",
        "*" + StringUtils.join(extArr, ", *"));


    mReminderWindowChB = new JCheckBox(mLocalizer.msg("reminderWindow", "Reminder window"), propDefaults.getValueFromProperties(ReminderPropertyDefaults.REMINDER_WINDOW_SHOW).equalsIgnoreCase("true"));

    mShowAlwaysOnTop = new JCheckBox(mLocalizer.msg("alwaysOnTop","Show always on top"), propDefaults.getValueFromProperties(ReminderPropertyDefaults.REMINDER_WINDOW_ALWAYS_ON_TOP).equalsIgnoreCase("true"));
    mShowAlwaysOnTop.setEnabled(mReminderWindowChB.isSelected());

    JPanel reminderWindowCfg = new JPanel(new FormLayout("12dlu,default:grow","default,1dlu,default,1dlu,default,1dlu,default"));
    reminderWindowCfg.add(mReminderWindowChB, CC.xyw(1,1,2));
    reminderWindowCfg.add(mShowAlwaysOnTop, CC.xy(2,7));
        
    JPanel postionPanel = new JPanel(new FormLayout("default,default,default,default,default,default","default,default,default,default,default,default"));
    
    int xPos = 1;
    int yPos = 1;
    
    int selected = Integer.parseInt(propDefaults.getValueFromProperties(ReminderPropertyDefaults.REMINDER_WINDOW_POSITION));
    
    mReminderWindowPosition = new JRadioButton[13];
    
    ButtonGroup positionGroup = new ButtonGroup();
    
    for(int i = 0; i < 13; i++) {
      mReminderWindowPosition[i] = new JRadioButton();
      mReminderWindowPosition[i].setSelected(i == selected);
      mReminderWindowPosition[i].setEnabled(mReminderWindowChB.isSelected());
      positionGroup.add(mReminderWindowPosition[i]);
      
      if(i == 3 || i == 8) {
        xPos = 2;
        yPos++;
      }
      else if(i == 5 || i == 10) {
        xPos = 1;
        yPos++;
      }
      
      postionPanel.add(mReminderWindowPosition[i], CC.xy(xPos, yPos));
      xPos += 2;
    }

    final JLabel posLabel = new JLabel(mLocalizer.msg("positionOnScreen", "Position on screen"));
    posLabel.setEnabled(mReminderWindowChB.isSelected());
    
    reminderWindowCfg.add(posLabel, CC.xy(2,3));
    reminderWindowCfg.add(postionPanel, CC.xy(2,5));
    
    mSoundFileChB = new FileCheckBox(mLocalizer.msg("playlingSound", "Play sound"), new File(soundFName), 0, false);

    JFileChooser soundChooser=new JFileChooser();
    soundChooser.setFileFilter(new ExtensionFileFilter(extArr, msg));

    mSoundFileChB.setFileChooser(soundChooser);

    mSoundFileChB.setSelected(mSettings.getProperty("usesound","false").equals("true"));

    mBeep = new JCheckBox(mLocalizer.msg("beep", "Speaker sound"), mSettings.getProperty("usebeep","true").equalsIgnoreCase("true"));

    mExecFileStr = mSettings.getProperty("execfile", "");
    mExecParamStr = mSettings.getProperty("execparam", "");

    final JButton soundTestBt = new JButton(mLocalizer.msg("test", "Test"));

    mExecChB = new JCheckBox(mLocalizer.msg("executeProgram", "Execute program"));
    mExecChB.setSelected(mSettings.getProperty("useexec","false").equals("true"));

    mExecFileDialogBtn = new JButton(mLocalizer.msg("executeConfig", "Configure"));
    mExecFileDialogBtn.setEnabled(mExecChB.isSelected());

    mPluginLabel = new JLabel();
    JButton choose = new JButton(mLocalizer.msg("selectPlugins","Choose Plugins"));

    mClientPluginTargets = ReminderPlugin.getInstance().getClientPluginsTargets();

    handlePluginSelection();

    choose.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {try{
        Window parent = UiUtilities.getLastModalChildOf(MainFrame
              .getInstance());
        PluginChooserDlg chooser = null;
        chooser = new PluginChooserDlg(parent, mClientPluginTargets, null,
              ReminderPluginProxy.getInstance());
        
        chooser.setVisible(true);

        if(chooser.getReceiveTargets() != null) {
          mClientPluginTargets = chooser.getReceiveTargets();
        }

        handlePluginSelection();}catch(Exception ee) {ee.printStackTrace();}
      }
    });

    int autoCloseReminderTime = 10;
    try {
      String asString = mSettings.getProperty("autoCloseReminderTime", "10");
      autoCloseReminderTime = Integer.parseInt(asString);

      if(autoCloseReminderTime == 0) {
        autoCloseReminderTime = 10;
      }
    } catch (Exception exc) {
      // ignore
    }

    mCloseOnEnd = new JRadioButton(mLocalizer.msg("autoCloseReminderAtProgramEnd","Program end"), mSettings.getProperty("autoCloseBehaviour","onEnd").equals("onEnd"));
    mCloseOnEnd.setEnabled(mReminderWindowChB.isSelected());

    mCloseNever = new JRadioButton(mLocalizer.msg("autoCloseNever","Never close"), mSettings.getProperty("autoCloseBehaviour","onEnd").equals("never"));
    mCloseNever.setEnabled(mReminderWindowChB.isSelected());

    mCloseOnTime = new JRadioButton(mLocalizer.ellipsisMsg("autoCloseAfterTime","After time"), mSettings.getProperty("autoCloseBehaviour","onEnd").equals("onTime"));
    mCloseOnTime.setEnabled(mReminderWindowChB.isSelected());

    ButtonGroup bg = new ButtonGroup();

    bg.add(mCloseOnEnd);
    bg.add(mCloseNever);
    bg.add(mCloseOnTime);

    mAutoCloseReminderTimeSp = new JSpinner(new SpinnerNumberModel(autoCloseReminderTime,autoCloseReminderTime < 5 ? 1 : 5,600,1));
    mAutoCloseReminderTimeSp.setEnabled(mCloseOnTime.isSelected() && mReminderWindowChB.isSelected());

    mShowTimeCounter = new JCheckBox(mLocalizer.msg("showTimeCounter","Show time counter"),mSettings.getProperty("showTimeCounter","false").compareTo("true") == 0);
    mShowTimeCounter.setEnabled(!mCloseNever.isSelected() && mReminderWindowChB.isSelected());

    PanelBuilder autoClosePanel = new PanelBuilder(new FormLayout("12dlu,default,2dlu,default:grow","pref,2dlu,pref,2dlu,pref,2dlu,pref,10dlu,pref"));
    autoClosePanel.add(mCloseOnEnd, CC.xyw(1,1,4));
    autoClosePanel.add(mCloseNever, CC.xyw(1,3,4));
    autoClosePanel.add(mCloseOnTime, CC.xyw(1,5,4));
    autoClosePanel.add(mAutoCloseReminderTimeSp, CC.xy(2,7));

    final JLabel secondsLabel = autoClosePanel.addLabel(mLocalizer.msg("seconds", "seconds (0 = off)"), CC.xy(4,7));

    autoClosePanel.add(mShowTimeCounter, CC.xyw(1,9,4));

    secondsLabel.setEnabled(mCloseOnTime.isSelected() && mReminderWindowChB.isSelected());

    String defaultReminderEntryStr = (String)mSettings.get("defaultReminderEntry");
    mDefaultReminderEntryList =new JComboBox(ReminderFrame.REMIND_BEFORE_VALUE_ARR);
    if (defaultReminderEntryStr != null) {
      try {
        int inx = Integer.parseInt(defaultReminderEntryStr);
        
        if(inx < 0) {
          inx = 0;
        }
        
        if (inx < ReminderFrame.REMIND_BEFORE_VALUE_ARR.length) {
          mDefaultReminderEntryList.setSelectedIndex(inx);
        }
      }catch(NumberFormatException e) {
        // ignore
      }
    }

    mShowTimeSelectionDlg = new JCheckBox(mLocalizer.msg("showTimeSelectionDialog","Show time selection dialog"));
    mShowTimeSelectionDlg.setSelected(mSettings.getProperty("showTimeSelectionDialog","true").compareTo("true") == 0);
    mShowRemovedDlg = new JCheckBox(mLocalizer.msg("showRemovedDialog","Show removed reminders after data update"));
    mShowRemovedDlg.setSelected(mSettings.getProperty("showRemovedDialog","true").compareTo("true") == 0);
    mShowDateSeparators = new JCheckBox(mLocalizer.msg("showDateSeparators", "Show date separator in program list"));
    mShowDateSeparators.setSelected(ReminderPlugin.getInstance().showDateSeparators());
    mProvideTab = new JCheckBox(mLocalizer.msg("provideTab", "Provide tab in TV-Browser main window"));
    mProvideTab.setSelected(mSettings.getProperty("provideTab","true").equals("true"));

    pb.addSeparator(mLocalizer.msg("remindBy", "Remind me by"), CC.xyw(1,1,10));

    pb.add(reminderWindowCfg, CC.xyw(2,3,4));
    pb.add(mSoundFileChB, CC.xyw(2,5,4));
    pb.add(mSoundFileChB.getButton(), CC.xy(7,5));
    pb.add(soundTestBt, CC.xy(9,5));
    pb.add(mBeep, CC.xy(2,7));
    pb.add(mExecChB, CC.xyw(2,9,4));
    pb.add(mExecFileDialogBtn, CC.xyw(7,9,3));

    pb.addSeparator(mLocalizer.msg("sendToPlugin", "Send reminded program to"), CC.xyw(1,11,10));

    pb.add(mPluginLabel, CC.xyw(2,13,4));
    pb.add(choose, CC.xyw(7,13,3));

    final JLabel c = (JLabel) pb.addSeparator(mLocalizer.msg("autoCloseReminder", "Automatically close reminder"), CC.xyw(1,15,10)).getComponent(0);
    c.setEnabled(mReminderWindowChB.isSelected());

    pb.add(autoClosePanel.getPanel(), CC.xyw(2,17,5));

    JPanel reminderEntry = new JPanel(new FlowLayout(FlowLayout.LEADING,0,0));
    reminderEntry.add(mDefaultReminderEntryList);

    pb.addSeparator(mLocalizer.msg("defaltReminderEntry","Default reminder time"), CC.xyw(1,19,10));
    pb.add(reminderEntry, CC.xyw(2,21,4));

    pb.addSeparator(mLocalizer.msg("miscSettings","Misc settings"), CC.xyw(1,23,10));
    pb.add(mShowTimeSelectionDlg, CC.xyw(2,25,7));
    pb.add(mShowRemovedDlg, CC.xyw(2,27,7));
    pb.add(mShowDateSeparators, CC.xyw(2,29,7));
    pb.add(mProvideTab, CC.xyw(2,31,7));
    
    mScrollTimeToNext = new JRadioButton(mLocalizer.msg("timeButtonScrollNext", "Scroll to next occurence of time from shown programs onward"), Boolean.parseBoolean(propDefaults.getValueFromProperties(ReminderPropertyDefaults.SCROLL_TIME_TYPE_NEXT)));
    mScrollTimeOnDay = new JRadioButton(mLocalizer.msg("timeButtonScrollDay", "Scroll to occurence of time on shown day in list"), !mScrollTimeToNext.isSelected());
    final JLabel scrollTimeLabel = new JLabel(mLocalizer.msg("timeButtonBehaviour", "Time buttons behaviour:"));
    
    mScrollTimeToNext.setEnabled(mProvideTab.isSelected());
    mScrollTimeOnDay.setEnabled(mProvideTab.isSelected());
    scrollTimeLabel.setEnabled(mProvideTab.isSelected());
    
    ButtonGroup time = new ButtonGroup();
    
    time.add(mScrollTimeToNext);
    time.add(mScrollTimeOnDay);

    JPanel timeButtonBehaviour = new JPanel(new FormLayout("10dlu,default:grow","5dlu,default,5dlu,default,1dlu,default"));

    timeButtonBehaviour.add(scrollTimeLabel, CC.xy(2, 2));
    timeButtonBehaviour.add(mScrollTimeToNext, CC.xy(2,4));
    timeButtonBehaviour.add(mScrollTimeOnDay, CC.xy(2,6));
    
    pb.add(timeButtonBehaviour, CC.xyw(2, 32, 7));
    
    mProvideTab.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        scrollTimeLabel.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        mScrollTimeToNext.setEnabled(scrollTimeLabel.isEnabled());
        mScrollTimeOnDay.setEnabled(scrollTimeLabel.isEnabled());
      }
    });

    pb.addSeparator(DefaultMarkingPrioritySelectionPanel.getTitle(), CC.xyw(1,34,10));
    pb.add(mMarkingsPanel = DefaultMarkingPrioritySelectionPanel.createPanel(ReminderPlugin.getInstance().getMarkPriority(),false,false),CC.xyw(2,36,9));

    mReminderWindowChB.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        mShowAlwaysOnTop.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        
        for(int i = 0; i < mReminderWindowPosition.length; i++) {
          mReminderWindowPosition[i].setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        }
        
        posLabel.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        
        c.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        secondsLabel.setEnabled(e.getStateChange() == ItemEvent.SELECTED && mCloseOnTime.isSelected());
        mCloseOnEnd.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        mCloseNever.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        mCloseOnTime.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        mShowTimeCounter.setEnabled(e.getStateChange() == ItemEvent.SELECTED && !mCloseNever.isSelected());
        mAutoCloseReminderTimeSp.setEnabled(e.getStateChange() == ItemEvent.SELECTED && mCloseOnTime.isSelected());
      }
    });

    soundTestBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        if(evt.getActionCommand().compareTo(mLocalizer.msg("test", "Test")) == 0) {
          mTestSound = ReminderPlugin.playSound(mSoundFileChB.getTextField().getText());
          if(mTestSound != null) {
            soundTestBt.setText(mLocalizer.msg("stop", "Stop"));
          }
          if(mTestSound != null) {
            if(mTestSound instanceof SourceDataLine) {
              ((SourceDataLine)mTestSound).addLineListener(new LineListener() {
                public void update(LineEvent event) {
                  if(event.getType() == Type.CLOSE || event.getType() == Type.STOP) {
                    soundTestBt.setText(mLocalizer.msg("test", "Test"));
                  }
                }
              });
            }
            else if(mTestSound instanceof Sequencer) {
              new Thread("Test MIDI sound") {
                public void run() {
                  setPriority(Thread.MIN_PRIORITY);
                  while(((Sequencer)mTestSound).isRunning()) {
                    try {
                      Thread.sleep(100);
                    }catch(Exception ee) {}
                  }

                  soundTestBt.setText(mLocalizer.msg("test", "Test"));
                }
              }.start();
            }
          }
        }
        else if(mTestSound != null) {
          if(mTestSound instanceof SourceDataLine && ((SourceDataLine)mTestSound).isRunning()) {
            ((SourceDataLine)mTestSound).stop();
          } else if(mTestSound instanceof Sequencer && ((Sequencer)mTestSound).isRunning()) {
            ((Sequencer)mTestSound).stop();
          }
        }
      }
    });

    mSoundFileChB.getCheckBox().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        soundTestBt.setEnabled(mSoundFileChB.isSelected());
      }
    });

    mSoundFileChB.getTextField().addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        String text = mSoundFileChB.getTextField().getText();
        if((new File(text)).isFile()) {
          boolean notFound = true;
          for (String extension : extArr) {
            if(StringUtils.endsWithIgnoreCase(text, extension)) {
              notFound = false;
              break;
            }
          }

          if(notFound) {
            soundTestBt.setEnabled(false);
          } else {
            soundTestBt.setEnabled(true);
          }
        } else {
          soundTestBt.setEnabled(false);
        }
      }
    });
    mSoundFileChB.getTextField().getKeyListeners()[0].keyReleased(null);

    mExecChB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        mExecFileDialogBtn.setEnabled(mExecChB.isSelected());
        if (mExecFileDialogBtn.isEnabled()) {
          showFileSettingsDialog();
        }
      }
    });

    mExecFileDialogBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showFileSettingsDialog();
      }
    });

    ItemListener autoCloseListener = new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        mAutoCloseReminderTimeSp.setEnabled(mCloseOnTime.isSelected());
        secondsLabel.setEnabled(mCloseOnTime.isSelected());
        mShowTimeCounter.setEnabled(mCloseOnTime.isSelected() || mCloseOnEnd.isSelected());
      }
    };

    mCloseOnEnd.addItemListener(autoCloseListener);
    mCloseOnTime.addItemListener(autoCloseListener);

    mCloseOnTime.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        mShowTimeCounter.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
      }
    });

    return pb.getPanel();
  }

  private void handlePluginSelection() {
    ArrayList<ProgramReceiveIf> plugins = new ArrayList<ProgramReceiveIf>();

    if(mClientPluginTargets != null) {
      for (ProgramReceiveTarget target : mClientPluginTargets) {
        if(!plugins.contains(target.getReceifeIfForIdOfTarget())) {
          plugins.add(target.getReceifeIfForIdOfTarget());
        }
      }

      ProgramReceiveIf[] mClientPlugins = plugins.toArray(new ProgramReceiveIf[plugins.size()]);

      if(mClientPlugins.length > 0) {
        mPluginLabel.setText(mClientPlugins[0].toString());
        mPluginLabel.setEnabled(true);
      }
      else {
        mPluginLabel.setText(mLocalizer.msg("noPlugins","No Plugins choosen"));
        mPluginLabel.setEnabled(false);
      }

      for (int i = 1; i < (mClientPlugins.length > 4 ? 3 : mClientPlugins.length); i++) {
        mPluginLabel.setText(mPluginLabel.getText() + ", " + mClientPlugins[i]);
      }

      if(mClientPlugins.length > 4) {
        mPluginLabel.setText(mPluginLabel.getText() + " (" + (mClientPlugins.length - 3) + " " + mLocalizer.ellipsisMsg("otherPlugins","others") + ")");
      }
    }
  }

  /**
   * Shows the Settings-Dialog for the Executable
   */
  private void showFileSettingsDialog() {
    ExecuteSettingsDialog execSettingsDialog;

    Window parent = UiUtilities.getLastModalChildOf(MainFrame.getInstance());
    execSettingsDialog = new ExecuteSettingsDialog(parent, mExecFileStr,
        mExecParamStr);

    execSettingsDialog.setVisible(true);

    if (execSettingsDialog.wasOKPressed()) {
      mExecFileStr = execSettingsDialog.getExecutable();
      mExecParamStr = execSettingsDialog.getParameters();
    }

  }


  /**
   * Called by the host-application, if the user wants to save the settings.
   */
  public void saveSettings() {
    mSettings.setProperty(ReminderPropertyDefaults.SOUNDFILE_KEY,mSoundFileChB.getTextField().getText());
    mSettings.setProperty("execfile",mExecFileStr);
    mSettings.setProperty("execparam",mExecParamStr);

    mSettings.setProperty(ReminderPropertyDefaults.REMINDER_WINDOW_SHOW, String.valueOf(mReminderWindowChB
        .isSelected()));
    mSettings.setProperty("usesound", String
        .valueOf(mSoundFileChB.isSelected()));
    mSettings.setProperty("usebeep", String.valueOf(mBeep.isSelected()));
    mSettings.setProperty("useexec", String.valueOf(mExecChB.isSelected()));

    ReminderPlugin.getInstance().setClientPluginsTargets(mClientPluginTargets);

    mSettings.setProperty("autoCloseBehaviour", mCloseOnEnd.isSelected() ? "onEnd" : mCloseNever.isSelected() ? "never" : "onTime");

    mSettings.setProperty("autoCloseReminderTime", mAutoCloseReminderTimeSp.getValue().toString());
    mSettings.setProperty("defaultReminderEntry", String.valueOf(mDefaultReminderEntryList.getSelectedIndex()));
    mSettings.setProperty("showTimeSelectionDialog", String.valueOf(mShowTimeSelectionDlg.isSelected()));
    mSettings.setProperty("showRemovedDialog", String.valueOf(mShowRemovedDlg.isSelected()));

    mSettings.setProperty("showTimeCounter", String.valueOf(!mCloseNever.isSelected() && mShowTimeCounter.isSelected()));
    mSettings.setProperty(ReminderPropertyDefaults.REMINDER_WINDOW_ALWAYS_ON_TOP, String.valueOf(mShowAlwaysOnTop.isSelected()));
    mSettings.setProperty("provideTab", String.valueOf(mProvideTab.isSelected()));
    
    mSettings.setProperty(ReminderPropertyDefaults.SCROLL_TIME_TYPE_NEXT, String.valueOf(mScrollTimeToNext.isSelected()));
    
    for(int i = 0; i < mReminderWindowPosition.length; i++) {
      if(mReminderWindowPosition[i].isSelected()) {
        mSettings.setProperty(ReminderPropertyDefaults.REMINDER_WINDOW_POSITION, String.valueOf(i));
        break;
      }
    }
    
    ReminderPlugin.getInstance().setShowDateSeparators(mShowDateSeparators.isSelected());

    ReminderPlugin.getInstance().setMarkPriority(mMarkingsPanel.getSelectedPriority());
    ReminderPlugin.getInstance().addPanel();
    
    Thread saveThread = new Thread("Save reminders") {
      public void run() {
        ReminderPlugin.getInstance().store();
      }
    };
    saveThread.setPriority(Thread.MIN_PRIORITY);
    saveThread.start();
  }

  /**
   * Returns the icon of the tab-sheet.
   */
  public Icon getIcon() {
    return IconLoader.getInstance().getIconFromTheme("apps", "appointment", 16);
  }

  /**
   * Returns the title of the tab-sheet.
   */
  public String getTitle() {
    return mLocalizer.msg("tabName", "Reminder");
  }
}