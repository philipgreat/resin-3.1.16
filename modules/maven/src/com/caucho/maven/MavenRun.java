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

package com.caucho.maven;

import com.caucho.resin.*;

import java.io.*;
import org.apache.maven.plugin.*;

/**
 * The MavenRun
 * @goal run
 */
public class MavenRun extends AbstractMojo
{
  private int _port = 8080;
  private String _contextPath = "/";
  private File _rootDirectory;

  /**
   * Sets the HTTP port that resin:run will listen to
   */
  public void setPort(int port)
  {
    _port = port;
  }

  /**
   * Sets the context-path (defaults to "/")
   */
  public void setContextPath(String contextPath)
  {
    _contextPath = contextPath;
  }

  /**
   * Sets the web-app's root directory
   */
  public void setRootDirectory(File rootDirectory)
  {
    _rootDirectory = rootDirectory;
  }

  /**
   * Executes the maven resin:run task
   */
  public void execute() throws MojoExecutionException
  {
    ResinEmbed resin = new ResinEmbed();

    HttpEmbed http = new HttpEmbed(_port);
    resin.addPort(http);

    WebAppEmbed webApp = new WebAppEmbed(_contextPath,
					 _rootDirectory.getAbsolutePath());

    resin.addWebApp(webApp);
    
    resin.start();
    try {
      resin.join();
    } finally {
      resin.destroy();
    }
  }
}
