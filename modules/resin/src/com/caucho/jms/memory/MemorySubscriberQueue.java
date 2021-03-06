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

package com.caucho.jms.memory;

import java.util.logging.*;

import javax.jms.*;

import com.caucho.jms.message.*;
import com.caucho.jms.queue.*;
import com.caucho.jms.connection.*;

/**
 * Implements a memory queue.
 */
public class MemorySubscriberQueue extends MemoryQueue
{
  private static final Logger log
    = Logger.getLogger(MemorySubscriberQueue.class.getName());
  
  private JmsSession _session;
  private boolean _isNoLocal;
  
  public MemorySubscriberQueue(JmsSession session, boolean noLocal)
  {
    _session = session;
    _isNoLocal = noLocal;
  }

  @Override
  public void send(JmsSession sendingSession, MessageImpl msg, long timeout)
  {
    if (_isNoLocal && _session == sendingSession)
      return;
    else {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " send message " + msg);
      
      super.send(sendingSession, msg, timeout);
    }
  }
}

