package org.cytoscape.session;

import static org.cytoscape.model.CyNetwork.DEFAULT_ATTRS;
import static org.cytoscape.model.CyNetwork.HIDDEN_ATTRS;
import static org.cytoscape.model.CyNetwork.LOCAL_ATTRS;
import static org.cytoscape.model.CyNetwork.NAME;
import static org.cytoscape.model.CyNetwork.SELECTED;
import static org.cytoscape.model.subnetwork.CyRootNetwork.SHARED_ATTRS;
import static org.cytoscape.model.subnetwork.CyRootNetwork.SHARED_DEFAULT_ATTRS;
import static org.junit.Assert.*;

import java.io.File;

import org.cytoscape.group.CyGroup;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.SavePolicy;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.TaskIterator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class Cy283GroupsSessionLoadingTest extends BasicIntegrationTest {

	@Before
	public void setup() throws Exception {
		sessionFile = new File("./src/test/resources/testData/session2x/", "v283Groups.cys");
		checkBasicConfiguration();
	}

	@Test
	public void testLoadSession() throws Exception {
		final TaskIterator ti = openSessionTF.createTaskIterator(sessionFile);
		tm.execute(ti);
		confirm();
	}

	private void confirm() {
		checkGlobalStatus();
		checkNetwork();
		checkGroups();
	}
	
	private void checkGlobalStatus() {
		assertEquals(1, networkManager.getNetworkSet().size());
		assertEquals(1, viewManager.getNetworkViewSet().size());
		assertEquals(1, applicationManager.getSelectedNetworks().size());
		assertEquals(1, applicationManager.getSelectedNetworkViews().size());
		assertEquals(getNetworkByName("Network"), applicationManager.getCurrentNetwork());
		assertNotNull(applicationManager.getCurrentNetworkView());
		assertEquals("default", vmm.getDefaultVisualStyle().getTitle());
		assertEquals(9, vmm.getAllVisualStyles().size());
	}
	
	private void checkNetwork() {
		final CyNetwork net = applicationManager.getCurrentNetwork();
		assertEquals(SavePolicy.SESSION_FILE, net.getSavePolicy());
		checkNodeEdgeCount(applicationManager.getCurrentNetwork(), 4, 2, 0, 0);
		assertEquals("Nested Network Style", vmm.getVisualStyle(viewManager.getNetworkViews(net).iterator().next()).getTitle());
	}
	
	private void checkGroups() {
		final CyNetwork net = applicationManager.getCurrentNetwork();
		final CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
		
		// GROUP NODES
		final CyNode gn1 = getNodeByName(root, "Metanode 1");
		final CyNode gn2 = getNodeByName(root, "Metanode 2");
		final CyNode gn3 = getNodeByName(root, "Metanode 3");
		assertNotNull(gn1);
		assertNotNull(gn2);
		assertNotNull(gn3);
		assertTrue(groupManager.isGroup(gn1, gn2.getNetworkPointer())); // nested group
		assertTrue(groupManager.isGroup(gn2, net));
		assertTrue(groupManager.isGroup(gn3, net));
		
		assertEquals(2, groupManager.getGroupSet(net).size());
		assertEquals(1, groupManager.getGroupSet(gn2.getNetworkPointer()).size());
		
		final CyGroup g1 = groupManager.getGroup(gn1, gn2.getNetworkPointer());
		final CyGroup g2 = groupManager.getGroup(gn2, net);
		final CyGroup g3 = groupManager.getGroup(gn3, net);
		assertTrue(g1.isCollapsed(gn2.getNetworkPointer()));
		assertTrue(g2.isCollapsed(net));
		assertFalse(g3.isCollapsed(net)); // Expanded!
		
		// INTERNAL/EXTERNAL EDGES
		// Metanode 1 (nested group)
		CyEdge e1 = getEdgeByName(root, "node0 (DirectedEdge) node1");
		assertTrue(g1.getInternalEdgeList().contains(e1));
		assertEquals(1, g1.getInternalEdgeList().size());
		CyEdge e2 = getEdgeByName(root, "node1 (DirectedEdge) node2");
		assertTrue(g1.getExternalEdgeList().contains(e2));
		assertEquals(1, g1.getExternalEdgeList().size());
		// Metanode 2
		CyEdge e3 = getEdgeByName(root, "Metanode 1 (DirectedEdge) node3");
		assertTrue(g2.getInternalEdgeList().contains(e3));
		assertEquals(1, g2.getInternalEdgeList().size());
		CyEdge e4 = getEdgeByName(root, "Metanode 1 (meta-DirectedEdge) node2");
		assertTrue(g2.getExternalEdgeList().contains(e4));
		assertEquals(1, g2.getExternalEdgeList().size());
		// Metanode 3
		CyEdge e5 = getEdgeByName(root, "node4 (DirectedEdge) node5");
		assertTrue(g3.getInternalEdgeList().contains(e5));
		assertEquals(1, g3.getInternalEdgeList().size());
		assertEquals(0, g3.getExternalEdgeList().size()); // No external edges
		
		// GROUP NETWORKS
		assertEquals(gn1.getNetworkPointer(), g1.getGroupNetwork());
		assertEquals(gn2.getNetworkPointer(), g2.getGroupNetwork());
		assertEquals(gn3.getNetworkPointer(), g3.getGroupNetwork());
		// Make sure the group subnetworks have the correct save policy
		assertEquals(SavePolicy.SESSION_FILE, g1.getGroupNetwork().getSavePolicy());
		assertEquals(SavePolicy.SESSION_FILE, g2.getGroupNetwork().getSavePolicy());
		assertEquals(SavePolicy.SESSION_FILE, g3.getGroupNetwork().getSavePolicy());
		
		// TODO check group network attributes
		// TODO check meta-edge attributes
	}
}