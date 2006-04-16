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
package tvbrowser.ui.programtable;

import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JScrollPane;

import tvbrowser.ui.programtable.background.BackgroundPainter;
import devplugin.Channel;

/**
 * 
 * @author Til Schneider, www.murfman.de
 */
public class ProgramTableScrollPane extends JScrollPane implements ProgramTableModelListener,
    MouseWheelListener {

  private ProgramTable mProgramTable;

  private ChannelPanel mChannelPanel;

  private boolean mBorderPainted;

  /**
   * Creates a new instance of ProgramTableScrollPane.
   */
  public ProgramTableScrollPane(ProgramTableModel model) {
    setFocusable(true);

    mProgramTable = new ProgramTable(model);
    setViewportView(mProgramTable);

    setWheelScrollingEnabled(false);
    addMouseWheelListener(this);

    getHorizontalScrollBar().setUnitIncrement(30);
    getVerticalScrollBar().setUnitIncrement(30);

    getHorizontalScrollBar().setFocusable(false);
    getVerticalScrollBar().setFocusable(false);

    mChannelPanel = new ChannelPanel(mProgramTable.getColumnWidth(), model.getShownChannels());
    setColumnHeaderView(mChannelPanel);

    setOpaque(false);
    // setBorder(mDefaultBorder);

    // NOTE: To avoid NullPointerExceptions the registration as listener must
    // happen after all member have been initialized.
    // (at the end of the constructor)
    model.addProgramTableModelListener(this);

    mProgramTable.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("backgroundpainter")) {
          BackgroundPainter painter = (BackgroundPainter) evt.getNewValue();
          handleBackgroundPainterChanged(painter);
        }
      }
    });
    handleBackgroundPainterChanged(mProgramTable.getBackgroundPainter());
  }

  public ProgramTable getProgramTable() {
    return mProgramTable;
  }

  public void forceRepaintAll() {
    getProgramTable().forceRepaintAll();
    tableDataChanged(null);
    getProgramTable().tableDataChanged(null);
  }

  public void repaint() {
    super.repaint();
    if (mProgramTable != null)
      mProgramTable.repaint();
    if (mChannelPanel != null)
      mChannelPanel.repaint();
  }

  public void updateChannelPanel() {
    mChannelPanel = new ChannelPanel(mProgramTable.getColumnWidth(), mProgramTable.getModel().getShownChannels());
    setColumnHeaderView(mChannelPanel);
    this.updateUI();
  }

  public void updateChannelLabelForChannel(Channel ch) {
    mChannelPanel.updateChannelLabelForChannel(ch);
  }

  public void setColumnWidth(int columnWidth) {
    mProgramTable.setColumnWidth(columnWidth);
    mChannelPanel.setColumnWidth(columnWidth);
  }

  public void scrollToChannel(Channel channel) {
    Channel[] shownChannelArr = mProgramTable.getModel().getShownChannels();
    for (int col = 0; col < shownChannelArr.length; col++) {
      if (channel.equals(shownChannelArr[col])) {
        Point scrollPos = getViewport().getViewPosition();
        if (scrollPos != null) {
          scrollPos.x = col * mProgramTable.getColumnWidth() - getViewport().getWidth() / 2
              + mProgramTable.getColumnWidth() / 2;
          if (scrollPos.x < 0) {
            scrollPos.x = 0;
          }
          int max = mProgramTable.getWidth() - getViewport().getWidth();
          if (scrollPos.x > max) {
            scrollPos.x = max;
          }
          getViewport().setViewPosition(scrollPos);
        }
      }
    }
  }

  public void scrollToTime(int minutesAfterMidnight) {
    Point scrollPos = getViewport().getViewPosition();

    scrollPos.y = mProgramTable.getTimeY(minutesAfterMidnight) - (getViewport().getHeight() / 4);

    if (scrollPos.y < 0) {
      scrollPos.y = 0;
    }

    int max = mProgramTable.getHeight() - getViewport().getHeight();
    if (scrollPos.y > max) {
      scrollPos.y = max;
    }

    getViewport().setViewPosition(scrollPos);
  }

  protected void handleBackgroundPainterChanged(BackgroundPainter painter) {
    setRowHeaderView(painter.getTableWest());
  }

  // implements ProgramTableModelListener

  public void tableDataChanged(Runnable callback) {
    mChannelPanel.setShownChannels(mProgramTable.getModel().getShownChannels());
  }

  public void tableCellUpdated(int col, int row) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
   */
  public void mouseWheelMoved(MouseWheelEvent e) {

    if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
      if ((e.getModifiersEx() & MouseWheelEvent.SHIFT_DOWN_MASK) != 0) {
        int amount = e.getUnitsToScroll() * getHorizontalScrollBar().getUnitIncrement();
        getHorizontalScrollBar().setValue(getHorizontalScrollBar().getValue() + amount);
      } else {
        int amount = e.getUnitsToScroll() * getVerticalScrollBar().getUnitIncrement();
        getVerticalScrollBar().setValue(getVerticalScrollBar().getValue() + amount);
      }
    }
  }

  /**
   * Go to the right program of the current program.
   * 
   */
  public void right() {
    mProgramTable.right();
  }

  /**
   * Go to the program on top of the current program.
   * 
   */
  public void up() {
    mProgramTable.up();
  }

  /**
   * Go to the program under the current program.
   * 
   */
  public void down() {
    mProgramTable.down();
  }

  /**
   * Go to the left program of the current program.
   * 
   */
  public void left() {
    mProgramTable.left();
  }

  /**
   * Opens the PopupMenu for the selected program.
   * 
   */
  public void showPopupMenu() {
    mProgramTable.showPopoupFromKeyboard();
  }

  /**
   * Starts the middle click Plugin.
   */
  public void handleMiddleClick() {
    mProgramTable.startMiddleClickPluginFromKeyboard();
  }

  /**
   * Starts the double click Plugin.
   */
  public void handleDoubleClick() {
    mProgramTable.startDoubleClickPluginFromKeyboard();
  }

  /**
   * Deselect the selected program.
   * 
   */
  public void deSelectItem() {
    mProgramTable.deSelectItem();
  }

}

// class ProgramTableBorder
