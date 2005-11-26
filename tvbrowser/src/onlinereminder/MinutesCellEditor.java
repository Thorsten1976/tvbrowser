package onlinereminder;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class MinutesCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
  JComboBox mComboBox;
  
  protected static final String EDIT = "edit";

  public MinutesCellEditor() {
    mComboBox = new JComboBox(ReminderValues.getAllValues());
    mComboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
  }

  public void setValue(Object value) {
    mComboBox.setSelectedIndex(ReminderValues.getValueForMinutes(((Integer) value).intValue()));
  }
  
  public boolean stopCellEditing() {
    if (mComboBox.isEditable()) {
      // Commit edited value.
      mComboBox.actionPerformed(new ActionEvent(this, 0, ""));
    }
    return super.stopCellEditing();
  }

  public void actionPerformed(ActionEvent e) {
    stopCellEditing();
  }

  // Implement the one CellEditor method that AbstractCellEditor doesn't.
  public Object getCellEditorValue() {
    return new Integer(ReminderValues.getMinutesForValue(mComboBox.getSelectedIndex()));
  }

  // Implement the one method defined by TableCellEditor.
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    setValue(value);
    return mComboBox;
  }
}