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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.entity;

import com.caucho.amber.entity.EntityFactory;
import com.caucho.amber.entity.EntityItem;
import com.caucho.amber.manager.AmberConnection;
import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.ejb.FinderException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages the set of persistent beans.
 */
public class AmberEntityFactory extends EntityFactory {
  private static final L10N L = new L10N(AmberEntityFactory.class);
  private static final Logger log = Log.open(AmberEntityFactory.class);

  private EntityServer _entityServer;

  AmberEntityFactory(EntityServer entityServer)
  {
    _entityServer = entityServer;
  }

  /**
   * Gets the appropriate entity given the key.
   */
  public Object getEntity(Object key)
  {
    // ejb/061c
    try {
      return _entityServer.getContext(key, false, false).getEJBLocalObject();
    } catch (FinderException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the appropriate entity given the EntityItem.
   */
  public Object getEntity(AmberConnection aConn, EntityItem item)
  {
    try {
      Object key = item.getEntity().__caucho_getPrimaryKey();

      return _entityServer.getContext(key, false).getEJBLocalObject();
    } catch (FinderException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the appropriate entity given the EntityItem.
   */
  public Object getEntity(AmberConnection aConn,
                          EntityItem item,
                          Map preloadedProperties)
  {
    return getEntity(aConn, item);
  }

  /**
   * Gets the appropriate entity given the EntityItem.
   */
  public void delete(AmberConnection aConn, Object proxy)
  {
    try {
      EntityObject entity = (EntityObject) proxy;

      entity.remove();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
