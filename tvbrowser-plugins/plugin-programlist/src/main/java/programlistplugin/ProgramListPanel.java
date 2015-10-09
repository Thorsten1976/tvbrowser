/*
 * TV-Browser
 * Copyright (C) 2012 TV-Browser team (dev@tvbrowser.org)
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

package programlistplugin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import util.program.ProgramUtilities;
import util.settings.PluginPictureSettings;
import util.settings.ProgramPanelSettings;
import util.ui.Localizer;
import util.ui.ProgramList;
import util.ui.SendToPluginDialog;
import util.ui.TVBrowserIcons;
import util.ui.UiUtilities;
import util.ui.persona.Persona;
import util.ui.persona.PersonaListener;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import devplugin.Channel;
import devplugin.Date;
import devplugin.FilterChangeListenerV2;
import devplugin.Plugin;
import devplugin.Program;
import devplugin.ProgramFilter;

/**
 * The panel with the program list.
 * 
 * @author René Mach
 */
public class ProgramListPanel extends JPanel implements PersonaListener, FilterChangeListenerV2 {
  private static final Localizer mLocalizer = ProgramListPlugin.mLocalizer;
  private static final String DATE_SEPARATOR = "DATE_SEPRATOR";
  private static final int ANCHESTOR_UPDATE_TIMEOUT = 250;
  
  /**
   * maximum number of programs to be shown in the list. If the filter has more results, only the first results are shown.
   */
  private int mMaxListSize = 5000;
  private int mUpdateCounter = 5;
  
  private JComboBox mChannelBox;
  private ProgramFilter mFilter;
  private ArrayList<Program> mPrograms = new ArrayList<Program>();
  private DefaultListModel mModel;
  private ProgramList mList;
  private ProgramPanelSettings mProgramPanelSettings;
  private JCheckBox mShowDescription;
  private JComboBox mFilterBox;
  private JComboBox mDateBox;
  private JLabel mChannelLabel, mFilterLabel, mDateLabel;
  private Thread mListThread;
  private Thread mUpdateThread;

  private JButton mSendBtn;
  private JButton mRefreshBtn;
  
  private JButton mNextDay;
  private JButton mPreviousDay;
  
  private boolean mUpdateList;
  
  private long mLastAncestorRemoved;
  
  public ProgramListPanel(final Channel selectedChannel, boolean showClose, int maxListSize) {
    mMaxListSize = maxListSize;
    createGui(selectedChannel,showClose);
  }
  
