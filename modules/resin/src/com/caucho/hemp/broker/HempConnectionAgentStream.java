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

package com.caucho.hemp.broker;

import com.caucho.hmtp.HmtpPresenceStream;
import com.caucho.hmtp.HmtpMessageStream;
import com.caucho.hmtp.HmtpQueryStream;
import com.caucho.hmtp.HmtpError;

import com.caucho.hemp.*;
import com.caucho.hmtp.HmtpAgentStream;
import com.caucho.util.*;
import java.io.Serializable;
import java.util.logging.*;

/**
 * Handles packets sent to the connection
 */
class HempConnectionAgentStream implements HmtpAgentStream
{
  private static final Logger log
    = Logger.getLogger(HempConnectionAgentStream.class.getName());
  
  private static final L10N L = new L10N(HempConnectionAgentStream.class);

  private final HempConnectionImpl _conn;
  private final String _jid;

  private HmtpMessageStream _messageHandler;
  private HmtpQueryStream _queryHandler;
  private HmtpPresenceStream _presenceHandler;

  HempConnectionAgentStream(HempConnectionImpl conn)
  {
    _conn = conn;
    _jid = conn.getJid();
  }
  
  /**
   * Returns the agent's jid
   */
  public String getJid()
  {
    return _jid;
  }

  //
  // handler registration
  //

  /**
   * Registers the listener
   */
  void setMessageHandler(HmtpMessageStream handler)
  {
    _messageHandler = handler;
  }

  /**
   * Registers the listener
   */
  public void setQueryHandler(HmtpQueryStream handler)
  {
    _queryHandler = handler;
  }

  /**
   * Sets the presence listener
   */
  public void setPresenceHandler(HmtpPresenceStream handler)
  {
    _presenceHandler = handler;
  }

  //
  // message handling
  //

  /**
   * Forwards the message
   */
  public void sendMessage(String to, String from, Serializable value)
  {
    HmtpMessageStream handler = _messageHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendMessage (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.sendMessage(to, from, value);
  }

  /**
   * Forwards the message
   */
  public void sendMessageError(String to,
			       String from,
			       Serializable value,
			       HmtpError error)
  {
    HmtpMessageStream handler = _messageHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendMessageError (no handler) to=" + to
		 + " from=" + from + " error=" + error);
      return;
    }
    
    handler.sendMessageError(to, from, value, error);
  }

  //
  // Query/RPC handling
  //

  /**
   * Forwards the message
   */
  public boolean sendQueryGet(long id,
			      String to,
			      String from,
			      Serializable query)
  {
    HmtpQueryStream handler = _queryHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendQueryGet (no handler) to=" + to + " from=" + from);
      return false;
    }
    
    if (handler.sendQueryGet(id, to, from, query))
      return true;

    HmtpError error = 
      new HmtpError(HmtpError.TYPE_CANCEL,
	            HmtpError.FEATURE_NOT_IMPLEMENTED,
		    "unknown query: " + query.getClass().getName());
    
     _conn.getBrokerStream().sendQueryError(id, from, to, query, error);
   
    return true;
  }

  /**
   * Forwards the message
   */
  public boolean sendQuerySet(long id,
		            String to,
		            String from,
		            Serializable query)
  {
    HmtpQueryStream handler = _queryHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendQuerySet (no handler) to=" + to + " from=" + from);
      return false;
    }
    
    if (handler.sendQuerySet(id, to, from, query))
      return true;

    HmtpError error =
      new HmtpError(HmtpError.TYPE_CANCEL,
		    HmtpError.FEATURE_NOT_IMPLEMENTED,
		    "unknown query: " + query.getClass().getName());
    
    _conn.getBrokerStream().sendQueryError(id, from, to, query, error);

    return true;
  }

  /**
   * Result from the message
   */
  public void sendQueryResult(long id,
		            String to,
		            String from,
		            Serializable value)
  {
    HmtpQueryStream handler = _queryHandler;

    if (handler == null) {
      if (_conn.onQueryResult(id, to, from, value))
	return;
      
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendQueryResult (no handler) to=" + to + " from=" + from);
      return;
    }

    handler.sendQueryResult(id, to, from, value);
  }

  /**
   * Error from the message
   */
  public void sendQueryError(long id,
		           String to,
		           String from,
		           Serializable query,
		           HmtpError error)
  {
    HmtpQueryStream handler = _queryHandler;

    if (handler == null) {
      if (_conn.onQueryError(id, to, from, query, error))
	return;
      
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendQueryError (no handler) to=" + to + " from=" + from);
      return;
    }

    handler.sendQueryError(id, to, from, query, error);
  }

  //
  // presence handling
  //

  /**
   * Forwards the presence
   */
  public void sendPresence(String to, String from, Serializable []data)
  {
    HmtpPresenceStream handler = _presenceHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresence (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.sendPresence(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void sendPresenceProbe(String to,
		              String from,
			      Serializable []data)
  {
    HmtpPresenceStream handler = _presenceHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceProbe (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.sendPresenceProbe(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void sendPresenceUnavailable(String to,
				    String from,
				    Serializable []data)
  {
    HmtpPresenceStream handler = _presenceHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceUnavailable (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.sendPresenceUnavailable(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void sendPresenceSubscribe(String to,
				  String from,
				  Serializable []data)
  {
    HmtpPresenceStream handler = _presenceHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceSubscribe (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.sendPresenceSubscribe(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void sendPresenceSubscribed(String to,
				   String from,
				   Serializable []data)
  {
    HmtpPresenceStream handler = _presenceHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceSubscribed (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.sendPresenceSubscribed(to, from, data);
  }
  
  /**
   * Forwards the presence
   */
  public void sendPresenceUnsubscribe(String to,
				    String from,
				    Serializable []data)
  {
    HmtpPresenceStream handler = _presenceHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceUnsubscribe (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.sendPresenceUnsubscribe(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void sendPresenceUnsubscribed(String to,
				     String from,
				     Serializable []data)
  {
    HmtpPresenceStream handler = _presenceHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceUnsubscribed (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.sendPresenceUnsubscribed(to, from, data);
  }

  /**
   * Forwards the presence
   */
  public void sendPresenceError(String to,
			      String from,
			      Serializable []data,
                              HmtpError error)
  {
    HmtpPresenceStream handler = _presenceHandler;

    if (handler == null) {
      if (log.isLoggable(Level.FINE))
	log.fine(this + " sendPresenceError (no handler) to=" + to + " from=" + from);
      return;
    }
    
    handler.sendPresenceError(to, from, data, error);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getJid() + "]";
  }
}
