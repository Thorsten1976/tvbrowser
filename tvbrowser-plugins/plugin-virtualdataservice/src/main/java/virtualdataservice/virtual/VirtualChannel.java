/*
 * VirtualDataService by Reinhard Lehrbaum
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
 */
package virtualdataservice.virtual;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class VirtualChannel implements Externalizable, Comparable<VirtualChannel>
{
	private static final long serialVersionUID = 4801468619648124038L;

	private int mID;
	private String mName;
	private List<VirtualProgram> mPrograms;
	private String mWorkingDirectory;

	public VirtualChannel()
	{
		mID = -1;
	}

	public VirtualChannel(final int id, final String name)
	{
		mID = id;
		mName = name;
	}

	public void setWorkingDirectory(final String workingDirectory)
	{
		mWorkingDirectory = workingDirectory;
	}

	public int getID()
	{
		return mID;
	}

	public String getName()
	{
		return mName;
	}

	public void setName(final String name)
	{
		mName = name;
	}

	public List<VirtualProgram> getPrograms()
	{
		checkChannelState();
		return mPrograms;
	}

	public String toString()
	{
		return mName;// + " [" + mID + "]";
	}

	public void readExternal(final ObjectInput in) throws IOException,
      ClassNotFoundException
	{
	  final int version = in.readInt();
		if (version == 1)
		{
			mID = in.readInt();
			mName = (String) in.readObject();
		}
	}

	public void writeExternal(final ObjectOutput out) throws IOException
	{
		if (mID > -1)
		{
			out.writeInt(1); //version
			out.writeInt(mID);
			out.writeObject(mName);
		}
	}

	private String getFileName()
	{
		return "VC" + mID + ".dat";
	}

	public void load()
	{
		if (mID > -1)
		{
			mPrograms = new ArrayList<VirtualProgram>();

			final File file = new File(mWorkingDirectory + getFileName());
			if (file.exists())
			{
				try
				{
				  final ObjectInputStream in = new ObjectInputStream(
              new FileInputStream(file));

				  final int version = in.readInt();

					if (version == 1)
					{
					  final int count = in.readInt();
						for (int i = 0; i < count; i++)
						{
						  final VirtualProgram program = (VirtualProgram) in.readObject();
							addProgram(program);
						}
					}
					in.close();
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
	}

	public void save()
	{
		if (mPrograms != null && mID > -1)
		{
			try
			{
			  final ObjectOutputStream out = new ObjectOutputStream(
            new FileOutputStream(mWorkingDirectory + getFileName()));

				out.writeInt(1); //version
				out.writeInt(mPrograms.size());

				for (VirtualProgram program : mPrograms)
				{
					out.writeObject(program);
				}
				out.close();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	}

	private boolean checkProgram(final VirtualProgram p)
	{
		return p.isActive();
	}

	private void checkChannelState()
	{
		if (mID != -1)
		{
			if (mPrograms == null)
			{
				load();
			}
		}
		else
		{
			mPrograms = null;
		}
	}

	public void addProgram(final VirtualProgram program)
	{
		checkChannelState();
		if (checkProgram(program))
		{
			mPrograms.add(program);
		}
	}

	public void removeProgram(final VirtualProgram program)
	{
		checkChannelState();
		mPrograms.remove(program);
	}

	public int compareTo(final VirtualChannel o)
	{
	  final int compare = getName().compareToIgnoreCase(o.getName());
		if (compare == 0)
		{
			return getID() < o.getID() ? 1 : -1;
		}
		return compare;
	}
}
