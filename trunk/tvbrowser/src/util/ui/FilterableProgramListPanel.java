/*
 * TV-Browser
 * Copyright (C) 2014 TV-Browser team (dev@tvbrowser.org)
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
 * SVN information:
 *     $Date$
 *   $Author$
 * $Revision$
 */
package util.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import tvbrowser.core.filters.FilterManagerImpl;
import util.exc.TvBrowserException;
import util.settings.ProgramPanelSettings;
import util.ui.persona.Persona;
import util.ui.persona.PersonaListener;

import com.jgoodies.forms.factories.CC;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import devplugin.FilterChangeListener;
import devplugin.Program;
import devplugin.ProgramFilter;

/**
 * A JPanel with a program list with filter selection.
 * 
 * @author René Mach
 * @since 3.3.4
 */
public class FilterableProgramListPanel extends JPanel implements FilterChangeListener, PersonaListener {
  private static final Localizer LOCALIZER = Localizer.getLocalizerFor(FilterableProgramListPanel.class);
  
  /** Type for filter for program title and program filter filtering */
  public static final int TYPE_NAME_AND_PROGRAM_FILTER = 0;
  /** Type for program filter filtering only */
  public static final int TYPE_PROGRAM_ONLY_FILTER = 1;
  /** Type for program title filtering only */
  public static final int TYPE_NAME_ONLY_FILTER = 2;
  
  private ProgramList mProgramList;
  private JScrollPane mProgramListScrollPane;
  private DefaultListModel mProgramListModel;
  
  private JLabel mProgramFilterLabel;
  private JComboBox mProgramFilterBox;
  
  private JComboBox mTitleFilterBox;
  private JLabel mTitleFilterLabel;
  
  private JLabel mNumberLabel;
  
  private Program[] mAllPrograms;
  
  private static final Program[] EMPTY_PROGRAM_ARR = new Program[0];
  
  private boolean mShowDateSeparators;
  
  /**
   * Create an new FilterableProgramListPanel.
   * <p>
   * @param type The type of this panel.
   * @param programs The programs to show in the list. (All programs, filtering is done in this panel of those programs.)
   * @param showNumberOfPrograms Show a panel with the number of listed programs.
   * @param showDateSeparators Show date separators in the program list.
   * @param progPanelSettings The settings for the program panels in the program list.
   */
  public FilterableProgramListPanel(int type, Program[] programs, boolean showNumberOfPrograms, boolean showDateSeparators, ProgramPanelSettings progPanelSettings) {
    mProgramListModel = new DefaultListModel();
    mProgramList = new ProgramList(mProgramListModel, progPanelSettings);
    mShowDateSeparators = showDateSeparators;
    
    FilterManagerImpl.getInstance().registerFilterChangeListener(this);
    createGUI(type, showNumberOfPrograms);
    setPrograms(programs);
  }
  
  /**
   * Sets if date separators should be shown in the list.
   * <p>
   * @param showDateSeparators <code>true</code> to show the date separators in the list, <code>false</code> otherwise.
   */
  public void setShowDateSeparators(boolean showDateSeparators) {
    mShowDateSeparators = showDateSeparators;
    
    setPrograms(mAllPrograms);
  }
  
  /**
   * Sets the programs to show in the list (All programs, filtering is done in this panel of those programs.)
   * <p>
   * @param programs The programs to show in the program list.
   */
  public void setPrograms(Program[] programs) {
    if(programs == null) {
      programs = EMPTY_PROGRAM_ARR;
    }
    
    mAllPrograms = programs;
    
    filterPrograms((ProgramFilter)mProgramFilterBox.getSelectedItem());
  }
  
  /**
   * Gets the program list of this panel. (Only contains filtered programs.)
   * <p>
   * @return The program list of this panel.
   */
  public ProgramList getProgramList() {
    return mProgramList;
  }
  
