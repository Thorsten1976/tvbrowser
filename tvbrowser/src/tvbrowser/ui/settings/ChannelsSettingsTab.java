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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import devplugin.Channel;
import tvbrowser.ui.customizableitems.CustomizableItemsPanel;
import tvbrowser.core.*;

/**
 * TV-Browser
 *
 * @author Martin Oberhauser
 */
public class ChannelsSettingsTab implements devplugin.SettingsTab {

  private static java.util.logging.Logger mLog
    = java.util.logging.Logger.getLogger(ChannelsSettingsTab.class.getName());
  
  private static final util.ui.Localizer mLocalizer
    = util.ui.Localizer.getLocalizerFor(ChannelsSettingsTab.class);
  
  private JPanel mSettingsPn;
  
  private CustomizableItemsPanel panel;

  
  
  public ChannelsSettingsTab() {
  }
 
  
  
  /**
   * Creates the settings panel for this tab.
   */
  public JPanel createSettingsPanel() {
    String msg;
    
    mSettingsPn = new JPanel(new BorderLayout());
    mSettingsPn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	
    String leftText = mLocalizer.msg("availableChannels", "Available channels");
    String rightText = mLocalizer.msg("subscribedChannels", "Subscribed channels");
    panel = CustomizableItemsPanel.createCustomizableItemsPanel(leftText, rightText);
    panel.setBorder(BorderFactory.createEmptyBorder(3,3,10,3));
    JTextArea textArea=new JTextArea(2,40);
    textArea.setOpaque(false);
    textArea.setEditable(false);
    textArea.setDisabledTextColor(Color.black);
    textArea.setEnabled(false);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    msg = mLocalizer.msg("infoText", "Use the left and right buttons to "
      + "subscribe or unsubscribe channels and the up and down buttons to "
      + "create your personal ordering of your favourite channels.");
    textArea.setText(msg);
    textArea.setBorder(BorderFactory.createEmptyBorder(10,5,5,0));

    final JButton configChannelBtn=new JButton(mLocalizer.msg("configSelectedChannels","Configure selected channels"));
    JPanel southPn=new JPanel();
    southPn.setLayout(new BoxLayout(southPn,BoxLayout.Y_AXIS));
    southPn.add(configChannelBtn);
    southPn.add(textArea);
    
    final JList rightList=panel.getRightList();
    int []sel=rightList.getSelectedIndices();
    configChannelBtn.setEnabled(sel.length>0);
    rightList.addListSelectionListener(new ListSelectionListener() {
       public void valueChanged(ListSelectionEvent e) {
         int []sel=rightList.getSelectedIndices();
         configChannelBtn.setEnabled(sel.length>0);
       }
    });
    
    configChannelBtn.addActionListener(new ActionListener(){
      public void actionPerformed(ActionEvent e) {
          Object[] o=panel.getRightSelections();
          
          Channel[] channelList=new Channel[o.length];
          for (int i=0;i<o.length;i++) {
            channelList[i]=(Channel)o[i];
          }
          ChannelConfigDlg dlg=new ChannelConfigDlg(mSettingsPn,mLocalizer.msg("configSelectedChannels","Configure selected channels"),channelList);
          dlg.centerAndShow();
      }
      
    });
    
    
    
  
    mSettingsPn.add(panel,BorderLayout.CENTER);
    mSettingsPn.add(southPn,BorderLayout.SOUTH);

    Channel ch;
    Iterator iter = ChannelList.getChannels();

    Channel[] subscribedChannels=new Channel[ChannelList.getNumberOfSubscribedChannels()];
    mLog.fine("ok till now");
    while (iter.hasNext()) {
      ch = (Channel) iter.next();
      mLog.fine("channel "+ch.getName());
      //if (ch.isSubscribed()) {
      if (ChannelList.isSubscribedChannel(ch)) {
      	mLog.fine("is subscribed");
      //  subscribedChannels[ch.getPos()]=ch;
     	int pos=ChannelList.getPos(ch);
      	subscribedChannels[pos]=ch;
      }else{
        panel.addElementLeft(ch);
        mLog.fine("is NOT subscribed");
      }
    }
    mLog.fine("done");
    
    mLog.fine("subscribedChannel.length: "+subscribedChannels.length);

    for (int i=0;i<subscribedChannels.length;i++) {
      panel.addElementRight(subscribedChannels[i]);
    }
    
    return mSettingsPn;
  }

  
  
  /**
   * Called by the host-application, if the user wants to save the settings.
   */
  public void saveSettings() {
    Object[] list = panel.getElementsRight();
    
    // Convert the list into a Channel[] and fill channels
    Channel[] channelArr = new Channel[list.length];
    for (int i = 0; i < list.length; i++) {
      channelArr[i] = (Channel) list[i];      
    }
    
    ChannelList.setSubscribeChannels(channelArr);
    Settings.setSubscribedChannels(channelArr);
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
    return mLocalizer.msg("channels", "Channels");
  }

}