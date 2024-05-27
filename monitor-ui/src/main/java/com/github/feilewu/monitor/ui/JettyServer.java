package com.github.feilewu.monitor.ui;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.feilewu.monitor.core.conf.MonitorConf;
import com.github.feilewu.monitor.core.deploy.master.Master;
import com.github.feilewu.monitor.core.ui.UIServer;
import com.github.feilewu.monitor.core.ui.UIServerException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import scala.Predef;

/**
 * @Author: pf_xu
 * @Date: 2024/5/22 22:35
 * @email：pfxuchn@gmail.com
 */
public class JettyServer implements UIServer {

    private Logger logger = LoggerFactory.getLogger(JettyServer.class);

    private Server server = null;

    private Master master;

    private MonitorConf conf;

    private boolean initialized = false;

    private int port = 8080;

    private String contextPath = "/";

    private String mappingUrl = "/**";

    public JettyServer() {
        server = new Server();
        server.setHandler(servletContextHandler(webApplicationContext()));
    }

    public JettyServer port(int port) {
        this.port = port;
        return this;
    }

    @Override
    public void init(MonitorConf conf, Master master) {
        this.conf = conf;
        this.master = master;
    }

    public void start() {
        try {
            Predef.require(initialized, () -> this.getClass().getName()+ "should be initialized.");
            server.start();
        } catch (Exception e) {
            throw new UIServerException(e.getMessage(), e);
        }
    }

    @Override
    public void stop() {

    }

    public void join() throws InterruptedException {
        if (server == null) {
            logger.warn("server instance is null, ignore join.");
        } else {
            server.join();
        }
    }

    private ServletContextHandler servletContextHandler(WebApplicationContext context) {
        ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath(contextPath);
        handler.addServlet(new ServletHolder(new DispatcherServlet(context)), mappingUrl);
        handler.addEventListener(new ContextLoaderListener(context));
        return handler;
    }

    private WebApplicationContext webApplicationContext() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(WebConfiguration.class);
        return context;
    }


}
