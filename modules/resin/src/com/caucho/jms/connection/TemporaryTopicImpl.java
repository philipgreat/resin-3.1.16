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

package com.caucho.jms.connection;

import com.caucho.jms.memory.MemoryTopic;

import com.caucho.jms.queue.MessageAvailableListener;
import com.caucho.util.L10N;
import java.util.ArrayList;
import javax.jms.*;

/**
 * A basic topic.
 */
public class TemporaryTopicImpl extends MemoryTopic implements TemporaryTopic
{
  private static final L10N L = new L10N(TemporaryTopicImpl.class);
  private static int _idCount;

  private JmsSession _session;

  private ArrayList<MessageAvailableListener> _consumerList
    = new ArrayList<MessageAvailableListener>();
  
  TemporaryTopicImpl(JmsSession session)
  {
    _session = session;
    
    setName("TemporaryTopic-" + _idCount++);
  }

  JmsSession getSession()
  {
    return _session;
  }

  @Override
  public void addMessageAvailableListener(MessageAvailableListener consumer)
  {
    if (! _consumerList.contains(consumer))
      _consumerList.add(consumer);
  }

  @Override
  public void removeMessageAvailableListener(MessageAvailableListener consumer)
  {
    _consumerList.remove(consumer);
  }

  public void delete()
    throws JMSException
  {
    if (_consumerList.size() > 0)
      throw new javax.jms.IllegalStateException(L.l("temporary topic is still active"));
  }
}

