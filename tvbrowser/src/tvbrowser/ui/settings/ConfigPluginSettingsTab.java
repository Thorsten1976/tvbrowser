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
package tvbrowser.ui.settings;

import java.awt.BorderLayout;

import javax.swing.*;

import tvbrowser.core.PluginLoader;
import tvbrowser.core.Settings;
import devplugin.Plugin;
import devplugin.SettingsTab;

public class ConfigPluginSettingsTab implements SettingsTab, SettingsChangeListener {
 
  private static final util.ui.Localizer mLocalizer
     = util.ui.Localizer.getLocalizerFor(ConfigPluginSettingsTab.class);

 
  private Plugin mPlugin;
  private boolean mPluginIsInstalled;
  private SettingsTab mSettingsTab;
  private JCheckBox mAddToToolbarCb;
  private JPanel mContentPanel;
  private JPanel mPluginPanel;
  
  public ConfigPluginSettingsTab(devplugin.Plugin plugin) {
    mPlugin=plugin;
    mPluginIsInstalled=PluginLoader.getInstance().isActivePlugin(mPlugin);
  }
  
  
  public JPanel createSettingsPanel() {
    mContentPanel=new JPanel(new BorderLayout());
    mContentPanel.setBorder(BorderFactory.createEmptyBorder(5,8,5,8));
    PluginInfoPanel pluginInfoPanel=new PluginInfoPanel(mPlugin.getInfo());
    pluginInfoPanel.setDefaultBorder();
    mContentPanel.add(pluginInfoPanel,BorderLayout.NORTH);
    mPluginPanel=createContentPanel();
    mContentPanel.add(mPluginPanel,BorderLayout.CENTER);
    return mContentPanel;
    
  }
 
  public JPanel createContentPanel() {
    
    
    if (!mPluginIsInstalled) {
      JPanel result=new JPanel(new BorderLayout());
      result.add(new JLabel(mLocalizer.msg("notactivated","This Plugin is currently not activated.")),BorderLayout.WEST);
      return result;
    }
      
    JPanel contentPanel=new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel,BoxLayout.Y_AXIS));
     
    if (mPlugin.getButtonText()!=null) {
      mAddToToolbarCb=new JCheckBox(mLocalizer.msg("showPluginInToolBar","Show plugin in toolbar"));
      String pluginClassName = mPlugin.getClass().getName();
      boolean hidden = Settings.propHiddenPluginButtons.containsItem(pluginClassName);
      mAddToToolbarCb.setSelected(! hidden);
      JPanel panel=new JPanel(new BorderLayout());
      panel.add(mAddToToolbarCb,BorderLayout.WEST);
      contentPanel.add(panel); 
    }
    
    mSettingsTab=mPlugin.getSettingsTab();
    if (mSettingsTab!=null) {
      contentPanel.add(mSettingsTab.createSettingsPanel());
    }
        
    
    JPanel content=new JPanel(new BorderLayout());
    content.add(contentPanel,BorderLayout.NORTH);
    return content;
    
  }

  
    /**
     * Called by the host-application, if the user wants to save the settings.
     */
    public void saveSettings() {
      if (mSettingsTab!=null) {
        mSettingsTab.saveSettings();
      }
      
      if (mPluginIsInstalled && mPlugin.getButtonText()!=null && mAddToToolbarCb!=null) {
        boolean hidden = ! mAddToToolbarCb.isSelected();
        System.out.println("plugin "+mPlugin+" is hidden: "+hidden);
        String className = mPlugin.getClass().getName();
        if (!hidden) {
          Settings.propHiddenPluginButtons.removeItem(className);
        }
        else if (! Settings.propHiddenPluginButtons.containsItem(className)) {
          Settings.propHiddenPluginButtons.addItem(className);
        }
      }  
    }

  
    /**
     * Returns the name of the tab-sheet.
     */
    public Icon getIcon() {
      return mPlugin.getMarkIcon();
    }
  
  
    /**
     * Returns the title of the tab-sheet.
     */
    public String getTitle() {
      return mPlugin.getInfo().getName();
    }

		
		public void settingsChanged(SettingsTab tab, Object activatedPlugins) {
      Plugin[] activatedPluginList=(Plugin[])activatedPlugins;
      boolean oldVal=mPluginIsInstalled;
      mPluginIsInstalled=false;
      for (int i=0;i<activatedPluginList.length&&!mPluginIsInstalled;i++) {
        if (mPlugin.equals(activatedPluginList[i])) {
          mPluginIsInstalled=true;
        }
      }
      
      if (oldVal!=mPluginIsInstalled) {        
        if (mContentPanel!=null) {
          mContentPanel.remove(mPluginPanel);
          mPluginPanel=createContentPanel();
          mContentPanel.add(mPluginPanel,BorderLayout.CENTER);
          mContentPanel.updateUI();
        }
      }
      
		}
  
}