/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package javax.servlet.jsp.jstl.core;

import javax.el.*;

public final class StringTokenValueExpression extends ValueExpression {
  private Integer _i;
  private ValueExpression _orig;

  public StringTokenValueExpression(ValueExpression orig, Integer i)
  {
    _orig = orig;
    _i = i;
  }

  @Override
  public String getExpressionString()
  {
    return _orig.getExpressionString() + "[" + _i + "]";
  }

  @Override
  public Class getExpectedType()
  {
    return String.class;
  }

  @Override
  public Class getType(ELContext context)
  {
    return String.class;
  }

  @Override
  public boolean isLiteralText()
  {
    return false;
  }

  @Override
  public boolean isReadOnly(ELContext context)
  {
    return true;
  }

  @Override
  public Object getValue(ELContext context)
  {
    Object base = _orig.getValue(context);

    if (! (base instanceof String))
      return null;

    String str = (String) base;

    int i = _i;
    int begin = 0;
    int length = str.length();
    int tail;

    while (i-- >= 0 && (tail = str.indexOf(',', begin)) >= 0) {
      if (i == -1)
        return str.substring(begin, tail);

      begin = tail + 1;
    }

    if (i == -1)
      return str.substring(begin, length);
    else
      return null;
  }

  @Override
  public void setValue(ELContext context, Object value)
  {
  }

  public int hashCode()
  {
    return 65521 * _orig.hashCode() + _i.hashCode();
  }

  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    else if (! (obj instanceof StringTokenValueExpression))
      return false;

    StringTokenValueExpression expr = (StringTokenValueExpression) obj;

    return _orig.equals(expr._orig) && _i.equals(expr._i);    
  }
}
