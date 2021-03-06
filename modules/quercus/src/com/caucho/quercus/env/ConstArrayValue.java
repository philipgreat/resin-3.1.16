/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.quercus.env;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Represents a PHP array value.
 */
public class ConstArrayValue extends ArrayValueImpl {
  public ConstArrayValue()
  {
  }

  public ConstArrayValue(Value []keys, Value []values)
  {
    super(keys, values);
  }
  
  /**
   * Copy for assignment.
   */
  public Value copy()
  {
    return new CopyArrayValue(this);
  }
  
  /**
   * Shuffles the array
   */
  public void shuffle()
  {
    throw new IllegalStateException();
  }

  //
  // Java generator code
  //

  /**
   * Generates code to recreate the expression.
   *
   * @param out the writer to the Java source code.
   */
  public void generate(PrintWriter out)
    throws IOException
  {
    out.print("new ConstArrayValue(");
    
    out.print("new Value[] {");
      
    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      if (entry != getHead())
	out.print(", ");
	    
      if (entry.getKey() != null)
	entry.getKey().generate(out);
      else
	out.print("null");
    }
      
    out.print("}, new Value[] {");

    for (Entry entry = getHead(); entry != null; entry = entry._next) {
      if (entry != getHead())
	out.print(", ");
	    
      entry.getValue().generate(out);
    }

    out.print("})");
  }
}

