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

package com.caucho.server.connection;

import java.io.IOException;

import com.caucho.vfs.*;

/**
 * Application handler for a bidirectional tcp stream
 *
 * The read and write callbacks are on different threads.
 * The ReadStream and WriteStream must not be passed to different
 * threads or stored in objects
 */
public interface TcpConnectionHandler
{
  /**
   * services a read packet
   *
   * @return true to continue the connection, false to kill it
   */
  public boolean serviceRead(ReadStream is,
			     TcpConnectionController controller)
    throws IOException;
  
  public boolean serviceWrite(WriteStream os,
			      TcpConnectionController controller)
    throws IOException;
}
