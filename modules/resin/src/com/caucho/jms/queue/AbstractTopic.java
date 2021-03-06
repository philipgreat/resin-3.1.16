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

package com.caucho.jms.queue;

import java.util.logging.*;

import javax.annotation.*;
import javax.jms.*;

import com.caucho.jms.message.*;
import com.caucho.jms.connection.*;

import com.caucho.util.L10N;

/**
 * Implements an abstract topic.
 */
abstract public class AbstractTopic extends AbstractDestination
  implements javax.jms.Topic
{
  private static final L10N L = new L10N(AbstractTopic.class);

  private TopicAdmin _admin;

  public void setTopicName(String name)
  {
    setName(name);
  }

  public void init()
  {
  }

  @PostConstruct
  public void postConstruct()
  {
    init();

    _admin = new TopicAdmin(this);
    _admin.register();
  }
  
  /**
   * Polls the next message from the store.  If no message is available,
   * wait for the timeout.
   */
  public MessageImpl receive(long timeout)
  {
    throw new java.lang.IllegalStateException(L.l("topic cannot be used directly for receive."));
  }

  public abstract AbstractQueue createSubscriber(JmsSession session,
                                                 String name,
                                                 boolean noLocal);

  public abstract void closeSubscriber(AbstractQueue subscriber);
}

