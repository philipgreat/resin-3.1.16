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
 * @author Rodrigo Westrupp
 */

package com.caucho.amber.cfg;

import javax.persistence.FetchType;


/**
 * The base class for <one-to-one>, <one-to-many> and so on.
 */
abstract public class AbstractRelationConfig {

  // attributes
  private String _name;
  private String _targetEntity;
  private FetchType _fetch;

  // elements
  private JoinTableConfig _joinTable;
  private CascadeConfig _cascade;

  public String getName()
  {
    return _name;
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getTargetEntity()
  {
    return _targetEntity;
  }

  public void setTargetEntity(String targetEntity)
  {
    _targetEntity = targetEntity;
  }

  public FetchType getFetch()
  {
    return _fetch;
  }

  public void setFetch(String fetch)
  {
    _fetch = FetchType.valueOf(fetch);
  }

  public CascadeConfig getCascade()
  {
    return _cascade;
  }

  public void setCascade(CascadeConfig cascade)
  {
    _cascade = cascade;
  }

  public JoinTableConfig getJoinTable()
  {
    return _joinTable;
  }

  public void setJoinTable(JoinTableConfig joinTable)
  {
    _joinTable = joinTable;
  }
}
