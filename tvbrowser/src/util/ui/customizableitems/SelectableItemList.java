/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (martin_oat@yahoo.de)
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

package util.ui.customizableitems;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.ListUI;

import util.ui.Localizer;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A class that provides a list that contains selectable items.
 * 
 * @author René Mach
 * 
 */
public class SelectableItemList extends JPanel implements ListSelectionListener{
  
  private static final long serialVersionUID = 1L;

  private static final Localizer mLocalizer = Localizer.getLocalizerFor(SelectableItemList.class);
  
  private SelectableItemListModel mListModel;
  private SelectableItemRenderer mItemRenderer;
  
  private JButton mSelectAllBt;
  private JButton mDeSelectAllBt;
  
  private JList mList;
  private Component[] mComponents;
  private boolean mIsEnabled = true;
  private JScrollPane mScrollPane;
  

  private JPanel mEditorComp = null; 
  private int mEditingIndex = -1; 

  /**
   * Creates the SelectableItemList without the selection buttons.
   * 
   * @param currSelection The currently selected Objects.
   * @param allItems All Objects of the list.
   */
  public SelectableItemList(Object[] currSelection, Object[] allItems) {
    this(currSelection,allItems,null);
  }

  
  /**
   * Creates the SelectableItemList without the selection buttons.
   * 
   * @param currSelection The currently selected Objects.
   * @param allItems All Objects of the list.
   * @param notSelectableItems All Objects that could not be selected/deselected
   * 
   * @since 2.7.2
   */
  public SelectableItemList(Object[] currSelection, Object[] allItems, Object[] notSelectableItems) {
    this(currSelection, allItems, false, notSelectableItems);
  }
  
  /**
   * Creates the SelectableItemList.
   * 
   * @param currSelection The currently selected Objects.
   * @param allItems All Objects of the list.
   * @param showSelectionButtons If the selection buttons should be shown.
   */
  public SelectableItemList(Object[] currSelection, Object[] allItems, boolean showSelectionButtons) {
    this(currSelection,allItems,showSelectionButtons,null);
  }
  