  private void createGui(final Channel selectedChannel, boolean showClose) {
    final ProgramListSettings mSettings = ProgramListPlugin.getInstance().getSettings();
    
    setLayout(new BorderLayout(0,10));
    setBorder(Borders.DIALOG);
    setOpaque(false);
    
    
    mModel = new DefaultListModel();
    boolean showDescription = mSettings.getBooleanValue(ProgramListSettings.KEY_SHOW_DESCRIPTION);
    mProgramPanelSettings = new ProgramPanelSettings(new PluginPictureSettings(
        PluginPictureSettings.ALL_PLUGINS_SETTINGS_TYPE), !showDescription, ProgramPanelSettings.X_AXIS);
    mList = new ProgramList(mModel, mProgramPanelSettings);
    final ListCellRenderer backend = mList.getCellRenderer();
    
    mList.setCellRenderer(new DefaultListCellRenderer() {
      public Component getListCellRendererComponent(final JList list, Object value, final int index, boolean isSelected,
          boolean cellHasFocus) {
        Component c = backend.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if(value instanceof String) {
          JPanel separator = new JPanel(new FormLayout("0dlu:grow,default,0dlu:grow","5dlu,default,5dlu"));
          separator.setBorder(BorderFactory.createMatteBorder(2, 0, 2, 0, UIManager.getColor("Label.foreground")));
          
          if(list.getModel().getSize() > index + 1) {
            JLabel date = new JLabel(((Program)list.getModel().getElementAt(index + 1)).getDateString());
            date.setFont(date.getFont().deriveFont(date.getFont().getSize2D() + 4).deriveFont(Font.BOLD));
            
            separator.add(date, new CellConstraints().xy(2, 2));
            
            return separator;
          }
          
        }
        
        return c;
      }
    });
    
    mList.addMouseListeners(null);

    Channel[] subscribedChannels = Plugin.getPluginManager().getSubscribedChannels();
    mChannelBox = new JComboBox(subscribedChannels);
    mChannelBox.insertItemAt(mLocalizer.msg("allChannels", "All channels"), 0);
    mChannelBox.setRenderer(new ChannelListCellRenderer());
    if (mSettings.getIndex() < mChannelBox.getItemCount()) {
      mChannelBox.setSelectedIndex(mSettings.getIndex());
    }
    
    if (selectedChannel != null) {
      mChannelBox.setSelectedItem(selectedChannel);
    }
    
    JButton resetChannelBox = new JButton(ProgramListPlugin.getInstance().createImageIcon("actions", "edit-undo", TVBrowserIcons.SIZE_SMALL));
    resetChannelBox.setToolTipText(mLocalizer.msg("reset", "Reset"));
    resetChannelBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            mChannelBox.setSelectedIndex(0);
          }
        });
      }
    });

    mFilterBox = new JComboBox();

    if (mSettings.getFilterName().isEmpty()) {
      mSettings.setFilterName(Plugin.getPluginManager().getFilterManager().getAllFilter().getName());
    }

    fillFilterBox();
    
    JButton resetFilterBox = new JButton(ProgramListPlugin.getInstance().createImageIcon("actions", "edit-undo", TVBrowserIcons.SIZE_SMALL));
    resetFilterBox.setToolTipText(mLocalizer.msg("reset", "Reset"));
    resetFilterBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateFilter(Plugin.getPluginManager().getFilterManager().getDefaultFilter());
      }
    });
    
    mDateBox = new JComboBox() {
      @Override
      public void setSelectedItem(Object anObject) {
        if(!(anObject instanceof JSeparator)) {
          super.setSelectedItem(anObject);
        }
      }
    };
    
    mDateBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value,
          int index, boolean isSelected, boolean cellHasFocus) {
        JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
            cellHasFocus);
        
        if(value instanceof Integer) {
          c.setText(mLocalizer.msg("dateFromNow", "From now"));
        }
        else if(value instanceof Date) {
          Date date = (Date)value;
          
          String dateString = date.getLongDateString();
          
          if(date.addDays(1).equals(Date.getCurrentDate())) {
            dateString = Localizer.getLocalization(Localizer.I18N_YESTERDAY) + ", " + dateString;
          }
          else if(date.equals(Date.getCurrentDate())) {
            dateString = Localizer.getLocalization(Localizer.I18N_TODAY) + ", " + dateString;
          }
          else if(date.addDays(-1).equals(Date.getCurrentDate())) {
            dateString = Localizer.getLocalization(Localizer.I18N_TOMORROW) + ", " + dateString;
          }
          
          c.setText(dateString);
        }
        else if(value instanceof JSeparator) {
          JSeparator sep = new JSeparator();
          return sep;
        }
        
        c.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        
        return c;
      }
    });
    
    fillDateBox();

    JButton resetDateBox = new JButton(ProgramListPlugin.getInstance().createImageIcon("actions", "edit-undo", TVBrowserIcons.SIZE_SMALL));
    resetDateBox.setToolTipText(mLocalizer.msg("reset", "Reset"));
    resetDateBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            mDateBox.setSelectedIndex(0);
          }
        });
      }
    });
    
    mFilterBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if(e == null || e.getStateChange() == ItemEvent.SELECTED) {
          ProgramFilter[] filterArr = ProgramListPlugin.getPluginManager().getFilterManager().getAvailableFilters();
          
          boolean found = false;
          
          for(ProgramFilter filter : filterArr) {
            if(filter.getName().equals(mFilterBox.getSelectedItem())) {
              mFilter = filter;
              
              if (mFilter != ProgramListPlugin.getInstance().getReceiveFilter()) {
                mSettings.setFilterName(mFilter.getName());
                found = true;
                break;
              }
            }
          }
          
          if(!found) {
            fillFilterBox();
          }
          
          mChannelBox.getItemListeners()[0].itemStateChanged(null);
        }
      }
    });

    mChannelBox.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        if(e == null || e.getStateChange() == ItemEvent.SELECTED) {
          fillProgramList();
        }
        
        mSettings.setIndex(mChannelBox.getSelectedIndex());
      }
    });
    
    mDateBox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if(e == null || e.getStateChange() == ItemEvent.SELECTED) {
          fillProgramList();
        }
      }
    });

    CellConstraints cc = new CellConstraints();

    JPanel panel = new JPanel(new FormLayout("1dlu,default,3dlu,default:grow,3dlu,default", "default,2dlu,default,2dlu,default,2dlu"));
    panel.setOpaque(false);
    panel.add(mDateLabel = new JLabel(mLocalizer.msg("date", "Date:")), cc.xy(2, 1));
    panel.add(mDateBox, cc.xy(4, 1));
    panel.add(resetDateBox, cc.xy(6, 1));
    panel.add(mChannelLabel = new JLabel(Localizer.getLocalization(Localizer.I18N_CHANNELS) + ":"), cc.xy(2, 3));
    panel.add(mChannelBox, cc.xy(4, 3));
    panel.add(resetChannelBox, cc.xy(6, 3));
    panel.add(mFilterLabel = new JLabel(mLocalizer.msg("filter", "Filter:")), cc.xy(2, 5));
    panel.add(mFilterBox, cc.xy(4, 5));
    panel.add(resetFilterBox, cc.xy(6, 5));

    mPreviousDay = new JButton(TVBrowserIcons.left(TVBrowserIcons.SIZE_SMALL));
    mPreviousDay.setToolTipText(mLocalizer.msg("prevDay", "Scrolls to previous day from current view position (if there is previous day in the list)"));
    mPreviousDay.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int index = mList.locationToIndex(mList.getVisibleRect().getLocation())-1;
        
        if(index > 0) {
          Object o = mList.getModel().getElementAt(index);
          
          if(o instanceof String) {
            o = mList.getModel().getElementAt(index-1);
            index--;
          }
          
          if(index > 0) {
            Date current = ((Program)o).getDate();
            
            for(int i = index-1; i >= 0; i--) {
              Object test = mList.getModel().getElementAt(i);
              
              if(test instanceof Program && current.compareTo(((Program)test).getDate()) > 0) {
                mList.ensureIndexIsVisible(i+1);
                return;
              }
            }
          }
        }
        
        if(mList.getModel().getSize() > 0) {
          mList.ensureIndexIsVisible(0);
        }
      }
    });
    
    mNextDay = new JButton(TVBrowserIcons.right(TVBrowserIcons.SIZE_SMALL));
    mNextDay.setToolTipText(mLocalizer.msg("nextDay", "Scrolls to next day from current view position (if there is next day in the list)"));
    mNextDay.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int index = mList.locationToIndex(mList.getVisibleRect().getLocation());
        
        if(index < mList.getModel().getSize() - 1) {
          Object o = mList.getModel().getElementAt(index);
          
          if(o instanceof String) {
            o = mList.getModel().getElementAt(index+1);
            index++;
          }
          
          if(index < mList.getModel().getSize() - 1) {
            Date current = ((Program)o).getDate();
            
            for(int i = index + 1; i < mList.getModel().getSize(); i++) {
              Object test = mList.getModel().getElementAt(i);
              
              if(test instanceof Program && current.compareTo(((Program)test).getDate()) < 0) {
                Point p = mList.indexToLocation(i-(ProgramListPlugin.getInstance().getSettings().getBooleanValue(ProgramListSettings.KEY_SHOW_DATE_SEPARATOR) ? 1 : 0));
                mList.scrollRectToVisible(new Rectangle(p.x,p.y,1,mList.getVisibleRect().height));
                return;
              }
            }            
          }
        }
      }
    });
    
    mSendBtn = new JButton(TVBrowserIcons.copy(TVBrowserIcons.SIZE_SMALL));
    mSendBtn.setToolTipText(mLocalizer.msg("send", "Send to other Plugins"));
    mSendBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {try {
        Object[] objects = mList.getSelectedValues();
        ArrayList<Program> programs = new ArrayList<Program>();
        
        for(Object o : objects) {
          if(o instanceof Program) {
            programs.add((Program)o);
          }
        }
        
        if (programs.size() == 0) {
          programs = mPrograms;
        }
        if (programs.size() > 0) {
          SendToPluginDialog sendDialog = new SendToPluginDialog(ProgramListPlugin.getInstance(), ProgramListPlugin.getInstance().getDialog() != null && ProgramListPlugin.getInstance().getDialog().isVisible() ? ProgramListPlugin.getInstance().getDialog() : ProgramListPlugin.getInstance().getSuperFrame(),
              programs.toArray(new Program[programs.size()]));
          sendDialog.setVisible(true);
        }
      }catch(Exception e1) {e1.printStackTrace();}
      }
    });
    
    mRefreshBtn = new JButton(TVBrowserIcons.refresh(TVBrowserIcons.SIZE_SMALL));
    mRefreshBtn.setToolTipText(mLocalizer.msg("refreshList", "Refresh list"));
    mRefreshBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        new Thread("refresh program list panel") {
          public void run() {
            fillProgramList();
            mChannelBox.getItemListeners()[0].itemStateChanged(null);            
          }
        }.start();
      }
    });

    mShowDescription = new JCheckBox(mLocalizer.msg("showProgramDescription", "Show program description"),
        showDescription);
    mShowDescription.setOpaque(false);
    mShowDescription.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        int topRow = mList.getFirstVisibleIndex();
        mProgramPanelSettings.setShowOnlyDateAndTitle(e.getStateChange() == ItemEvent.DESELECTED);
        
        if(mMaxListSize != ProgramListPlugin.MAX_PANEL_LIST_SIZE) {
          ProgramListPlugin.getInstance().updateDescriptionSelection(e.getStateChange() == ItemEvent.SELECTED);
        }
        
        mSettings.setBooleanValue(ProgramListSettings.KEY_SHOW_DESCRIPTION,e.getStateChange() == ItemEvent.SELECTED);
        mList.updateUI();
        if (topRow != -1) {
          mList.ensureIndexIsVisible(topRow);
        }
      }
    });

    JButton close = new JButton(Localizer.getLocalization(Localizer.I18N_CLOSE));
    close.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ProgramListPlugin.getInstance().closeDialog();
      }
    });

    JPanel southPanel = new JPanel(new FormLayout("default,5dlu,default,10dlu,default,5dlu,default,5dlu,default,0dlu:grow,default", "default"));
    southPanel.setOpaque(false);
    
    southPanel.add(mPreviousDay, cc.xy(1, 1));
    southPanel.add(mNextDay, cc.xy(3, 1));
    southPanel.add(mSendBtn, cc.xy(5, 1));
    southPanel.add(mRefreshBtn, cc.xy(7, 1));
    southPanel.add(mShowDescription, cc.xy(9, 1));
    
    if(showClose) {
      southPanel.add(close, cc.xy(11, 1));
    }

    add(panel, BorderLayout.NORTH);
    add(new JScrollPane(mList), BorderLayout.CENTER);
    add(southPanel, BorderLayout.SOUTH);
    
    mUpdateList = false;
    mLastAncestorRemoved = -1;
    
    addAncestorListener(new AncestorListener() {
      @Override
      public void ancestorRemoved(AncestorEvent event) {
        mLastAncestorRemoved = System.currentTimeMillis();
        mUpdateList = false;
      }
      
      @Override
      public void ancestorMoved(AncestorEvent event) {}
      
      @Override
      public void ancestorAdded(AncestorEvent event) {
        mUpdateList = true;
        
        if(mLastAncestorRemoved == -1) {
          fillProgramList();
        }
        else {
          if(System.currentTimeMillis() - mLastAncestorRemoved > ANCHESTOR_UPDATE_TIMEOUT) {
            mCurrentSelection = mList.getSelectedValue();
            mCurrentVisible = mList.getVisibleRect();
            mCurrentCount = mModel.getSize();
            
            mChannelBox.getItemListeners()[0].itemStateChanged(null);
          }
        }
      }
    });
  }
  
  void fillFilterBox() {
    mCurrentSelection = mList.getSelectedValue();
    
    // initialize filter as allFilter because we may no longer find the filter
    // with the stored name
    ProgramFilter receiveFilter = ProgramListPlugin.getInstance().getReceiveFilter();
    
    if (mFilter == null) {
      mFilter = Plugin.getPluginManager().getFilterManager().getAllFilter();
    }
    ArrayList<ProgramFilter> filters = new ArrayList<ProgramFilter>();
    for (ProgramFilter filter : Plugin.getPluginManager().getFilterManager().getAvailableFilters()) {
      filters.add(filter);
    }
    if (receiveFilter != null) {
      filters.add(receiveFilter);
    }

    for (ProgramFilter filter : filters) {
      boolean found = false;

      for (int i = 0; i < mFilterBox.getItemCount(); i++) {
        if (filter != null && filter.getName().equals(mFilterBox.getItemAt(i))) {
          found = true;
          break;
        }
      }

      if (!found) {
        mFilterBox.addItem(filter.getName());

        if ((receiveFilter == null && filter.getName().equals(ProgramListPlugin.getInstance().getSettings().getFilterName()))
            || (receiveFilter != null && filter.getName().equals(receiveFilter.getName()))) {
          mFilter = filter;
          mFilterBox.setSelectedItem(filter.getName());
        }
      }
    }

    for (int i = mFilterBox.getItemCount() - 1; i >= 0; i--) {
      boolean found = false;

      for (ProgramFilter filter : filters) {
        if (filter.getName().equals(mFilterBox.getItemAt(i))) {
          found = true;
          break;
        }
      }

      if (!found) {
        mFilterBox.removeItemAt(i);
      }
    }

  }
  
  void fillDateBox() {
    Object currentDate = mDateBox.getSelectedItem();
    JSeparator sep = new JSeparator();
    
    mDateBox.removeAllItems();
    mDateBox.addItem(Integer.valueOf(-1));
    mDateBox.addItem(sep);
    
    for(Date d = Date.getCurrentDate().addDays(-1); Plugin.getPluginManager().isDataAvailable(d); d = d.addDays(1)) {
      if(d.isFirstDayOfWeek() && mDateBox.getItemCount() > 2) {
        mDateBox.addItem(sep);
      }
      
      mDateBox.addItem(d);
    }
    
    if(currentDate != null) {
      mDateBox.setSelectedItem(currentDate);
    }
  }

  void fillProgramList() {
    if(mUpdateList && (mListThread == null || !mListThread.isAlive())) {
      mListThread = new Thread() {
        public void run() {
          if(mUpdateThread != null && mUpdateThread.isAlive()) {
            try {
              mUpdateThread.join();
            } catch (InterruptedException e) {}
          }
          
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              DefaultListModel model = new DefaultListModel();
              mFilterBox.setEnabled(false);
              mChannelBox.setEnabled(false);
              mRefreshBtn.setEnabled(false);
              mSendBtn.setEnabled(false);
              
              try {
                setPriority(MIN_PRIORITY);
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                mModel.clear();
                mPrograms.clear();
                
                Channel[] channels = mChannelBox.getSelectedItem() instanceof String ? Plugin.getPluginManager()
                    .getSubscribedChannels() : new Channel[] { (Channel) mChannelBox.getSelectedItem() };
                
                boolean dateSelected = mDateBox.getSelectedItem() instanceof Date;
                    
                Date date = dateSelected ? (Date)mDateBox.getSelectedItem() : Date.getCurrentDate();
      
                int startTime = Plugin.getPluginManager().getTvBrowserSettings().getProgramTableStartOfDay();
                int endTime = Plugin.getPluginManager().getTvBrowserSettings().getProgramTableEndOfDay();
      
                int maxDays = dateSelected ? 2 : 28;
                
                //boolean showExpired = date.compareTo(Date.getCurrentDate()) != 0;
                
                for (int d = 0; d < maxDays; d++) {
                  if (Plugin.getPluginManager().isDataAvailable(date)) {
                    for (Channel channel : channels) {
                      for (Iterator<Program> it = Plugin.getPluginManager().getChannelDayProgram(date, channel); it.hasNext();) {
                        Program program = it.next();
                        if ((dateSelected || !program.isExpired()) && mFilter.accept(program)) {
                          if (dateSelected) {
                            if ((d == 0 && program.getStartTime() >= startTime)
                                || (d == 1 && program.getStartTime() <= endTime)) {
                              mPrograms.add(program);
                            }
                          } else {
                            mPrograms.add(program);
                          }
                        }
                      }
                    }
                  }
                  date = date.addDays(1);
                }
      
                if (channels.length > 1) {
                  Collections.sort(mPrograms, ProgramUtilities.getProgramComparator());
                }
      
                int index = -1;
                
                Program lastProgram = null;
                
                int currentSelectionNewIndex = -1;
                
                for (Program program : mPrograms) {
                  if (model.size() < mMaxListSize) {
                    if(ProgramListPlugin.getInstance().getSettings().getBooleanValue(ProgramListSettings.KEY_SHOW_DATE_SEPARATOR) && 
                        (lastProgram == null || program.getDate().compareTo(lastProgram.getDate()) > 0)) {
                      model.addElement(DATE_SEPARATOR);
                    }
                    
                    model.addElement(program);
                    
                    if(mCurrentSelection != null && mCurrentSelection.equals(program)) {
                      currentSelectionNewIndex = model.getSize()-1;
                    }
                    
                    if (!program.isExpired() && index == -1) {
                      index = model.getSize() - (ProgramListPlugin.getInstance().getSettings().getBooleanValue(ProgramListSettings.KEY_SHOW_DATE_SEPARATOR) ? 2 : 1);
                    }
                    
                    lastProgram = program;
                  }
                }
                int forceScrollingIndex = model.size() - 1;
                if (forceScrollingIndex > 1000) {
                  forceScrollingIndex = 1000;
                }
                
                mCurrentSelection = null;
                
                if(currentSelectionNewIndex != -1) {
                  index = currentSelectionNewIndex;
                }
                
                if(index == -1 && dateSelected) {
                  index = 0;
                }
                
                updateList(model, forceScrollingIndex, index, currentSelectionNewIndex != -1);
                //mList.updateUI();
              } catch (Exception e) {
                e.printStackTrace();
              }
              
              mFilterBox.setEnabled(true);
              mChannelBox.setEnabled(true);
              mRefreshBtn.setEnabled(true);
              mSendBtn.setEnabled(true);
            }
          });
        }
      };
      mListThread.setDaemon(true);
      mListThread.start();
    }
  }
  
  
  private void updateList(final DefaultListModel model, final int forceScrollingIndex, final int index, final boolean select) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mModel = model;
        mList.setModel(model);
        mList.ensureIndexIsVisible(forceScrollingIndex);
        mList.ensureIndexIsVisible(index);
        
        if(select) {
          mList.setSelectedIndex(index);
        }
        else if(mCurrentVisible != null && mList.contains(mCurrentVisible.getLocation()) && mModel.getSize() == mCurrentCount) {
          mList.scrollRectToVisible(mCurrentVisible);
        }
        
        mCurrentVisible = null;
        mCurrentCount = 0;
        
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));   
        mList.updateUI();
      }
    });
  }

  private static class ChannelListCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
        boolean cellHasFocus) {
      Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      if (c instanceof JLabel && value instanceof Channel) {
        ((JLabel) c).setIcon(UiUtilities.createChannelIcon(((Channel) value).getIcon()));
      }

      return c;
    }
  }


  @Override
  public void updatePersona() {
    if(Persona.getInstance().getHeaderImage() != null) {
      mShowDescription.setForeground(Persona.getInstance().getTextColor());    }
    else {
      mShowDescription.setForeground(UIManager.getColor("Label.foreground"));
    }
    
    mChannelLabel.setForeground(mShowDescription.getForeground());
    mFilterLabel.setForeground(mShowDescription.getForeground());
    mDateLabel.setForeground(mShowDescription.getForeground());
  }
  
  private Object mCurrentSelection = null;
  private Rectangle mCurrentVisible = null;
  private int mCurrentCount = 0;
  
  synchronized void updateFilter(ProgramFilter filter) {
    if(mListThread == null || !mListThread.isAlive()) {
      if(mFilterBox.getSelectedItem() != null && mFilterBox.getSelectedItem().toString().equals(filter.getName())) {
        mCurrentVisible = mList.getVisibleRect();
        mCurrentCount = mModel.getSize();
      }
      
      fillFilterBox();
      mFilterBox.setSelectedItem(filter.getName());
    }
  }
  
  void updateDescriptionSelection(boolean value) {
    mShowDescription.setSelected(value);
  }
  
  synchronized void timeEvent() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        mUpdateCounter--;
        if(mUpdateCounter < 0) {
          mUpdateCounter = 5;
          
          if((mListThread == null || !mListThread.isAlive()) && (mUpdateThread == null || !mUpdateThread.isAlive())) {
            mUpdateThread = new Thread() {
              public void run() {
                synchronized (mModel) {
                  for(int i = mModel.getSize() - 1; i >= 0; i--) {
                    Object value = mModel.get(i);
                    if(value instanceof Program && ((Program)value).isExpired()) {
                      mModel.remove(i);
                    }
                  }                  
                }
              }
            };
            mUpdateThread.start();
          }
        }        
      }
    });
  }

  @Override
  public void filterAdded(ProgramFilter filter) {
    mFilterBox.addItem(filter.getName());
  }

  @Override
  public void filterDefaultChanged(ProgramFilter filter) {}

  @Override
  public void filterRemoved(ProgramFilter filter) {
    String selectedFilterName = (String)mFilterBox.getSelectedItem();
    
    mFilterBox.removeItem(filter.getName());
    
    if(selectedFilterName != null && selectedFilterName.equals(filter.getName())) {
      fillProgramList();
    }
  }

  @Override
  public void filterTouched(ProgramFilter filter) {
    if(mFilterBox.getSelectedItem() != null && mFilterBox.getSelectedItem().toString().equals(filter.getName())) {
      mCurrentSelection = mList.getSelectedValue();
      mCurrentVisible = mList.getVisibleRect();
      mCurrentCount = mModel.getSize();
      
      fillProgramList();
    }
  }
  
  void dateSelected(Date date) {
    if(mDateBox.getSelectedItem() == null || !mDateBox.getSelectedItem().equals(date)) {
      mDateBox.setSelectedItem(date);
    }
  }
  
  private Thread mTimeScrollWaitingThread;
  private static final int TIME_SCROLL_WAITING_TIME = 250;
  
  void nowSelected() {
    if(mTimeScrollWaitingThread != null && mTimeScrollWaitingThread.isAlive()) {
      mTimeScrollWaitingThread.interrupt();
    }
    
    if(mDateBox.getSelectedIndex() != 0) {
      mDateBox.setSelectedIndex(0);
    }
    else if(mUpdateList) {
      mList.scrollRectToVisible(new Rectangle(0,0));
    }
  }
  
  void scrollToTime(final int time) {
    if(mUpdateList) {
      mTimeScrollWaitingThread = new Thread("PROGRAM LIST SCROLL TO TIME THREAD") {
        @Override
        public void run() {
          try {
            sleep(TIME_SCROLL_WAITING_TIME);
            
            SwingUtilities.invokeLater(new Runnable() {
              @Override
              public void run() {
                if(ProgramListPlugin.getInstance().getSettings().getBooleanValue(ProgramListSettings.KEY_TAB_TIME_SCROLL_AROUND)) {
                  mList.scrollToTimeFromCurrentViewIfAvailable(time);
                }
                else {
                  mList.scrollToFirstOccurrenceOfTimeFromCurrentViewOnwardIfAvailable(time);
                }
              }
            });
          } catch (InterruptedException e1) {
            // Ignore
          }
        };
      };
      mTimeScrollWaitingThread.start();
      
    }
  }
  
  void selectChannel(Channel ch) {
    if(mChannelBox.getSelectedItem() == null || !mChannelBox.getSelectedItem().equals(ch)) {
      mChannelBox.setSelectedItem(ch);
    }
  }
}
