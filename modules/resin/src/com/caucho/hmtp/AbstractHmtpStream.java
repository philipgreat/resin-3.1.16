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

package com.caucho.hmtp;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;

/**
 * Configuration for a service
 */
public class AbstractHmtpStream implements HmtpStream
{
  private static final Logger log
    = Logger.getLogger(AbstractHmtpStream.class.getName());
  
  /**
   * Callback to handle messages
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void sendMessage(String to, String from, Serializable value)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendMessage to=" + to + " from=" + from
		+ " value=" + value);
    }
  }
  
  /**
   * Callback to handle messages
   * 
   * @param to the target JID
   * @param from the source JID
   * @param value the message payload
   */
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       HmtpError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendMessageError to=" + to + " from=" + from
		+ " error=" + error);
    }
  }
  
  public boolean sendQueryGet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendQueryGet id=" + id
		+ " to=" + to + " from=" + from
		+ " query=" + query);
    }
    
    return false;
  }
  
  public boolean sendQuerySet(long id,
			    String to,
			    String from,
			    Serializable query)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendQuerySet id=" + id
		+ " to=" + to + " from=" + from
		+ " query=" + query);
    }
    
    return false;
  }
  
  public void sendQueryResult(long id,
			    String to,
			    String from,
			    Serializable value)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendQueryResult id=" + id
		+ " to=" + to + " from=" + from
		+ " value=" + value);
    }
  }
  
  public void sendQueryError(long id,
			     String to,
			     String from,
			     Serializable query,
			     HmtpError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendQueryError id=" + id
		+ " to=" + to + " from=" + from
		+ " error=" + error);
    }
  }
  
  /**
   * General presence, for clients announcing availability
   */
  public void sendPresence(String to,
			 String from,
			 Serializable []data)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendPresence to=" + to + " from=" + from);
    }
  }

  /**
   * General presence, for clients announcing unavailability
   */
  public void sendPresenceUnavailable(String to,
				    String from,
				    Serializable []data)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendPresenceUnavailable to=" + to + " from=" + from);
    }
  }

  /**
   * Presence probe from the server to a client
   */
  public void sendPresenceProbe(String to,
				String from,
				Serializable []data)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendPresenceProbe to=" + to + " from=" + from);
    }
  }

  /**
   * A subscription request from a client
   */
  public void sendPresenceSubscribe(String to,
				  String from,
				  Serializable []data)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendPresenceSubscribe to=" + to + " from=" + from);
    }
  }

  /**
   * A subscription response to a client
   */
  public void sendPresenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendPresenceSubscribed to=" + to + " from=" + from);
    }
  }

  /**
   * An unsubscription request from a client
   */
  public void sendPresenceUnsubscribe(String to,
				    String from,
				    Serializable []data)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendPresenceUnsubscribe to=" + to + " from=" + from);
    }
  }

  /**
   * A unsubscription response to a client
   */
  public void sendPresenceUnsubscribed(String to,
				     String from,
				     Serializable []data)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendPresenceUnsubscribed to=" + to + " from=" + from);
    }
  }

  /**
   * An error response to a client
   */
  public void sendPresenceError(String to,
			      String from,
			      Serializable []data,
			      HmtpError error)
  {
    if (log.isLoggable(Level.FINER)) {
      log.finer(this + " sendPresenceError to=" + to + " from=" + from
		+ " error=" + error);
    }
  }
}