  /**
   * Creates the SelectableItemList.
   * 
   * @param currSelection The currently selected Objects.
   * @param allItems All Objects of the list.
   * @param showSelectionButtons If the selection buttons should be shown.
   * @param notSelectableItems All Objects that could not be selected/deselected
   * 
   * @since 2.7.2
   */
  public SelectableItemList(Object[] currSelection, Object[] allItems, boolean showSelectionButtons, Object[] notSelectableItems) {
    setLayout(new BorderLayout(0,3));
    
    mListModel = new SelectableItemListModel();
    setEntries(currSelection,allItems,notSelectableItems);
    
    mList = new JList(mListModel);
    mList.setCellRenderer(mItemRenderer = new SelectableItemRenderer());
    
    mScrollPane = new JScrollPane(mList);
    
    mScrollPane.getVerticalScrollBar().setBlockIncrement(50);
    mScrollPane.getVerticalScrollBar().setUnitIncrement(20);
    
    mItemRenderer.setScrollPane(mScrollPane);
    
    add(mScrollPane, BorderLayout.CENTER);
    
    mList.addListSelectionListener(this);
    
    mList.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent evt) {
        int index = mList.locationToIndex(evt.getPoint());
        
        if (index != -1) {
          Rectangle oldCellBounds = mList.getCellBounds(index,index);
          
          calculateSize();
          addEditor(index);
          
          if (evt.getX() <= mItemRenderer.getSelectionWidth() && mIsEnabled) {
            if(oldCellBounds.contains(evt.getPoint())) {
              SelectableItem item = (SelectableItem) mListModel.getElementAt(index);
              if(item.isSelectable()) {
                item.setSelected(! item.isSelected());
                handleItemSelectionChanged();
                mList.repaint();
              }
            }
          }
        }
      }
    });
    mList.addKeyListener(new KeyAdapter(){
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          addEditor(mList.getSelectedIndex());
          
          Object[] objs = mList.getSelectedValues();
          for (Object obj : objs) {
            if (obj instanceof SelectableItem) {
              SelectableItem item = (SelectableItem) obj;
              item.setSelected(!item.isSelected());
            }
          }
          handleItemSelectionChanged();
          mList.repaint();
        }
      }
      
      public void keyReleased(KeyEvent e) {
        calculateSize();
        addEditor(mList.getSelectedIndex());
      }
    });
    
    CellConstraints cc = new CellConstraints();
    FormLayout layout = new FormLayout("pref,3dlu:grow,pref","pref");
    layout.setColumnGroups(new int[][] {{1,3}});
    
    JPanel p3 = new JPanel(layout);
    
    mSelectAllBt = new JButton(mLocalizer.msg("addAll", "Select all items"));
    mSelectAllBt.setToolTipText(mLocalizer.msg("tooltip.all", "Select all items in the list."));
    mSelectAllBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        selectAll();
      }
    });
    p3.add(mSelectAllBt, cc.xy(1,1));

    mDeSelectAllBt = new JButton(mLocalizer.msg("delAll", "Deselect all items"));
    mDeSelectAllBt.setToolTipText(mLocalizer.msg("tooltip.none", "Deselect all items in the list."));
    mDeSelectAllBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearSelection();
      }
    });
    p3.add(mDeSelectAllBt, cc.xy(3,1));
    
    if(showSelectionButtons) {
      add(p3, BorderLayout.SOUTH);
    }
    
  }
  
  private void handleItemSelectionChanged() {
    if(mEditorComp != null) {
      JCheckBox cb = ((JCheckBox) mEditorComp.getComponent(0));
      cb.setSelected(((SelectableItem) mListModel.getElementAt(mEditingIndex)).isSelected());
      mEditorComp.repaint();
      
      ListSelectionListener[] listeners = mList.getListSelectionListeners();
      if(listeners != null) {
        for(ListSelectionListener listener : listeners) {
          if(mList.getSelectedValue() != null) {
            int[] indices = mList.getSelectedIndices();
            listener.valueChanged(new ListSelectionEvent(mList.getSelectedValue(), indices[0], indices[indices.length-1], false));
          }
        }
      }
    }
  }
  
  /**
   * @since 2.5
   * @param listener Add this Listener
   */
  public void addListSelectionListener(ListSelectionListener listener) {
    mList.addListSelectionListener(listener);
  }
  
  /**
   * @since 2.5
   * @param listener Remove this Listener
   */
  public void removeListSelectionListener(ListSelectionListener listener) {
    mList.removeListSelectionListener(listener);
  }

  /**
   * @param selectionMode The selection mode of the list.
   * @since 2.5
   */
  public void setSelectionMode(int selectionMode) {
    mList.setSelectionMode(selectionMode);
  }
  
  /**
   * Current selected Items in the List.
   * 
   * Attention: This is not a List with all activated Items.
   * 
   * @since 2.5
   * @return Current selected Items in the List
   */
  public Object[] getListSelection() {
    Object[] values = mList.getSelectedValues();
    
    Object[] items = new Object[values.length];
    
    int max = values.length;
    for (int i=0;i< max;i++) {
      items[i] = ((SelectableItem)values[0]).getItem();
    }
    
    return items;
  }
  
  private void setEntries(Object[] currSelection, Object[] allItems, Object[] disabledItems) {
    mListModel.removeAllElements();
    mComponents = new Component[allItems.length];
    
    ArrayList<Object> selectionList = new ArrayList<Object>();
    
    for (Object element : currSelection) {
      selectionList.add(element);
    }
    
    for (int i = 0; i < allItems.length; i++) {
      SelectableItem item = new SelectableItem(allItems[i], selectionList.remove(allItems[i]),!arrayContainsItem(disabledItems,allItems[i]));
      mListModel.addElement(item);
    }
  }
  
  private boolean arrayContainsItem(Object[] itemArr, Object item) {
    if(item != null && itemArr != null) {
      for(Object arrayItem : itemArr) {
        if(arrayItem != null && arrayItem.equals(item)) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  /**
   * Attention: This is not a List with all selected Items in the List. This List
   * is a List with all checked Items
   * 
   * @return The selected Objects
   */
  public Object[] getSelection() {
    return mListModel.getSelection();
  }
  
  /**
   * @return The current selected value (value that has focus)
   * @since 2.5
   */
  public Object getSelectedValue() {
    return mList.getSelectedValue();
  }

  /**
   * set the (focus) selection to the item with the given index
   * 
   * @param index
   * @since 3.0
   */
  public void setSelectedIndex(int index) {
    mList.setSelectedIndex(index);
  }

  /**
   * Invert the selection
   */
  public void invertSelection() {
    removeEditor();
    if(mIsEnabled) {
      mListModel.invertSelection();
      mList.repaint();
    }
  }

  /**
   * Select all items.
   */
  public void selectAll() {
    removeEditor();
    if(mIsEnabled) {
      mListModel.selectAll();
      mList.repaint();
    }
  }
  
  /**
   * Clear the selection.
   */
  public void clearSelection() {
    removeEditor();
    if(mIsEnabled) {
      mListModel.clearSelection();
      mList.repaint();
    }
  }
  
  public void setEnabled(boolean value) {
    mIsEnabled = value;
    mItemRenderer.setEnabled(value);
    mList.setEnabled(value);
    mSelectAllBt.setEnabled(value);
    mDeSelectAllBt.setEnabled(value);
    mScrollPane.getVerticalScrollBar().setEnabled(value);
    mScrollPane.setWheelScrollingEnabled(value);
  }

  /**
   * Calculates the size of the list.
   */
  public void calculateSize() {
    if(mList != null) {
      mList.setSize(mList.getPreferredSize());
      mList.ensureIndexIsVisible(mList.getSelectedIndex());
      mList.repaint();
    }
  }
  
  /**
   * Adds the render component that is to be used for the given class or it's super class.
   * <p>
   * @param clazz The class to use the render component for, the render component is also used for the super class of clazz.
   * @param component The render component.
   * @since 2.7
   */

  public void addCenterRendererComponent(Class<?> clazz, SelectableItemRendererCenterComponentIf component) {
    mItemRenderer.setCenterRendererComponent(clazz,component);
  }
  
  /**
   * Sets the UI to be used for the list.
   * <p>
   * @param ui The list ui that should be used for the list.
   * @since 2.7
   */
  public void setListUI(ListUI ui) {
    mList.setUI(ui);
  }
  
  /**
   * Sets if the horizontal scroll policy.
   * <p>
   * @param value The values from ScrollPaneConstants.
   * @since 2.7
   */
  public void setHorizontalScrollBarPolicy(int value) {
    mScrollPane.setHorizontalScrollBarPolicy(value);
  }
  
  /**
   * Adds a mouse listener to the list.
   * <p>
   * @param listener The listener to add.
   * @since 2.7
   */
  public void addMouseListener(MouseListener listener) {
    mList.addMouseListener(listener);
  }
  
  /**
   * Sets the combo box that contains ItemFilters to filter
   * the shown values of the list.
   * <p>
   * @param filterBox The combo box with the ItemFilters.
   * @since 2.7
   */
  public void setFilterComboBox(JComboBox filterBox) {
    mListModel.setComboBox(filterBox);
  }

  /**
   * Removes the editor for selected cell
   * <p>
   * @since 3.3.3
   */
  private void removeEditor() {
    mList.invalidate();
    if (mEditorComp != null) {
      mEditorComp.setVisible(false);
      mEditorComp.setEnabled(false);
      mList.remove(mEditorComp);
    }

    mEditingIndex = -1;
    mEditorComp = null;
    mList.validate();
    mList.repaint();
  }

  /**
   * Adds the editor for selected cell
   * <p>
   * @since 3.3.3
   */

  private boolean addEditor(int index) {
    // vertical correction
    int blockcorrection = 0;
    if (mEditorComp != null) {
      if (mEditingIndex < index) {
        blockcorrection = mList.getCellBounds(mEditingIndex, mEditingIndex).height - mList.getCellBounds(index, index).height;
      }
      // remove old Editor
      removeEditor();
    }
    mList.repaint();
    if (index < 0 || index >= mListModel.getSize())
      return false;
    mList.validate();

    mEditorComp = mItemRenderer.getListCellComponent(mList, mListModel.getElementAt(index), index, true, true);
    mEditorComp.validate();
    Rectangle cb = mList.getCellBounds(index, index);
    if (blockcorrection > 0) {
      cb.y -= blockcorrection;
    }
    mEditorComp.setBounds(cb);
    mEditingIndex = index;
    mList.add(mEditorComp);

    mList.validate();
    ((JCheckBox) mEditorComp.getComponent(0)).addItemListener(new ItemListener(){
      public void itemStateChanged(ItemEvent arg0) {
        JCheckBox cb = ((JCheckBox) mEditorComp.getComponent(0));
        ((SelectableItem) mListModel.getElementAt(mEditingIndex)).setSelected(cb.isSelected());
        handleItemSelectionChanged();
      }
      
    });
    mEditorComp.repaint();

    return true;
  }
  
  private class SelectableItemListModel extends AbstractListModel {
    private JComboBox mFilterBox;
    
    private ArrayList<SelectableItem> mFullList = new ArrayList<SelectableItem>();
    private ArrayList<SelectableItem> mFilteredList = new ArrayList<SelectableItem>();
    
    protected void setComboBox(JComboBox filterBox) {
      mFilterBox = filterBox;
      
      mFilterBox.addItemListener(new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          if(e.getStateChange() == ItemEvent.SELECTED) {
            mFilteredList.clear();
            removeEditor();
            
            Object filter = mFilterBox.getSelectedItem();
            
            for(SelectableItem o : mFullList) {
              if(filter instanceof ItemFilter) {
                if((((ItemFilter)filter).accept(o.getItem()))) {
                  mFilteredList.add(o);
                }
              }
              else {
                mFilteredList.add(o);
              }
            }
            
            fireIntervalRemoved(this,0,mFullList.size());
            fireIntervalAdded(this,0,mFilteredList.size());
          }
        }

      });
    }

    protected void addElement(SelectableItem o) {
      mFullList.add(o);
      
      if(mFilterBox != null) {
        Object filter = mFilterBox.getSelectedItem();
        
        if(filter instanceof ItemFilter) {
          if((((ItemFilter)filter).accept(o.getItem()))) {
            mFilteredList.add(o);
          }
        }
        else {
          mFilteredList.add(o);
        }
      }
      else {
        mFilteredList.add(o);
      }
    }

    public SelectableItem getElementAt(int index) {
      return mFilteredList.get(index);
    }

    public int getSize() {
      return mFilteredList.size();
    }
    
    @SuppressWarnings("unused")
    protected int size() {
      return getSize();
    }
    
    protected void removeAllElements() {
      mFullList.clear();
      mFilteredList.clear();
    }
    
    protected Object[] getSelection() {
      ArrayList<Object> objList = new ArrayList<Object>();
      for (int i = 0; i < mFullList.size(); i++) {
        SelectableItem item = mFullList.get(i);
        if (item.isSelected()) {
          objList.add(item.getItem());
        }
      }

      Object[] asArr = new Object[objList.size()];
      objList.toArray(asArr);

      return asArr;
    }
    
    protected void selectAll() {
      for (int i = 0; i < mFullList.size(); i++) {
        SelectableItem item = (SelectableItem) mFullList.get(i);
        item.setSelected(true);
      }
    }
    
    protected void clearSelection() {
      for (int i = 0; i < mFullList.size(); i++) {
        SelectableItem item = (SelectableItem) mFullList.get(i);
        item.setSelected(false);
      }
    }
    
    protected void invertSelection() {
      for (int i = 0; i < mFullList.size(); i++) {
        SelectableItem item = (SelectableItem) mFullList.get(i);
        item.setSelected(!item.isSelected());
      }
    }
  }

  public int getItemCount() {
    return mListModel.getSize();
  }
  
  /**
   * Sets the vertical scroll bar block increment
   * <p>
   * @param value The scroll bar block increment
   * @since 3.1
   */
  public void setVerticalScrollBarBlockIncrement(int value) {
    mScrollPane.getVerticalScrollBar().setBlockIncrement(value);
  }

  public void valueChanged(ListSelectionEvent e) {
    for(int i = e.getFirstIndex(); i<= e.getLastIndex();++i){
      mComponents[i] = mItemRenderer.getListCellRendererComponent(mList, mListModel.getElementAt(i), i, mList.isSelectedIndex(i), true); 
    }
    
  }
  
  /**
   * Gets if this selectable item list is empty.
   * <p>
   * @return <code>true</code> if this list is empty.
   * @since 3.4.3
   */
  public boolean isEmpty() {
    return mListModel.getSize() == 0;
  }
}
