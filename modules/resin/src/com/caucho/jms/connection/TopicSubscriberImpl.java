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

import com.caucho.jms.message.*;
import com.caucho.jms.queue.*;
import com.caucho.jms.selector.Selector;
import com.caucho.jms.selector.SelectorParser;
import com.caucho.log.Log;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.AlarmListener;

import javax.jms.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A basic topic subscriber
 */
public class TopicSubscriberImpl extends MessageConsumerImpl
  implements TopicSubscriber
{
  private static final Logger log
    = Logger.getLogger(TopicSubscriberImpl.class.getName());
  private static final L10N L = new L10N(TopicSubscriberImpl.class);

  private AbstractTopic _topic;
  private AbstractQueue _subscription;
  
  TopicSubscriberImpl(JmsSession session,
		      AbstractTopic topic,
		      String messageSelector,
		      boolean noLocal)
    throws JMSException
  {
    super(session, topic.createSubscriber(session, messageSelector, noLocal),
          messageSelector, noLocal);

    _topic = topic;
    _subscription = (AbstractQueue) getDestination();
  }
  
  TopicSubscriberImpl(JmsSession session,
		      AbstractTopic topic,
		      AbstractQueue subscription,
		      String messageSelector,
		      boolean noLocal)
    throws JMSException
  {
    super(session, subscription, messageSelector, noLocal);

    _topic = topic;
    _subscription = subscription;
  }

  public Topic getTopic()
    throws JMSException
  {
    return _topic;
  }

  @Override
  public void close()
  {
    AbstractQueue subscription = _subscription;
    _subscription = null;

    if (subscription != null) {
      _topic.closeSubscriber(subscription);

      subscription.close();
    }
  }
}

