/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.application;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import javax.faces.*;
import javax.faces.application.*;
import javax.faces.component.*;
import javax.faces.component.html.*;
import javax.faces.context.*;
import javax.faces.render.*;

import javax.servlet.*;

import com.caucho.util.*;
import com.caucho.hessian.io.*;

public class SessionStateManager extends StateManager
{
  private static final L10N L = new L10N(SessionStateManager.class);
  private static final Logger log
    = Logger.getLogger(SessionStateManager.class.getName());

  private static final IntMap _typeMap = new IntMap();
  private static final ArrayList<Class> _typeList = new ArrayList();
  
  @Override
  public Object saveView(FacesContext context)
  {
    UIViewRoot root = context.getViewRoot();

    if (root == null || root.isTransient())
      return null;

    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();

      Hessian2Output out = new Hessian2Output(bos);
      
      serialize(out, context, root, new HashSet<String>());

      out.close();

      byte []state = bos.toByteArray();

      if (log.isLoggable(Level.FINE)) {
	log.fine("JSF[" + root.getViewId() + "] serialize (" + state.length + " bytes)");

	if (log.isLoggable(Level.FINER))
	  debugState(state);
      }

      if (! isSavingStateInClient(context)) {
        Map sessionMap = context.getExternalContext().getSessionMap();

        Map viewMap = (Map) sessionMap.get("caucho.jsf.view");

        if (viewMap == null)
          viewMap = new HashMap();

        viewMap.put(root.getViewId(), state);

        sessionMap.put("caucho.jsf.view", viewMap);
        
        return "!";
      }

      return state;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Deprecated
  @Override
  public SerializedView saveSerializedView(FacesContext context)
  {
    return new SerializedView(saveView(context), null);
  }

  @Deprecated
  public void writeState(FacesContext context,
			 SerializedView state)
    throws IOException
  {
    Object [] stateArray = new Object [2];

    stateArray [0] = state.getStructure();
    stateArray [1] = state.getState();

    writeState(context, stateArray);
  }

  public void writeState(FacesContext context, Object state)
    throws IOException
  {
    ResponseStateManager rsm = context.getRenderKit().getResponseStateManager();

    if (! (state instanceof Object []))
      rsm.writeState(context, new Object []{state, null});
    else
      rsm.writeState(context, state);
  }

  @Override
  public UIViewRoot restoreView(FacesContext context,
				String viewId,
				String renderKitId)
  {

    RenderKit renderKit = context.getRenderKit();
    
    if (renderKit == null) {
      RenderKitFactory renderKitFactory
	= (RenderKitFactory) FactoryFinder.getFactory(
	FactoryFinder.RENDER_KIT_FACTORY);
      
      renderKit = renderKitFactory.getRenderKit(context, renderKitId);
    }
    
    ResponseStateManager rsm =
      renderKit.getResponseStateManager();

    Object state = rsm.getState(context, viewId);//sessionMap.get("caucho.jsf.view");

    if (!isSavingStateInClient(context) && "!".equals(((Object [])state) [0])) {
      Map viewMap = (Map) context.getExternalContext()
        .getSessionMap()
        .get("caucho.jsf.view");

      if (viewMap != null)
        state = viewMap.get(viewId);
    }

    if (state == null)
      return null;
    else if (state instanceof byte[])
      return restoreView(context, (byte []) state);
    if (state instanceof Object [])
      return restoreView(context, (byte []) ((Object []) state) [0]);
    else if (state instanceof StateManager.SerializedView) {
      StateManager.SerializedView serView
        = (StateManager.SerializedView) state;

      return restoreView(context, (byte[]) serView.getStructure());
    }
    else
      throw new IllegalStateException(L.l("unexpected saved state: '{0}'",
                                          state));
  }

  private void serialize(AbstractHessianOutput out,
			 FacesContext context,
			 UIComponent comp,
			 HashSet<String> idMap)
    throws IOException
  {
    if (comp.isTransient())
      return;
    
    if (idMap.contains(comp.getId()))
      throw new IllegalStateException(L.l("'{0}' is a duplicate component during serialization.",
					  comp.getId()));

    if (comp.getId() != null)
      idMap.add(comp.getId());

    if (comp instanceof NamingContainer)
      idMap = new HashSet<String>(8);

    int typeId = _typeMap.get(comp.getClass());
    out.writeInt(typeId);
    if (typeId <= 0)
      out.writeString(comp.getClass().getName());

    int fullChildCount = comp.getChildCount();
    if (fullChildCount > 0) {
      int childCount = 0;

      List<UIComponent> children = comp.getChildren();
      
      for (int i = 0; i < fullChildCount; i++) {
	UIComponent child = children.get(i);

	if (! child.isTransient())
	  childCount++;
      }

      out.writeInt(childCount);
    
      for (int i = 0; i < fullChildCount; i++) {
	UIComponent child = children.get(i);

	serialize(out, context, child, idMap);
      }
    }
    else
      out.writeInt(0);

    int facetCount = comp.getFacetCount();
    out.writeInt(facetCount);
    
    if (facetCount > 0) {
      for (Map.Entry<String,UIComponent> entry : comp.getFacets().entrySet()) {
	out.writeString(entry.getKey());

	serialize(out, context, entry.getValue(), idMap);
      }
    }

    out.writeObject(comp.saveState(context));
  }
    
  private UIViewRoot restoreView(FacesContext context,
				 byte []data)
  {
    if (data == null)
      return null;

    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(data);
      Hessian2Input in = new Hessian2Input(bis);

      return (UIViewRoot) deserialize(in, context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private UIComponent deserialize(AbstractHessianInput in,
				  FacesContext context)
    throws IOException,
	   ClassNotFoundException,
	   InstantiationException,
	   IllegalAccessException
  {
    int typeId = in.readInt();

    Class type;

    if (typeId > 0) {
      type = _typeList.get(typeId);
    }
    else {
      String typeName = in.readString();
      
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
	
      type = Class.forName(typeName, false, loader);
    }

    UIComponent comp = (UIComponent) type.newInstance();

    int childCount = in.readInt();
    for (int i = 0; i < childCount; i++) {
      comp.getChildren().add(deserialize(in, context));
    }

    int facetCount = in.readInt();

    for (int i = 0; i < facetCount; i++) {
      String key = in.readString();

      comp.getFacets().put(key, deserialize(in, context));
    }

    comp.restoreState(context, in.readObject());

    return comp;
  }
  
  public String toString()
  {
    return "SessionStateManager[]";
  }

  private static void addType(Class type)
  {
    if (_typeMap.get(type) > 0)
      return;
    
    _typeMap.put(type, _typeList.size());
    _typeList.add(type);
  }

  private void debugState(byte []state)
  {
    for (int i = 0; i < state.length; i++) {
      if (i != 0 && i % 40 == 0)
	System.out.println();

      int ch = state[i];

      if ('a' <= ch && ch <= 'z'
	  || 'A' <= ch && ch <= 'Z'
	  || '0' <= ch && ch <= '9'
	  || ch == ' ' || ch == '[' || ch == '.' || ch == ']'
	  || ch == '/' || ch == '\\'
	  || ch == '-' || ch == '_' || ch == '{' || ch == '}'
	  || ch == '#' || ch == '$' || ch == ':')
	System.out.print((char) ch);
      else {
	System.out.print("x"
			 + Integer.toHexString((ch / 16) & 0xf)
			 + Integer.toHexString(ch & 0xf));
      }
    }
    System.out.println();
  }

  static {
    _typeList.add(null);

    addType(UIColumn.class);
    addType(UICommand.class);
    addType(UIData.class);
    addType(UIForm.class);
    addType(UIGraphic.class);
    addType(UIInput.class);
    addType(UIMessage.class);
    addType(UIMessages.class);
    addType(UINamingContainer.class);
    addType(UIOutput.class);
    addType(UIPanel.class);
    addType(UIParameter.class);
    addType(UISelectBoolean.class);
    addType(UISelectItem.class);
    addType(UISelectItems.class);
    addType(UISelectMany.class);
    addType(UISelectOne.class);
    addType(UIViewRoot.class);
    
    addType(HtmlColumn.class);
    addType(HtmlCommandButton.class);
    addType(HtmlCommandLink.class);
    addType(HtmlDataTable.class);
    addType(HtmlForm.class);
    addType(HtmlGraphicImage.class);
    addType(HtmlInputHidden.class);
    addType(HtmlInputSecret.class);
    addType(HtmlInputText.class);
    addType(HtmlInputTextarea.class);
    addType(HtmlMessage.class);
    addType(HtmlMessages.class);
    addType(HtmlOutputFormat.class);
    addType(HtmlOutputLabel.class);
    addType(HtmlOutputLink.class);
    addType(HtmlOutputText.class);
    addType(HtmlPanelGrid.class);
    addType(HtmlPanelGroup.class);
    addType(HtmlSelectBooleanCheckbox.class);
    addType(HtmlSelectManyCheckbox.class);
    addType(HtmlSelectManyListbox.class);
    addType(HtmlSelectManyMenu.class);
    addType(HtmlSelectOneListbox.class);
    addType(HtmlSelectOneMenu.class);
    addType(HtmlSelectOneRadio.class);
  }
}
