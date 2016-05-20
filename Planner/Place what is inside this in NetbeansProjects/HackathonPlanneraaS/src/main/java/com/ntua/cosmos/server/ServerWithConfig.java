/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ntua.cosmos.server;

import com.ntua.cosmos.hackathonplanneraas.InterfacesOfPlanner;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;


/**
 *
 * @author PANAGIOTIS BOURELOS NTUA PhD Candidate
 */
public class ServerWithConfig {
    private final String USER_AGENT = "Mozilla/5.0";
    
    /**
     * Constructor for the ServerWithConfig Class that instantiates a server at port
     * 3030 and adds Servlet Handlers at specific URLs.
     */
    public ServerWithConfig() {
        try {
            //start custom config
            // Setup Threadpool
            QueuedThreadPool threadPool = new QueuedThreadPool(512);
            // Setup Jetty Server instance
            Server server = new Server(threadPool);
            server.manage(threadPool);
            server.setDumpAfterStart(false);
            server.setDumpBeforeStop(false);
            // Common HTTP configuration
            HttpConfiguration config = new HttpConfiguration();
            config.setSecurePort(8443);
            config.addCustomizer(new ForwardedRequestCustomizer());
            config.addCustomizer(new SecureRequestCustomizer());
            config.setSendServerVersion(true);
            // Http Connector Setup
            HttpConnectionFactory http = new HttpConnectionFactory(config);
            ServerConnector httpConnector = new ServerConnector(server,http);
            httpConnector.setPort(3030);
            httpConnector.setIdleTimeout(30000);
            server.addConnector(httpConnector);
            //end custom config
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setContextPath("/");
            server.setHandler(context);
            InterfacesOfPlanner intPlan = new InterfacesOfPlanner();
            context.addServlet(new ServletHolder(intPlan.new RetrieveSimilarCase()), "/sendcase/generic");
            context.addServlet(new ServletHolder(intPlan.new ArithmeticCaseSimilarity()), "/planner/similarity/arithmetic");
            server.start();
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
