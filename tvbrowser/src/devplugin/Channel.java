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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */


 /**
  * TV-Browser
  * @author Martin Oberhauser
  */

package devplugin;

/**
 * This interface provides a view of the channel object, implemented in the
 * host-application.
 */

public interface Channel {

  /**
   * Returns the channel position (ranking by the user) or -1, if the channel
   * is not subscribed.
   */
    public int getPos();

    /**
     * Returns the name of the channel.
     */
    public String getName();

    /**
     * Returns a unique ID of the channel
     */
    public int getId();

    public boolean equals(Channel ch);

    /**
     * Returns true, if the channel is subscribed
     */
    public boolean isSubscribed();
}