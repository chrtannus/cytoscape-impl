package org.cytoscape.application.internal;

/*
 * #%L
 * Cytoscape Application Impl (application-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2008 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.util.Properties;

import org.cytoscape.application.NetworkViewRenderer;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;


public class CyActivator extends AbstractCyActivator {
	
	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		CyEventHelper cyEventHelperServiceRef = getService(bc,CyEventHelper.class);
		CyNetworkManager cyNetworkManagerServiceRef = getService(bc,CyNetworkManager.class);
		CyNetworkViewManager cyNetworkViewManagerServiceRef = getService(bc,CyNetworkViewManager.class);
		
		CyApplicationManagerImpl cyApplicationManager = new CyApplicationManagerImpl(cyEventHelperServiceRef,cyNetworkManagerServiceRef,cyNetworkViewManagerServiceRef);
		Bundle rootBundle = bc.getBundle(0);
		ShutdownHandler cytoscapeShutdown = new ShutdownHandler(cyEventHelperServiceRef, rootBundle);
		CyApplicationConfigurationImpl cyApplicationConfiguration = new CyApplicationConfigurationImpl();
		CyProperty<Properties> cyApplicationCoreProperty = getService(bc,CyProperty.class,"(cyPropertyName=cytoscape3.props)");
		CyVersionImpl cytoscapeVersion = new CyVersionImpl(cyApplicationCoreProperty);
		
		registerAllServices(bc,cyApplicationManager, new Properties());
		registerAllServices(bc,cytoscapeShutdown, new Properties());
		registerAllServices(bc,cytoscapeVersion, new Properties());
		registerAllServices(bc,cyApplicationConfiguration, new Properties());
		
		registerServiceListener(bc, cyApplicationManager, "addNetworkViewRenderer", "removeNetworkViewRenderer", NetworkViewRenderer.class);
		DefaultNetworkViewFactory viewFactory = new DefaultNetworkViewFactory(cyApplicationManager);
		Properties viewFactoryProperties = new Properties();
		viewFactoryProperties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
		registerService(bc, viewFactory, CyNetworkViewFactory.class, viewFactoryProperties);
	}
}
