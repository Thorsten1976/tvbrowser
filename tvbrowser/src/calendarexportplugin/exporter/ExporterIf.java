/*
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
 *     $Date: 2005-12-26 21:46:18 +0100 (Mo, 26 Dez 2005) $
 *   $Author: troggan $
 * $Revision: 1764 $
 */
package calendarexportplugin.exporter;

import devplugin.Program;

import java.util.Properties;

/**
 * This interface must be implemented for all export-methods 
 * 
 * @author bodum
 */
public interface ExporterIf {

  /**
   * Export programs 
   * 
   * @param programs this programs
   * @param settings the settings
   * @return true, if successfully
   */
  public boolean exportPrograms(Program[] programs, Properties settings);
  
  /**
   * @return true, if this exporter has a settings-dialog
   */
  public boolean hasSettingsDialog();
  
  /**
   * Show settings-dialog
   * @param settings Show Dialog with this settings 
   */
  public void showSettingsDialog(Properties settings);
  
  /**
   * @return Name of Exporter
   */
  public String getName();  
}
