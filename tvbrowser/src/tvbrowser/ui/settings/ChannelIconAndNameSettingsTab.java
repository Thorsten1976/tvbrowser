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
 *     $Date$
 *   $Author$
 * $Revision$
 */
package tvbrowser.ui.settings;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import tvbrowser.core.Settings;
import util.settings.ProgramPanelSettings;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;

import devplugin.SettingsTab;

/**
 * Settings for the icon and name values in
 * program table and channel list.
 * 
 * @author René Mach
 */
public class ChannelIconAndNameSettingsTab implements SettingsTab {
  private static final util.ui.Localizer mLocalizer = util.ui.Localizer.getLocalizerFor(ChannelIconAndNameSettingsTab.class);
  
  private JRadioButton mShowIconAndNameInProgramTable;
  private JRadioButton mShowOnlyIconInProgramTable;
  private JRadioButton mShowOnlyNameInProgramTable;

  private JRadioButton mShowIconInProgramPanelPlugins;
  private JRadioButton mShowIconInProgramPanelNever;
  
  private JRadioButton mShowIconAndNameInChannelLists;
  private JRadioButton mShowOnlyIconInChannelLists;
  private JRadioButton mShowOnlyNameInChannelLists;

  private JCheckBox mShowTooltipInProgramTable;
  
  private JCheckBox mShowSortNumberInProgramTable;
  private JCheckBox mShowSortNumberInChannelLists;
  
