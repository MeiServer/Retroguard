/* ===========================================================================
 * $RCSfile: Md.java,v $
 * ===========================================================================
 *
 * RetroGuard -- an obfuscation package for Java classfiles.
 *
 * Copyright (c) 1998-2006 Mark Welsh (markw@retrologic.com)
 *
 * This program can be redistributed and/or modified under the terms of the
 * Version 2 of the GNU General Public License as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */

package COM.rl.obf;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import COM.rl.util.*;
import COM.rl.obf.classfile.*;

/**
 * Tree item representing a method.
 * 
 * @author Mark Welsh
 */
public class Md extends MdFd
{
    // Constants -------------------------------------------------------------


    // Fields ----------------------------------------------------------------


    // Class Methods ---------------------------------------------------------


    // Instance Methods ------------------------------------------------------
    /**
     * Constructor
     * 
     * @param parent
     * @param isSynthetic
     * @param name
     * @param descriptor
     * @param access
     */
    public Md(TreeItem parent, boolean isSynthetic, String name, String descriptor, int access)
    {
        super(parent, isSynthetic, name, descriptor, access);
    }

    /**
     * Return the display name of the descriptor types.
     */
    @Override
    protected String getDescriptorName()
    {
        List<String> types = this.parseTypes();
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        if (types.size() > 0)
        {
            for (int i = 0; i < (types.size() - 1); i++)
            {
                sb.append(types.get(i));
                if (i < (types.size() - 2))
                {
                    sb.append(", ");
                }
            }
        }
        sb.append(");");
        return sb.toString();
    }

    /**
     * Does this method match the wildcard pattern? (compatibility mode)
     * 
     * @param namePattern
     * @param descPattern
     */
    public boolean isOldStyleMatch(String namePattern, String descPattern)
    {
        return this.isOldStyleMatch(namePattern) && TreeItem.isMatch(descPattern, this.getDescriptor());
    }
}
