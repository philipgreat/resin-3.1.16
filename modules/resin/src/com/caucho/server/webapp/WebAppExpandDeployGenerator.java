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

package com.caucho.server.webapp;

import com.caucho.config.ConfigException;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentListener;
import com.caucho.log.Log;
import com.caucho.server.deploy.DeployContainer;
import com.caucho.server.deploy.ExpandDeployGenerator;
import com.caucho.vfs.CaseInsensitive;
import com.caucho.vfs.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The generator for the web-app deploy
 */
public class WebAppExpandDeployGenerator
  extends ExpandDeployGenerator<WebAppController>
  implements EnvironmentListener
{
  private static final Logger log
    = Logger.getLogger(WebAppExpandDeployGenerator.class.getName());

  private final WebAppExpandDeployGeneratorAdmin _admin;

  private WebAppContainer _container;

  private WebAppController _parent;

  private String _urlPrefix = "";

  private ArrayList<WebAppConfig> _webAppDefaults
    = new ArrayList<WebAppConfig>();

  private HashMap<Path,WebAppConfig> _webAppConfigMap
    = new HashMap<Path,WebAppConfig>();

  // Maps from the context-path to the webapps directory
  private HashMap<String,Path> _contextPathMap
    = new HashMap<String,Path>();

  private ClassLoader _parentLoader;

  /**
   * Creates the new expand deploy.
   */
  public WebAppExpandDeployGenerator(DeployContainer<WebAppController> container,
                                     WebAppContainer webAppContainer)
  {
    super(container, webAppContainer.getRootDirectory());

    _container = webAppContainer;

    _parentLoader = webAppContainer.getClassLoader();

    try {
      setExtension(".war");
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    _admin = new WebAppExpandDeployGeneratorAdmin(this);
  }

  /**
   * Gets the webApp container.
   */
  public WebAppContainer getContainer()
  {
    return _container;
  }

  /**
   * Sets the parent webApp.
   */
  public void setParent(WebAppController parent)
  {
    _parent = parent;
  }

  /**
   * Sets the parent loader.
   */
  public void setParentClassLoader(ClassLoader loader)
  {
    _parentLoader = loader;
  }

  /**
   * Sets the url prefix.
   */
  public void setURLPrefix(String prefix)
  {
    if (prefix.equals("")) {
    }

    while (prefix.endsWith("/"))
      prefix = prefix.substring(0, prefix.length() - 1);

    _urlPrefix = prefix;
  }

  /**
   * Gets the url prefix.
   */
  public String getURLPrefix()
  {
    return _urlPrefix;
  }

  /**
   * Sets true for a lazy-init.
   */
  public void setLazyInit(boolean lazyInit)
    throws ConfigException
  {
    log.config("lazy-init is deprecated.  Use <startup>lazy</startup> instead.");
    if (lazyInit)
      setStartupMode("lazy");
    else
      setStartupMode("automatic");
  }

  /**
   * Adds an overriding web-app
   */
  public void addWebApp(WebAppConfig config)
  {
    String docDir = config.getDocumentDirectory();

    Path appDir = getExpandDirectory().lookup(docDir);

    _webAppConfigMap.put(appDir, config);

    if (config.getContextPath() != null)
      _contextPathMap.put(config.getContextPath(), appDir);
  }

  /**
   * Adds a default.
   */
  public void addWebAppDefault(WebAppConfig config)
  {
    _webAppDefaults.add(config);
  }

  @Override
  protected void initImpl()
  {
    super.initImpl();
  }

  /**
   * Returns the log.
   */
  @Override
  protected Logger getLog()
  {
    return log;
  }

  /**
   * Returns the deployed keys.
   */
  @Override
  protected void fillDeployedKeys(Set<String> keys)
  {
    super.fillDeployedKeys(keys);

    for (WebAppConfig cfg : _webAppConfigMap.values()) {
      if (cfg.getContextPath() != null)
        keys.add(cfg.getContextPath());
    }
  }

  /**
   * Start the deploy.
   */
  @Override
  protected void startImpl()
  {
    super.startImpl();

    Environment.addEnvironmentListener(this, _parentLoader);

    _admin.register();
  }

  /**
   * Returns the new controller.
   */
  @Override
  protected WebAppController createController(String name)
  {
    if (! name.startsWith(_urlPrefix))
      return null;

    String segmentName = name.substring(_urlPrefix.length());

    Path webAppRoot = _contextPathMap.get(segmentName);

    if (webAppRoot != null)
      segmentName = "/" + webAppRoot.getTail();
    else if (segmentName.indexOf('/', 1) > 0)
      return null;

    String contextPath = segmentName;

    if (segmentName.equals("")) {
      if (CaseInsensitive.isCaseInsensitive())
        segmentName = "/root";
      else
        segmentName = "/ROOT";
    }

    // server/26cg
    if (contextPath.equals("/ROOT"))
      contextPath = "";
    else if (contextPath.equalsIgnoreCase("/root")
	     && CaseInsensitive.isCaseInsensitive())
      contextPath = "";

    ArrayList<String> versionNames = getVersionNames(segmentName);

    if (versionNames == null || versionNames.size() == 0) {
      return makeController(name,
			    _urlPrefix + contextPath,
			    _urlPrefix + segmentName);
    }

    WebAppController controller
      = new WebAppVersioningController(name, contextPath, this, _container);

    return controller;
  }

  private WebAppController makeController(String name,
					  String contextPath,
					  String versionName)
  {
    String version = "";
    String baseName = contextPath;

    if (isVersioning()) {
      int p = versionName.lastIndexOf('-');
      
      if (p > 0) {
	version = versionName.substring(p + 1);
	baseName = versionName.substring(0, p);
      }
    }

    int p = versionName.lastIndexOf('/');

    String segmentName = versionName.substring(p + 1);
    
    String expandName = getExpandName(segmentName);

    String archiveName = segmentName + ".war";
    Path jarPath = getArchiveDirectory().lookup("./" + archiveName);

    Path rootDirectory;

    if (jarPath.isDirectory()) {
      //rootDirectory = getExpandDirectory().lookup("./" + archiveName);
      // server/10kw
      rootDirectory = jarPath;
      jarPath = null;
    }
    else {
      // server/003j
      rootDirectory = getExpandDirectory().lookup("./" + expandName);
    }


    if (! rootDirectory.isDirectory()
        && (jarPath == null || ! jarPath.isFile()))
      return null;
    else if (rootDirectory.isDirectory()
             && ! isValidDirectory(rootDirectory, versionName.substring(1)))
      return null;

    WebAppConfig cfg = _webAppConfigMap.get(rootDirectory);

    if (cfg != null && cfg.getContextPath() != null) {
      baseName = contextPath = cfg.getContextPath();
    }

    WebAppController controller
      = new WebAppController(versionName, contextPath,
			     rootDirectory, _container);

    controller.setWarName(versionName.substring(1));

    controller.setParentWebApp(_parent);

    controller.setDynamicDeploy(true);
    controller.setSourceType("expand");

    controller.setVersion(version);

    if (! baseName.equals(contextPath)) {
      WebAppController versionController
	= _container.getWebAppGenerator().findController(baseName);
      
      if (versionController instanceof WebAppVersioningController) {
	((WebAppVersioningController) versionController).setModified(true);
      }
    }

    return controller;
  }

  /**
   * Returns the current array of webApp entries.
   */
  @Override
  protected WebAppController mergeController(WebAppController controller,
                                             String key)
  {
    try {
      Path expandDirectory = getExpandDirectory();
      Path rootDirectory = controller.getRootDirectory();

      if (! expandDirectory.equals(rootDirectory.getParent()))
        return controller;

      controller = super.mergeController(controller, key);

      if (controller.getArchivePath() == null) {
        String archiveName = rootDirectory.getTail() + ".war";

        Path jarPath = getArchiveDirectory().lookup(archiveName);

        if (! jarPath.isDirectory()) {
          controller.setArchivePath(jarPath);
          controller.addDepend(jarPath);
        }
      }

      controller.setStartupMode(getStartupMode());
      // controller.setRedeployMode(getRedeployMode());

      for (int i = 0; i < _webAppDefaults.size(); i++)
        controller.addConfigDefault(_webAppDefaults.get(i));

      WebAppConfig cfg = _webAppConfigMap.get(rootDirectory);

      if (cfg != null)
        controller.addConfigDefault(cfg);
    } catch (ConfigException e) {
      log.warning(e.toString());
      log.log(Level.FINEST, e.toString(), e);
      controller.setConfigException(e);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      controller.setConfigException(e);
    }

    return controller;
  }

  /**
   * Converts the name.
   */
  @Override
  protected String pathNameToEntryName(String name)
  {
    String entryName = super.pathNameToEntryName(name);

    if (entryName == null)
      return null;

    if (CaseInsensitive.isCaseInsensitive()) {
      try {
        String []list = getExpandDirectory().list();

        String matchName = null;

        for (int i = 0; i < list.length; i++) {
          if (list[i].equalsIgnoreCase(entryName))
            matchName = list[i];
        }

        if (matchName == null)
          matchName = entryName.toLowerCase();
      } catch (Exception e) {
        entryName = entryName.toLowerCase();
      }
    }

    if (entryName.equalsIgnoreCase("root"))
      return _urlPrefix;
    else
      return _urlPrefix + "/" + entryName;
  }

  @Override
  protected String entryNameToArchiveName(String entryName)
  {
    String prefix = _urlPrefix + "/";

    if (entryName.equals(_urlPrefix))
      return "ROOT" + getExtension();
    else if (entryName.startsWith(prefix))
      return entryName.substring(prefix.length()) + getExtension();
    else
      return null;
  }

  /**
   * Destroy the deployment.
   */
  @Override
  protected void destroyImpl()
  {
    _admin.unregister();

    _container.removeWebAppDeploy(this);

    Environment.removeEnvironmentListener(this, _parentLoader);

    super.destroyImpl();
  }
}
