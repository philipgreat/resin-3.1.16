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
* @author Emil Ong
*/

package com.caucho.xml.saaj;

import javax.xml.soap.*;
import javax.xml.transform.*;
import org.w3c.dom.*;
import java.util.*;

public class SimpleDocument implements Document
{
  private SOAPElementImpl _root;

  public SimpleDocument(SOAPElementImpl root)
  {
    _root = root;
    _root.setOwner(this);
  }

  // org.w3c.dom.Document

  public org.w3c.dom.Node adoptNode(org.w3c.dom.Node source)
  {
    throw new UnsupportedOperationException();
  }

  public Attr createAttribute(String name)
  {
    throw new UnsupportedOperationException();
  }

  public Attr createAttributeNS(String namespaceURI, String qualifiedName)
  {
    throw new UnsupportedOperationException();
  }

  public CDATASection	createCDATASection(String data)
  {
    throw new UnsupportedOperationException();
  }

  public Comment createComment(String data)
  {
    throw new UnsupportedOperationException();
  }

  public DocumentFragment createDocumentFragment()
  {
    throw new UnsupportedOperationException();
  }

  public Element createElement(String tagName)
  {
    throw new UnsupportedOperationException();
  }

  public Element createElementNS(String namespaceURI, String qualifiedName)
  {
    throw new UnsupportedOperationException();
  }

  public EntityReference createEntityReference(String name)
  {
    throw new UnsupportedOperationException();
  }

  public ProcessingInstruction createProcessingInstruction(String target, 
                                                           String data)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Text createTextNode(String data)
  {
    throw new UnsupportedOperationException();
  }

  public DocumentType getDoctype()
  {
    throw new UnsupportedOperationException();
  }

  public Element getDocumentElement()
  {
    return _root;
  }

  public String getDocumentURI()
  {
    throw new UnsupportedOperationException();
  }

  public DOMConfiguration getDomConfig()
  {
    throw new UnsupportedOperationException();
  }

  public Element getElementById(String elementId)
  {
    throw new UnsupportedOperationException();
  }

  public NodeList getElementsByTagName(String tagname)
  {
    throw new UnsupportedOperationException();
  }

  public NodeList getElementsByTagNameNS(String namespaceURI, String localName)
  {
    throw new UnsupportedOperationException();
  }

  public DOMImplementation getImplementation()
  {
    throw new UnsupportedOperationException();
  }

  public String getInputEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public boolean getStrictErrorChecking()
  {
    throw new UnsupportedOperationException();
  }

  public String getXmlEncoding()
  {
    throw new UnsupportedOperationException();
  }

  public boolean getXmlStandalone()
  {
    throw new UnsupportedOperationException();
  }

  public String getXmlVersion()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node importNode(org.w3c.dom.Node importedNode, 
                                     boolean deep)
  {
    throw new UnsupportedOperationException();
  }

  public void normalizeDocument()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node renameNode(org.w3c.dom.Node n, 
                                     String namespaceURI, 
                                     String qualifiedName)
    throws DOMException
  {
    throw new UnsupportedOperationException();
  }

  public void setDocumentURI(String documentURI)
  {
    throw new UnsupportedOperationException();
  }

  public void setStrictErrorChecking(boolean strictErrorChecking)
  {
    throw new UnsupportedOperationException();
  }

  public void setXmlStandalone(boolean xmlStandalone)
  {
    throw new UnsupportedOperationException();
  }

  public void setXmlVersion(String xmlVersion)
  {
    throw new UnsupportedOperationException();
  }

  // org.w3c.dom.Node

  public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node cloneNode(boolean deep)
  {
    throw new UnsupportedOperationException();
  }

  public short compareDocumentPosition(org.w3c.dom.Node other)
  {
    throw new UnsupportedOperationException();
  }

  public NamedNodeMap getAttributes()
  {
    throw new UnsupportedOperationException();
  }

  public String getBaseURI()
  {
    throw new UnsupportedOperationException();
  }

  public NodeList getChildNodes()
  {
    throw new UnsupportedOperationException();
  }

  public Object getFeature(String feature, String version)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getFirstChild()
  {
    return _root;
  }

  public org.w3c.dom.Node getLastChild()
  {
    return _root;
  }

  public String getLocalName()
  {
    throw new UnsupportedOperationException();
  }

  public String getNamespaceURI()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getNextSibling()
  {
    throw new UnsupportedOperationException();
  }

  public String getNodeName()
  {
    return "#document";
  }

  public short getNodeType()
  {
    return DOCUMENT_NODE;
  }

  public String getNodeValue()
  {
    throw new UnsupportedOperationException();
  }

  public Document getOwnerDocument()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getParentNode()
  {
    throw new UnsupportedOperationException();
  }

  public String getPrefix()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node getPreviousSibling()
  {
    throw new UnsupportedOperationException();
  }

  public String getTextContent()
  {
    throw new UnsupportedOperationException();
  }

  public Object getUserData(String key)
  {
    throw new UnsupportedOperationException();
  }

  public boolean hasAttributes()
  {
    throw new UnsupportedOperationException();
  }

  public boolean hasChildNodes()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node insertBefore(org.w3c.dom.Node newChild, 
                                       org.w3c.dom.Node refChild)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isDefaultNamespace(String namespaceURI)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isEqualNode(org.w3c.dom.Node arg)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isSameNode(org.w3c.dom.Node other)
  {
    throw new UnsupportedOperationException();
  }

  public boolean isSupported(String feature, String version)
  {
    throw new UnsupportedOperationException();
  }

  public String lookupNamespaceURI(String prefix)
  {
    throw new UnsupportedOperationException();
  }

  public String lookupPrefix(String namespaceURI)
  {
    throw new UnsupportedOperationException();
  }

  public void normalize()
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node removeChild(org.w3c.dom.Node oldChild)
  {
    throw new UnsupportedOperationException();
  }

  public org.w3c.dom.Node replaceChild(org.w3c.dom.Node newChild, 
                                       org.w3c.dom.Node oldChild)
  {
    throw new UnsupportedOperationException();
  }

  public void setNodeValue(String nodeValue)
  {
    throw new UnsupportedOperationException();
  }

  public void setPrefix(String prefix)
  {
    throw new UnsupportedOperationException();
  }

  public void setTextContent(String textContent)
  {
    throw new UnsupportedOperationException();
  }

  public Object setUserData(String key, Object data, UserDataHandler handler)
  {
    throw new UnsupportedOperationException();
  }
}
