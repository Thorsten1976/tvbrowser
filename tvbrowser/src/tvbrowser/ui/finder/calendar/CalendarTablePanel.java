/*
 * Copyright Michael Keppler
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package tvbrowser.ui.finder.calendar;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import devplugin.Date;

public class CalendarTablePanel extends AbstractCalendarPanel implements ListSelectionListener {
  private static final int COLUMNS = 7;

  private JTable mTable;
  private CalendarTableModel mTableModel;

  private int mLastColumn = -1;

  private int mLastRow = -1;

  private boolean mAllowEvents = true;
  
  private static final long LAST_MARK_TIMEOUT = 200;
  
  private long mLastMarkTime;

  public CalendarTablePanel(KeyListener keyListener) {
    setLayout(new BorderLayout());
    mTableModel = new CalendarTableModel(getFirstDate());
    mTable = new JTable(mTableModel);
    mTable.addKeyListener(keyListener);
    CalendarTableCellRenderer renderer = new CalendarTableCellRenderer();
    mTable.setDefaultRenderer(Date.class, renderer);
    mTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mTable.setRowSelectionAllowed(false);
    mTable.setRowHeight(mTable.getFont().getSize()+4);
    mTable.setColumnSelectionAllowed(false);
    mTable.setCellSelectionEnabled(true);
    mTable.setFillsViewportHeight(true);
    mTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    mTable.setShowGrid(true);
    for (int i=0; i < COLUMNS; i++) {
      TableColumn column = mTable.getColumnModel().getColumn(i);
      column.setResizable(false);
    }
    mTable.getTableHeader().setReorderingAllowed(false);
    mTable.getTableHeader().setFont(new Font(mTable.getTableHeader().getFont().getFontName(), Font.PLAIN, mTable.getTableHeader().getFont().getSize() - 2));
    JScrollPane pane = new JScrollPane(mTable);
    pane.getViewport().addKeyListener(keyListener);
    pane.getVerticalScrollBar().addKeyListener(keyListener);
    pane.getHorizontalScrollBar().addKeyListener(keyListener);
    pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    add(pane, BorderLayout.CENTER);
    addMouseListener(this);
    mTable.getSelectionModel().addListSelectionListener(this);
    mTable.getColumnModel().getSelectionModel().addListSelectionListener(this);
    mTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        int clickColumn = mTable.columnAtPoint(e.getPoint());
        int clickRow = mTable.rowAtPoint(e.getPoint());
        
        if(mLastRow == clickRow && mLastColumn == clickColumn) {
          Date date = (Date) mTable.getValueAt(mLastRow, mLastColumn);
          markDate(date,true);
        }
      }
    });
  }

  protected void rebuildControls() {
  }

  
  @Override
  public void markDate(Date date, boolean informPluginPanels) {
    if(System.currentTimeMillis() - mLastMarkTime > LAST_MARK_TIMEOUT) {
      super.markDate(date, informPluginPanels);
    }
  }

  public void markDate(final Date date, final Runnable callback, final boolean informPluginPanels) {
    if (!isValidDate(date)) {
      askForDataUpdate(date);
      return;
    }
    
    mLastMarkTime = System.currentTimeMillis();
    
    Thread thread = new Thread("Finder") {
      public void run() {
        mDateChangedListener.dateChanged(date, CalendarTablePanel.this, callback, informPluginPanels);
      }
    };

    if (date.equals(getSelectedDate())) {
      thread.start();
      
      return;
    }

    setCurrentDate(date);
    mTable.getColumnModel().getSelectionModel().removeListSelectionListener(this);
    mTable.getSelectionModel().removeListSelectionListener(this);
    mTableModel.setCurrentDate(date);
    mTable.getColumnModel().getSelectionModel().addListSelectionListener(this);
    mTable.getSelectionModel().addListSelectionListener(this);

    Point position = mTableModel.getPositionOfDate(date);
    if (position != null) {
      mAllowEvents  = false;
      mTable.setColumnSelectionInterval(position.x, position.x);
      mAllowEvents = true;
      mTable.setRowSelectionInterval(position.y, position.y);
    }
    
    mLastColumn = mTable.getSelectedColumn();
    mLastRow = mTable.getSelectedRow();

    if (mDateChangedListener == null) {
      return;
    }

    thread.start();

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        repaint();
      }
    });
  }

  public void updateItems() {
    repaint();
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting()) {
      return;
    }
    if (!mAllowEvents) {
      return;
    }
    final int column = mTable.getSelectedColumn();
    final int row= mTable.getSelectedRow();
    if (column >= 0 && row >= 0) {
      Date date = (Date) mTable.getValueAt(row, column);
      CalendarTableModel model = (CalendarTableModel)mTable.getModel();
      if (date != model.getCurrentDate()) {
        // filter out the duplicate events caused by listening to row and column selection changes
        if (column != mLastColumn || row != mLastRow) {
          markDate(date,true);

          mLastColumn = column;
          mLastRow = row;
        }
      }
    }
  }


}
