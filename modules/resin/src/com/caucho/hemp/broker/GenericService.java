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

import com.caucho.hmtp.spi.HmtpBroker;
import com.caucho.hmtp.disco.DiscoInfoQuery;
import com.caucho.hmtp.disco.DiscoIdentity;
import com.caucho.hmtp.disco.DiscoFeature;
import com.caucho.hmtp.HmtpStream;
import com.caucho.hmtp.HmtpConnection;
import com.caucho.config.*;
import com.caucho.hmtp.AbstractHmtpAgentStream;
import com.caucho.hmtp.HmtpAgentStream;
import com.caucho.hmtp.spi.HmtpService;
import com.caucho.util.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.*;
import javax.annotation.*;
import javax.webbeans.*;

/**
 * GenericService implementation to simplify configuring a service.
 */
public class GenericService extends AbstractHmtpAgentStream
  implements HmtpService
{
  private static final L10N L = new L10N(GenericService.class);
  private static final Logger log
    = Logger.getLogger(GenericService.class.getName());
  
  private @In HmtpBroker _broker;
  
  private String _jid;
  
  private HmtpConnection _conn;
  private HmtpStream _brokerStream;

  private HmtpAgentStream _agentStream;
  
  public void setName(String name)
  {
    setJid(name);
  }

  public void setJid(String jid)
  {
    _jid = jid;
  }
  
  /**
   * Returns the service's jid.
   */
  public String getJid()
  {
    return _jid;
  }

  public void setBroker(HmtpBroker broker)
  {
    _broker = broker;
  }

  protected HmtpBroker getBroker()
  {
    return _broker;
  }
  
  protected HmtpConnection getConnection()
  {
    return _conn;
  }

  public HmtpStream getBrokerStream()
  {
    return _brokerStream;
  }

  @PostConstruct
  public void init()
  {
    if (getJid() == null)
      throw new ConfigException(L.l("{0} requires a jid",
				    getClass().getSimpleName()));

    _agentStream = createQueue(this);

    _broker.addService(this);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " init");

    _brokerStream = _broker.getBrokerStream();
  }

  protected HmtpAgentStream createQueue(HmtpAgentStream stream)
  {
    return new HempMemoryQueue(stream, _broker.getBrokerStream());
  }

  public HmtpAgentStream getAgentStream()
  {
    return _agentStream;
  }
  
  //
  // HmtpService API
  //
  
  /**
   * Create a filter for requests sent to the service's agent.
   */
  public HmtpAgentStream getAgentFilter(HmtpAgentStream agentStream)
  {
    return agentStream;
  }
  
  /**
   * Create a filter for requests sent by the service to the broker.
   */
  public HmtpStream getBrokerFilter(HmtpStream brokerStream)
  {
    return brokerStream;
  }
  
  /**
   * Callback when a child agent logs in.
   */
  public void onAgentStart(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onAgentStart(" + jid + ")");
  }
  
  /**
   * Callback when a child agent logs out.
   */
  public void onAgentStop(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " onAgentStop(" + jid + ")");
  }
  
  /**
   * Returns a child agent given a jid.
   */
  public HmtpAgentStream findAgent(String jid)
  {
    if (log.isLoggable(Level.FINER))
      log.finer(this + " findAgent(" + jid + ")");
    
    return null;
  }
  
  //
  // queries
  //

  @Override
  public boolean sendQueryGet(long id, String to, String from,
			      Serializable value)
  {
    if (value instanceof DiscoInfoQuery) {
      _brokerStream.sendQueryResult(id, from, to,
				new DiscoInfoQuery(getDiscoIdentity(),
						   getDiscoFeatures()));

      return true;
    }

    Serializable result = doQueryGet(to, from, value);

    if (result != null) {
      _brokerStream.sendQueryResult(id, from, to, result);
      return true;
    }

    return false;
  }

  protected Serializable doQueryGet(String to, String from, Serializable value)
  {
    return null;
  }

  protected Serializable doQuerySet(String to, String from, Serializable value)
  {
    return null;
  }

  /**
   * Returns the disco identity of the resource
   */
  protected DiscoIdentity []getDiscoIdentity()
  {
    return new DiscoIdentity[] {
      new DiscoIdentity(getDiscoCategory(), getDiscoType()),
    };
  }

  /**
   * Returns the disco features of the resource
   */
  protected DiscoFeature []getDiscoFeatures()
  {
    ArrayList<String> featureNames = new ArrayList<String>();

    getDiscoFeatureNames(featureNames);

    DiscoFeature []features = new DiscoFeature[featureNames.size()];

    for (int i = 0; i < featureNames.size(); i++) {
      features[i] = new DiscoFeature(featureNames.get(i));
    }

    return features;
  }

  protected String getDiscoCategory()
  {
    return "x-application";
  }

  protected String getDiscoType()
  {
    return getClass().getSimpleName();
  }

  protected void getDiscoFeatureNames(ArrayList<String> featureNames)
  {
    featureNames.add(DiscoInfoQuery.FEATURE);
  }

  @PreDestroy
  protected void destroy()
  {
    HmtpConnection conn = _conn;
    _conn = null;

    if (conn != null)
      conn.close();

    if (log.isLoggable(Level.FINE))
      log.fine(this + " destroy");
  }
}
