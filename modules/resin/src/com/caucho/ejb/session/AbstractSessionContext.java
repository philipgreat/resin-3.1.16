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

package com.caucho.ejb.session;

import com.caucho.config.*;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.xa.*;
import com.caucho.util.*;

import javax.ejb.*;
import javax.xml.rpc.handler.MessageContext;

/**
 * Abstract base class for an session context
 */
abstract public class AbstractSessionContext extends AbstractContext
  implements SessionContext
{
  private static final L10N L = new L10N(AbstractSessionContext.class);
  
  /**
   * Returns the EJBHome stub for the container.
   */
  public EJBHome getEJBHome()
  {
    throw new EJBException(L.l("EJBHome does not exist for this class"));
  }
  
  /**
   * Returns the EJBLocalHome stub for the container.
   */
  public EJBLocalHome getEJBLocalHome()
  {
    throw new EJBException(L.l("EJBLocalHome does not exist for this class"));
  }
  
  public <T> T getBusinessObject(Class<T> type)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Obsolete jaxrpc
   */
  public MessageContext getMessageContext()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
}
