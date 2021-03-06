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

package com.caucho.server.http;

import com.caucho.server.connection.Connection;
import com.caucho.server.port.Protocol;
import com.caucho.server.port.ServerRequest;

/**
 * The main class for the HTTP server.
 *
 * <p>TcpServer handles the main thread control.  HttpServer just needs
 * to create the right kind of request when a new thread is spawned.
 *
 * @see com.caucho.server.TcpServer
 */
public class HttpProtocol extends Protocol {
  static final String COPYRIGHT =
    "Copyright (c) 1998-2008 Caucho Technology.  All rights reserved.";

  private String _protocolName = "http";

  /**
   * Returns the protocol name.
   */
  public String getProtocolName()
  {
    return _protocolName;
  }
  
  /**
   * Sets the protocol name.
   */
  public void setProtocolName(String name)
  {
    _protocolName = name;
  }

  /**
   * Create a HttpRequest object for the new thread.
   */
  public ServerRequest createRequest(Connection conn)
  {
    return new HttpRequest(getServer(), conn);
  }
}
