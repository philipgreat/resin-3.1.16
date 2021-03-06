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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.cluster;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Base class for the distributed objects
 */
public interface ObjectManager {
  /**
   * Returns the maximum idle time.
   */
  public long getMaxIdleTime();
  
  /**
   * Loads the object from the input stream.
   */
  public void load(InputStream in, Object object)
    throws IOException;
  
  /**
   * Returns true if the object is empty.
   */
  public boolean isEmpty(Object object)
    throws IOException;
  
  /**
   * Stores the object in the output stream.
   */
  public void store(OutputStream out, Object object)
    throws IOException;

  /**
   * Notifies that an object is updated.
   */
  public void notifyUpdate(String objectId)
    throws IOException;

  /**
   * Notifies an object has been removed.
   */
  public void notifyRemove(String objectId)
    throws IOException;
}
