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

package com.caucho.ejb.cfg21;

import com.caucho.ejb.cfg.*;
import com.caucho.ejb.gen21.AmberIdGetter;
import com.caucho.ejb.gen21.ViewClass;
import com.caucho.java.gen.BaseMethod;
import com.caucho.java.gen.CallChain;
import com.caucho.util.L10N;

/**
 * Configuration for a method of a view.
 */
public class CmpIdGetter extends EjbMethod {
  private static final L10N L = new L10N(CmpIdGetter.class);

  /**
   * Creates a new method.
   *
   * @param view the owning view
   * @param apiMethod the method from the view
   * @param implMethod the method from the implementation
   */
  public CmpIdGetter(EjbView view, ApiMethod apiMethod, ApiMethod implMethod)
  {
    super(view, apiMethod, implMethod);
  }

  /**
   * Assembles the method.
   */
  public BaseMethod assemble(ViewClass viewAssembler, String fullClassName)
  {
    BaseMethod method = new AmberIdGetter(getApiMethod(),
					  getView().getImplClass().getName());

    CallChain call = method.getCall();
    if (call != null)
      method.setCall(assembleCallChain(call));
    
    return method;
  }
}
