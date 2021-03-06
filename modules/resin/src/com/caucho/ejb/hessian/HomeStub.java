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

package com.caucho.ejb.hessian;

import javax.ejb.EJBHome;
import javax.ejb.EJBMetaData;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.RemoveException;
import java.rmi.RemoteException;

/**
 * Base class for home stubs.
 */
abstract public class HomeStub extends HessianStub implements EJBHome {
  private transient EJBMetaData _metaData;

  abstract public String getHessianType();
  
  /**
   * Returns the stub's home handle
   */
  public HomeHandle getHomeHandle() throws RemoteException
  {
    return _client.getHomeHandle();
  }

  public EJBMetaData getEJBMetaData() throws RemoteException
  {
    if (_metaData == null)
      _metaData = _ejb_getEJBMetaData();
    
    return _metaData;
  }

  /**
   * Remove the object specified by the handle from the server.
   *
   * @param handle the handle to the object to remove
   */
  public void remove(Handle handle) throws RemoteException, RemoveException
  {
    _ejb_remove(handle);
  }

  /**
   * Remove the object specified by the public key from the server.
   *
   * @param publicKey the public key of the object to remove
   */
  public void remove(Object publicKey) throws RemoteException, RemoveException
  {
    _ejb_remove(publicKey);
  }

  protected EJBMetaData _ejb_getEJBMetaData() throws RemoteException
  {
    throw new UnsupportedOperationException();
  }
  
  protected void _ejb_remove(Handle handle)
    throws RemoteException, RemoveException
  {
    throw new UnsupportedOperationException();
  }
  
  protected void _ejb_remove(Object primaryKey)
    throws RemoteException, RemoveException
  {
    throw new UnsupportedOperationException();
  }
}