  /**
   * Creates the settings panel for this tab.
   */
  public JPanel createSettingsPanel() {
    PanelBuilder pb = new PanelBuilder(new FormLayout("5dlu, default:grow, 5dlu",
    "default,5dlu,default,default,default,5dlu,default,default,10dlu,default," +
    "5dlu,default,default,10dlu,default,5dlu,default,default,default,5dlu,default"));
    pb.border(Borders.DIALOG);

    int y = 1;
    
    pb.addSeparator(mLocalizer.msg("programTable","Program table"), CC.xyw(1,y++,3));
    pb.add(mShowIconAndNameInProgramTable = new JRadioButton(mLocalizer.msg("showIconAndName","Show channel icon and channel name"), Settings.propShowChannelIconsInProgramTable.getBoolean() && Settings.propShowChannelNamesInProgramTable.getBoolean()), CC.xy(2,++y));
    pb.add(mShowOnlyIconInProgramTable = new JRadioButton(mLocalizer.msg("showOnlyIcon","Show channel icon"), Settings.propShowChannelIconsInProgramTable.getBoolean() && !Settings.propShowChannelNamesInProgramTable.getBoolean()), CC.xy(2,++y));
    pb.add(mShowOnlyNameInProgramTable = new JRadioButton(mLocalizer.msg("showOnlyName","Show channel name"), Settings.propShowChannelNamesInProgramTable.getBoolean() && !Settings.propShowChannelIconsInProgramTable.getBoolean()), CC.xy(2,++y));
    
    y += 2;
    
    pb.add(mShowSortNumberInProgramTable = new JCheckBox(mLocalizer.msg("showChannelNumber", "Show sort number"), Settings.propShowSortNumberInProgramTable.getBoolean()), CC.xy(2, y++));
    pb.add(mShowTooltipInProgramTable = new JCheckBox(mLocalizer.msg("showToolTip","Show large channel icons in tooltip"), Settings.propShowChannelTooltipInProgramTable.getBoolean()), CC.xy(2,y));
    
    mShowTooltipInProgramTable.setEnabled(!mShowOnlyNameInProgramTable.isSelected());
    
    mShowOnlyNameInProgramTable.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        mShowTooltipInProgramTable.setEnabled(e.getStateChange() == ItemEvent.DESELECTED);
      }
    });
    
    ButtonGroup programTable = new ButtonGroup();
    programTable.add(mShowIconAndNameInProgramTable);
    programTable.add(mShowOnlyIconInProgramTable);
    programTable.add(mShowOnlyNameInProgramTable);
    
    y += 2;
    
    pb.addSeparator(mLocalizer.msg("programPanels", "Program panels"),CC.xyw(1,y++,3));
    pb.add(mShowIconInProgramPanelPlugins = new JRadioButton(mLocalizer.msg("showLogoPlugins", "Plugins decide showing of channel logos"), Settings.propShowChannelLogoForProgramPanel.getInt() == ProgramPanelSettings.SHOW_CHANNEL_LOGO_PLUGINS_CONTROL), CC.xy(2,++y));
    pb.add(mShowIconInProgramPanelNever = new JRadioButton(mLocalizer.msg("showLogoNever", "Channel logos are never shown"), Settings.propShowChannelLogoForProgramPanel.getInt() == ProgramPanelSettings.SHOW_CHANNEL_LOGO_NEVER), CC.xy(2,++y));
    
    ButtonGroup programPanels = new ButtonGroup();
    programPanels.add(mShowIconInProgramPanelPlugins);
    programPanels.add(mShowIconInProgramPanelNever);
    
    y += 2;
    
    pb.addSeparator(mLocalizer.msg("channelLists","Channel lists"), CC.xyw(1,y++,3));
    pb.add(mShowIconAndNameInChannelLists = new JRadioButton(mLocalizer.msg("showIconAndName","Show channel icon and channel name"), Settings.propShowChannelIconsInChannellist.getBoolean() && Settings.propShowChannelNamesInChannellist.getBoolean()), CC.xy(2,++y));
    pb.add(mShowOnlyIconInChannelLists = new JRadioButton(mLocalizer.msg("showOnlyIcon","Show channel icon"), Settings.propShowChannelIconsInChannellist.getBoolean() && !Settings.propShowChannelNamesInChannellist.getBoolean()), CC.xy(2,++y));
    pb.add(mShowOnlyNameInChannelLists = new JRadioButton(mLocalizer.msg("showOnlyName","Show channel name"), Settings.propShowChannelNamesInChannellist.getBoolean() && !Settings.propShowChannelIconsInChannellist.getBoolean()), CC.xy(2,++y));
    
    y += 2;
    
    pb.add(mShowSortNumberInChannelLists = new JCheckBox(mLocalizer.msg("showChannelNumber", "Show sort number"), Settings.propShowSortNumberInProgramLists.getBoolean()), CC.xy(2, y++));
    
    ButtonGroup channelLists = new ButtonGroup();
    channelLists.add(mShowIconAndNameInChannelLists);
    channelLists.add(mShowOnlyIconInChannelLists);
    channelLists.add(mShowOnlyNameInChannelLists);
    
    return pb.getPanel();
  }
  
    /**
   * Called by the host-application, if the user wants to save the settings.
   */
  public void saveSettings() {
    Settings.propShowChannelIconsInProgramTable.setBoolean(mShowIconAndNameInProgramTable.isSelected() || mShowOnlyIconInProgramTable.isSelected());
    Settings.propShowChannelNamesInProgramTable.setBoolean(mShowIconAndNameInProgramTable.isSelected() || mShowOnlyNameInProgramTable.isSelected());
    
    if(mShowIconInProgramPanelPlugins.isSelected()) {
      Settings.propShowChannelLogoForProgramPanel.setInt(ProgramPanelSettings.SHOW_CHANNEL_LOGO_PLUGINS_CONTROL);
    }
    else {
      Settings.propShowChannelLogoForProgramPanel.setInt(ProgramPanelSettings.SHOW_CHANNEL_LOGO_NEVER);
    }
    
    Settings.propShowChannelIconsInChannellist.setBoolean(mShowIconAndNameInChannelLists.isSelected() || mShowOnlyIconInChannelLists.isSelected());
    Settings.propShowChannelNamesInChannellist.setBoolean(mShowIconAndNameInChannelLists.isSelected() || mShowOnlyNameInChannelLists.isSelected());

    Settings.propShowSortNumberInProgramTable.setBoolean(mShowSortNumberInProgramTable.isSelected());
    Settings.propShowChannelTooltipInProgramTable.setBoolean(mShowTooltipInProgramTable.isSelected());
    Settings.propShowSortNumberInProgramLists.setBoolean(mShowSortNumberInChannelLists.isSelected());
  }

  /**
   * Returns the name of the tab-sheet.
   */
  public Icon getIcon() {
    return null;
  }

  /**
   * Returns the title of the tab-sheet.
   */
  public String getTitle() {
    return mLocalizer.msg("title", "Channel icons and names");
  }
}