  private void createGUI(int type, boolean showNumberOfPrograms) {
    FormLayout layout = new FormLayout("default,3dlu,100dlu:grow","default,3dlu,fill:default:grow");
    
    setLayout(layout);
    
    setOpaque(false);
    
    if(type == TYPE_NAME_AND_PROGRAM_FILTER) {
      layout.insertRow(1, RowSpec.decode("default"));
      layout.insertRow(2, RowSpec.decode("3dlu"));
    }
    
    int y = 1;
    
    if(type == TYPE_NAME_AND_PROGRAM_FILTER || type == TYPE_PROGRAM_ONLY_FILTER) {
      mProgramFilterBox = new WideComboBox();
      mProgramFilterBox.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          if(e.getStateChange() == ItemEvent.SELECTED) {
            filterPrograms((ProgramFilter)mProgramFilterBox.getSelectedItem());
            scrollToFirstNotExpiredIndex(false);
          }
        }
      });
      
      fillProgramFilterBox();
      
      mProgramFilterLabel = new JLabel(LOCALIZER.msg("filterPrograms", "Program filter:"));
      
      add(mProgramFilterLabel, CC.xy(1, y));
      add(mProgramFilterBox, CC.xy(3, y++));
      
      y++;
    }
    
    if(type == TYPE_NAME_AND_PROGRAM_FILTER || type == TYPE_NAME_ONLY_FILTER) {
      mTitleFilterBox = new WideComboBox();
      mTitleFilterBox.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          if(e.getStateChange() == ItemEvent.SELECTED) {
            filterPrograms((ProgramFilter)mTitleFilterBox.getSelectedItem(), true);
            scrollToFirstNotExpiredIndex(false);
          }
        }
      });
      
      mTitleFilterLabel = new JLabel(LOCALIZER.msg("filterTitles","Title filter:"));
      
      add(mTitleFilterLabel, CC.xy(1, y));
      add(mTitleFilterBox, CC.xy(3, y++));
      
      y++;
    }
    
    if(showNumberOfPrograms) {
      layout.insertRow(y, RowSpec.decode("default"));
      layout.insertRow(y+1, RowSpec.decode("3dlu"));
      
      mNumberLabel = new JLabel(LOCALIZER.msg("numberOfPrograms", "Number of shown programs: {0}", 0));
      
      add(mNumberLabel, CC.xyw(1, y++, 3));
      
      y++;
    }
    
    mProgramListScrollPane = new JScrollPane(mProgramList);
    mProgramListScrollPane.setBorder(null);
    
    add(mProgramListScrollPane, CC.xyw(1, y, 3));
  }
  
  private void fillProgramFilterBox() {
    ProgramFilter[] filters = FilterManagerImpl.getInstance().getAvailableFilters();
    
    for(ProgramFilter filter : filters) {
      mProgramFilterBox.addItem(filter);
    }
    
    mProgramFilterBox.setSelectedItem(FilterManagerImpl.getInstance().getAllFilter());
  }

  @Override
  public void filterAdded(ProgramFilter filter) {
    mProgramFilterBox.addItem(filter);
  }

  @Override
  public void filterRemoved(ProgramFilter filter) {
    if(mProgramFilterBox.getSelectedItem().equals(filter)) {
      mProgramFilterBox.setSelectedItem(FilterManagerImpl.getInstance().getAllFilter());
    }
    
    mProgramFilterBox.removeItem(filter);
  }

  @Override
  public void filterTouched(ProgramFilter filter) {
    if(mProgramFilterBox.getSelectedItem().equals(filter)) {
      filterPrograms(filter);
    }
    
    mProgramFilterBox.updateUI();
  }
  
  private void filterPrograms(ProgramFilter filter) {
    filterPrograms(filter,false);
  }
  
  private void filterPrograms(ProgramFilter filter, boolean fromTitleFilter) {
    if(mAllPrograms != null) {
      mProgramListModel.clear();

      DefaultListModel model = new DefaultListModel();
      
      ArrayList<ProgramFilter> titleFilterValues = new ArrayList<ProgramFilter>();
      HashMap<String, String> titleMap = new HashMap<String, String>();
            
      if(FilterManagerImpl.getInstance().getAllFilter().equals(filter)) {
        for(Program p : mAllPrograms) {
          model.addElement(p);
          
          if(!fromTitleFilter && mTitleFilterBox != null && titleMap.get(p.getTitle().toLowerCase()) == null) {
            titleMap.put(p.getTitle().toLowerCase(), "available");
            titleFilterValues.add(new SimpleTitleFilter(p.getTitle()));
          }
        }
      }
      else {
        for(Program p : mAllPrograms) {
          if(filter.accept(p)) {
            model.addElement(p);
            
            if(!fromTitleFilter && mTitleFilterBox != null && titleMap.get(p.getTitle().toLowerCase()) == null) {
              titleMap.put(p.getTitle().toLowerCase(), "available");
              titleFilterValues.add(new SimpleTitleFilter(p.getTitle()));
            }
          }
        }
      }
      
      if(!fromTitleFilter && mTitleFilterBox != null) {
        mTitleFilterBox.removeAllItems();
        
        Collections.sort(titleFilterValues, new Comparator<ProgramFilter>() {
          @Override
          public int compare(ProgramFilter o1, ProgramFilter o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
          }
        });
        
        titleFilterValues.add(0,FilterManagerImpl.getInstance().getAllFilter());
        
        for(ProgramFilter titleFilter : titleFilterValues) {
          mTitleFilterBox.addItem(titleFilter);
        }
      }
      
      mProgramListModel = model;
      mProgramList.setModel(model);
            
      if(mNumberLabel != null) {
        mNumberLabel.setText(LOCALIZER.msg("numberOfPrograms", "Number of shown programs: {0}", mProgramListModel.size()));
      }
      
      if(mShowDateSeparators) {
        try {
          mProgramList.addDateSeparators();
        } catch (TvBrowserException e) {
          // ignore
        }
      }
    }
    
    mProgramList.repaint();
  }
  
  /**
   * Remove all programs from the program list.
   */
  public void clearPrograms() {
    setPrograms(EMPTY_PROGRAM_ARR);
  }
  
  /**
   * @param check If scrolling should only be done if the program in not visible.
   */
  public void scrollToFirstNotExpiredIndex(boolean check) {
    synchronized (mProgramListModel) {
      if(check) {
        int firstVisibleIndex = mProgramList.locationToIndex(mProgramList.getVisibleRect().getLocation());
        int lastVisibleIndex = mProgramList.locationToIndex(new Point(0,mProgramList.getVisibleRect().y + mProgramList.getVisibleRect().height));
        
        for(int i = firstVisibleIndex; i < lastVisibleIndex; i++) {
          Object test = mProgramListModel.getElementAt(i);
          
          if(test instanceof Program && !((Program)test).isExpired()) {
            return;
          }
        }
      }
      
      for(int i = 0; i < mProgramListModel.getSize(); i++) {try {
        Object test = mProgramListModel.getElementAt(i);
        
        if(test instanceof Program && !((Program)test).isExpired()) {
          scrollToIndex(i);
          break;
        }
      }catch(Throwable t) {t.printStackTrace();}
      }
    };
  }
  
  /**
   * Scrolls to the given index.
   * 
   * @param index The index to scroll to.
   */
  public void scrollToIndex(final int index) {
    if (index < 0) {
      return;
    }
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        mProgramListScrollPane.getVerticalScrollBar().setValue(0);
        mProgramListScrollPane.getHorizontalScrollBar().setValue(0);
        
        Rectangle cellBounds = mProgramList.getCellBounds(index,index);
        if (cellBounds != null) {
          cellBounds.setLocation(cellBounds.x, cellBounds.y + mProgramListScrollPane.getHeight() - cellBounds.height);
          mProgramList.scrollRectToVisible(cellBounds);
        }
      }
    });
  }
  
  /**
   * Scrolls to the given index.
   * ATTENTION: Date separators are not counted, so don't include them in the index.
   * 
   * @param index The index to scroll to (date separators excluded).
   */
  public void scrollToIndexWithoutDateSeparators(final int index) {
    scrollToIndex(mProgramList.getNewIndexForOldIndex(index));
  }

  @Override
  public void updatePersona() {
    if(Persona.getInstance().getHeaderImage() != null) {
      mProgramFilterLabel.setForeground(Persona.getInstance().getTextColor());
      mTitleFilterLabel.setForeground(Persona.getInstance().getTextColor());
      mNumberLabel.setForeground(Persona.getInstance().getTextColor());
    }
    else {
      mProgramFilterLabel.setForeground(UIManager.getColor("Label.foreground"));
      mTitleFilterLabel.setForeground(UIManager.getColor("Label.foreground"));
      mNumberLabel.setForeground(UIManager.getColor("Label.foreground"));
    }
  }
  
  /** Sets the filter to the given filter (only if type is {@value #TYPE_NAME_AND_PROGRAM_FILTER} or {@value #TYPE_PROGRAM_ONLY_FILTER} ) */
  public void selectFilter(ProgramFilter filter) {
    if(mProgramFilterBox != null && filter != null) {
      mProgramFilterBox.setSelectedItem(filter);
    }
  }
  
  private static final class SimpleTitleFilter implements ProgramFilter {
    private String mTitle;
  
    /**
     * @param title The title that the filter accepts
     */
    public SimpleTitleFilter(String title) {
      mTitle = title;
    }

    @Override
    public boolean accept(Program program) {
      return program.getTitle().equalsIgnoreCase(mTitle);
    }

    @Override
    public String getName() {
      return mTitle;
    }
    
    @Override
    public String toString() {
      return getName();
    }
  }
}