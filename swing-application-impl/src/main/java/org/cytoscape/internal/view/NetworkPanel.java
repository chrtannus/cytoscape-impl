package org.cytoscape.internal.view;

/*
 * #%L
 * Cytoscape Swing Application Impl (swing-application-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.events.SetSelectedNetworksEvent;
import org.cytoscape.application.events.SetSelectedNetworksListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.internal.task.TaskFactoryTunableAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.task.DynamicTaskFactoryProvisioner;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewCollectionTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.edit.EditNetworkTitleTaskFactory;
import org.cytoscape.util.swing.JTreeTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkPanel extends JPanel implements TreeSelectionListener, SetSelectedNetworksListener,
		NetworkAddedListener, NetworkViewAddedListener, NetworkAboutToBeDestroyedListener,
		NetworkViewAboutToBeDestroyedListener, RowsSetListener, SetCurrentNetworkViewListener {

	private final static long serialVersionUID = 1213748836763243L;

	private static final Logger logger = LoggerFactory.getLogger(NetworkPanel.class);

	static final Color FONT_COLOR = new Color(20, 20, 20);
	static final Color SELECTION_BG_COLOR = new Color(0, 100, 255, 40);
	private static final int TABLE_ROW_HEIGHT = 16;
	private static final Dimension PANEL_SIZE = new Dimension(400, 700);

	private JSplitPane split1;
	private JSplitPane split2;
	private JTreeTable netTable;
	private JTable viewTable;
	private JPanel navigatorPanel;

	private NetworkTreeTableModel treeTableModel;
	private final NetworkTreeNode root;
	
	private final CyApplicationManager appMgr;
	final CyNetworkManager netMgr;
	private final CyNetworkViewManager netViewMgr;
	private final RenderingEngineManager renderingEngineMgr;
	private final DialogTaskManager taskMgr;
	private final DynamicTaskFactoryProvisioner factoryProvisioner;

	private final JPopupMenu netPopup;
	private final JPopupMenu viewPopup;
	private JMenuItem editRootNetworTitle;
	
	private final Map<TaskFactory, JMenuItem> popupMap;
	private final Map<TaskFactory, CyAction> popupActions;
	private HashMap<JMenuItem, Double> actionGravityMap;
	private final Map<Object, TaskFactory> provisionerMap;
	
	private final Map<CyTable, CyNetwork> nameTables;
	private final Map<CyTable, CyNetwork> nodeEdgeTables;
	private final Map<Long, NetworkTreeNode> treeNodeMap;
	private final Map<CyNetwork, NetworkTreeNode> network2nodeMap;

	private boolean ignoreTreeSelectionEvents;

	private final EditNetworkTitleTaskFactory rootNetworkTitleEditor;
	private final JPopupMenu rootPopupMenu;	
	private CyRootNetwork selectedRoot;
	private Set<CyRootNetwork> selectedRootSet;

	// Locale-specific sorting
	private final Collator collator;
	
	/**
	 * 
	 * @param appMgr
	 * @param netMgr
	 * @param netViewMgr
	 * @param bird
	 * @param taskMgr
	 */
	public NetworkPanel(final CyApplicationManager appMgr,
						final CyNetworkManager netMgr,
						final CyNetworkViewManager netViewMgr,
						final RenderingEngineManager renderingEngineMgr,
						final BirdsEyeViewHandler bird,
						final DialogTaskManager taskMgr,
						final DynamicTaskFactoryProvisioner factoryProvisioner,
						final EditNetworkTitleTaskFactory networkTitleEditor) {
		super();

		this.treeNodeMap = new HashMap<Long, NetworkTreeNode>();
		this.provisionerMap = new HashMap<Object, TaskFactory>();
		this.appMgr = appMgr;
		this.netMgr = netMgr;
		this.netViewMgr = netViewMgr;
		this.renderingEngineMgr = renderingEngineMgr;
		this.taskMgr = taskMgr;
		this.factoryProvisioner = factoryProvisioner;
		
		collator = Collator.getInstance(Locale.getDefault());
		collator.setStrength(Collator.PRIMARY);
		
		root = new NetworkTreeNode("Network Root", null);
		initialize();

		this.actionGravityMap = new HashMap<JMenuItem, Double>();
		
		// create and populate the popup window
		netPopup = new JPopupMenu();
		viewPopup = new JPopupMenu();
		popupMap = new WeakHashMap<TaskFactory, JMenuItem>();
		popupActions = new WeakHashMap<TaskFactory, CyAction>();
		nameTables = new WeakHashMap<CyTable, CyNetwork>();
		nodeEdgeTables = new WeakHashMap<CyTable, CyNetwork>();
		this.network2nodeMap = new WeakHashMap<CyNetwork, NetworkTreeNode>();
		
		setNavigator(bird.getBirdsEyeView());

		this.rootNetworkTitleEditor = networkTitleEditor;
		rootPopupMenu = new JPopupMenu();
		editRootNetworTitle = new JMenuItem("Rename Network Collection...");
		editRootNetworTitle.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				taskMgr.execute(rootNetworkTitleEditor.createTaskIterator(selectedRoot));
			}
		});
		rootPopupMenu.add(editRootNetworTitle);
		
		JMenuItem selectAllSubNetsMenuItem = new JMenuItem("Select All Networks");
		selectAllSubNetsMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				selectAllSubnetwork();
			}
		});
		rootPopupMenu.add(selectAllSubNetsMenuItem);
	}

	protected void initialize() {
		setLayout(new BorderLayout());
		setPreferredSize(PANEL_SIZE);
		setSize(PANEL_SIZE);

		add(getSplit1());
	}

	public Map<Long, Integer> getNetworkListOrder() {
		Map<Long, Integer> order = new HashMap<Long, Integer>();
		
		// Save the network orders
		final JTree tree = getNetTable().getTree();
		
		for (final Entry<CyNetwork, NetworkTreeNode> entry : network2nodeMap.entrySet()) {
			final CyNetwork net = entry.getKey();
			final NetworkTreeNode node = entry.getValue();
			
			if (node != null) {
				final TreePath tp = new TreePath(node.getPath());
				final int row = tree.getRowForPath(tp);
				order.put(net.getSUID(), row);
			}
		}
				
		return order;
	}
	
	public void setNetworkListOrder(final Map<Long, Integer> order) {
		if (order == null || order.size() < 2)
			return; // No need to sort 1 network
		
		// Restore the network collections order
		final Set<CyNetwork> networks = netMgr.getNetworkSet();
		final List<CyNetwork> sortedNetworks = new ArrayList<CyNetwork>(networks);
		
		Collections.sort(sortedNetworks, new Comparator<CyNetwork>() {
			@Override
			public int compare(final CyNetwork n1, final CyNetwork n2) {
				try {
					Integer o1 = order.get(n1.getSUID());
					Integer o2 = order.get(n2.getSUID());
					if (o1 == null) o1 = -1;
					if (o2 == null) o2 = -1;
					
					return o1.compareTo(o2);
				} catch (final Exception e) {
					logger.error("Cannot sort networks", e);
				}
				
				return 0;
			}
		});
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				nameTables.clear();
				nodeEdgeTables.clear();
				network2nodeMap.clear();
				treeNodeMap.clear();
				
				ignoreTreeSelectionEvents = true;
				root.removeAllChildren();
				getNetTable().getTree().updateUI();
				getNetTable().repaint();
				ignoreTreeSelectionEvents = false;
				
				for (final CyNetwork n : sortedNetworks)
					addNetwork(n);
					
				updateNetworkTreeSelection();
			}
		});
	}
	
	public void addTaskFactory(TaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		addFactory(factory, props, netPopup);
	}

	public void removeTaskFactory(TaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(factory);
	}

	public void addNetworkCollectionTaskFactory(NetworkCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props, netPopup);
	}

	public void removeNetworkCollectionTaskFactory(NetworkCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void addNetworkViewCollectionTaskFactory(NetworkViewCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props, viewPopup);
	}

	public void removeNetworkViewCollectionTaskFactory(NetworkViewCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void addNetworkTaskFactory(NetworkTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props, netPopup);
	}

	public void removeNetworkTaskFactory(NetworkTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void addNetworkViewTaskFactory(final NetworkViewTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props, viewPopup);
	}

	public void removeNetworkViewTaskFactory(NetworkViewTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void setNavigator(final Component comp) {
		getNavigatorPanel().removeAll();
		getNavigatorPanel().add(comp, BorderLayout.CENTER);
	}

	public JSplitPane getSplit1() {
		if (split1 == null) {
			split1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, getSplit2(), getNavigatorPanel());
			split1.setBorder(BorderFactory.createEmptyBorder());
			split1.setResizeWeight(1);
			split1.setDividerLocation(400);
		}
		
		return split1;
	}
	
	public JSplitPane getSplit2() {
		if (split2 == null) {
			final JScrollPane scroll1 = new JScrollPane(getNetTable());
			final JScrollPane scroll2 = new JScrollPane(getViewTable());
			
			split2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll1, scroll2);
			split2.setBorder(BorderFactory.createEmptyBorder());
			split2.setResizeWeight(1);
			split2.setDividerLocation(300);
		}
		
		return split2;
	}
	
	/**
	 * This is used by Session writer.
	 * @return
	 */
	private JTreeTable getNetTable() {
		if (netTable == null) {
			treeTableModel = new NetworkTreeTableModel(this, root);
			netTable = new JTreeTable(treeTableModel);
			
			/*
			 * Remove CTR-A for enabling select all function in the main window.
			 */
			for (KeyStroke listener : netTable.getRegisteredKeyStrokes()) {
				if (listener.toString().equals("ctrl pressed A")) {
					final InputMap map = netTable.getInputMap();
					map.remove(listener);
					netTable.setInputMap(WHEN_FOCUSED, map);
					netTable.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map);
				}
			}
			
			netTable.getTree().addTreeSelectionListener(this);
			netTable.getTree().setRootVisible(false);

			ToolTipManager.sharedInstance().registerComponent(netTable);

			netTable.getTree().setCellRenderer(new TreeCellRenderer(netTable));

			netTable.getColumn("Network").setPreferredWidth(250);
			netTable.getColumn("Nodes").setPreferredWidth(45);
			netTable.getColumn("Edges").setPreferredWidth(45);

			netTable.setBackground(Color.WHITE);
			netTable.setRowHeight(TABLE_ROW_HEIGHT);
			netTable.setForeground(FONT_COLOR);
//			netTable.setSelectionBackground(SELECTION_BG_COLOR);
			netTable.setSelectionForeground(FONT_COLOR);
			netTable.setCellSelectionEnabled(false);
			netTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			netTable.getTree().setSelectionModel(new DefaultTreeSelectionModel());
			
			// this mouse listener listens for the right-click event and will show
			// the pop-up window when that occurs
			netTable.addMouseListener(new NetPopupListener());
		}
		
		return netTable;
	}

	public JTable getViewTable() {
		if (viewTable == null) {
			final String[] columnNames = new String[]{ "Network View", "Engine" };
			final Class<?>[] columnClasses = new Class[]{ CyNetworkView.class, RenderingEngine.class };
			
			final TableModel model = new DefaultTableModel(columnNames, 0) {
				@Override
				public int getColumnCount() {
					return columnNames.length;
				}
				
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					return columnClasses[columnIndex];
				}
			};
			
			viewTable = new JTable(model);
			
			final ViewCellRenderer cellRenderer = new ViewCellRenderer();
			viewTable.setDefaultRenderer(CyNetworkView.class, cellRenderer);
			viewTable.setDefaultRenderer(RenderingEngine.class, cellRenderer);
			
			viewTable.getColumn("Network View").setPreferredWidth(250);
			
			viewTable.setRowHeight(TABLE_ROW_HEIGHT);
			viewTable.setBackground(Color.WHITE);
			viewTable.setForeground(FONT_COLOR);
			viewTable.setSelectionBackground(SELECTION_BG_COLOR);
			viewTable.setSelectionForeground(FONT_COLOR);
			viewTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			
			viewTable.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					if (SwingUtilities.isRightMouseButton(e)) {
						// Show context menu
						final Component parent = (Component) e.getSource();
						viewPopup.show(parent, e.getX(), e.getY());
					}
				}
			});
		}
		
		return viewTable;
	}
	
	private JPanel getNavigatorPanel() {
		if (navigatorPanel == null) {
			navigatorPanel = new JPanel();
			navigatorPanel.setLayout(new BorderLayout());
			navigatorPanel.setPreferredSize(new Dimension(280, 280));
			navigatorPanel.setSize(new Dimension(280, 280));
			navigatorPanel.setBackground(Color.WHITE);
		}
		
		return navigatorPanel;
	}

	// // Event handlers // //
	
	@Override
	public void handleEvent(final NetworkAboutToBeDestroyedEvent nde) {
		final CyNetwork net = nde.getNetwork();
		logger.debug("Network about to be destroyed: " + net);
		removeNetwork(net);
	}

	@Override
	public void handleEvent(final NetworkAddedEvent e) {
		final CyNetwork net = e.getNetwork();
		logger.debug("Network added: " + net);
		addNetwork(net);
	}

	@Override
	public void handleEvent(final RowsSetEvent e) {
		final Collection<RowSetRecord> payload = e.getPayloadCollection();
		if (payload.size() == 0)
			return;

		final RowSetRecord record = e.getPayloadCollection().iterator().next();
		if (record == null)
			return;
		
		final CyTable table = e.getSource();
		final CyNetwork network = nameTables.get(table);
		
		// Case 1: Network name/title updated
		if (network != null && record.getColumn().equals(CyNetwork.NAME)) {
			final CyRow row = payload.iterator().next().getRow();
			final String newTitle = row.get(CyNetwork.NAME, String.class);
			final NetworkTreeNode node = this.network2nodeMap.get(network);
			final String oldTitle = treeTableModel.getValueAt(node, 0).toString();

			if (newTitle.equals(oldTitle) == false) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						treeTableModel.setValueAt(newTitle, node, 0);
						getNetTable().repaint();
					}
				});
			}
			return;
		}

		final CyNetwork updateSelected = nodeEdgeTables.get(table);

		// Case 2: Selection updated.
		if (updateSelected != null && record.getColumn().equals(CyNetwork.SELECTED)) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					getNetTable().repaint();
				}
			});
		}
	}

	@Override
	public void handleEvent(final SetSelectedNetworksEvent e) {
		updateNetworkTreeSelection();
	}
	
	@Override
	public void handleEvent(final SetCurrentNetworkViewEvent e) { // FIXME
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				final CyNetworkView curView = appMgr.getCurrentNetworkView();
				
				if (curView != null) {
					final DefaultTableModel model = (DefaultTableModel) getViewTable().getModel();
					final int rowCount = getViewTable().getModel().getRowCount();
					final int[] selectedRows = getViewTable().getSelectedRows();
					
					for (int i = 0; i < rowCount; i++) {
						if (curView.equals(model.getValueAt(i, 0))) {
							if (Arrays.binarySearch(selectedRows, i) < 0) // This row is not selected yet, so select it
								getViewTable().setRowSelectionInterval(i, i);
							
							return;
						}
					}
				}
			}
		});
	}
	
	@Override
	public void handleEvent(final NetworkViewAboutToBeDestroyedEvent nde) {
		final CyNetworkView netView = nde.getNetworkView();
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				logger.debug("Network view about to be destroyed: " + netView);
				final NetworkTreeNode node = treeNodeMap.get(netView.getModel().getSUID());
				
				if (node != null) {
					final Collection<CyNetworkView> views = netViewMgr.getNetworkViews(netView.getModel());
					
					if (views.isEmpty()) {
						node.setNodeColor(NetworkTreeNode.DEF_NODE_COLOR);
						getNetTable().repaint();
					}
				}
				
				updateNetworkViewTable(getSelectionNetworks(CyNetwork.class));
			}
		});
	}

	@Override
	public void handleEvent(final NetworkViewAddedEvent nde) {
		final CyNetworkView netView = nde.getNetworkView();
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				logger.debug("Network view added to NetworkPanel: " + netView);
				final NetworkTreeNode node = treeNodeMap.get(netView.getModel().getSUID());
				
				if (node != null) {
					node.setNodeColor(Color.BLACK);
					getNetTable().repaint();
				}
				
				final Set<CyNetwork> selectedNetworks = getSelectionNetworks(CyNetwork.class);
				updateNetworkViewTable(selectedNetworks);
			}
		});
	}
	
	/**
	 * This method highlights a network in the NetworkPanel.
	 */
	@Override
	public void valueChanged(final TreeSelectionEvent e) {
		if (ignoreTreeSelectionEvents)
			return;

		final JTree tree = getNetTable().getTree();

		// Sets the "current" network based on last node in the tree selected
		final NetworkTreeNode node = (NetworkTreeNode) tree.getLastSelectedPathComponent();
		
		if (node == null || node.getUserObject() == null)
			return;

		CyNetwork cn = node.getNetwork();
		final List<CyNetwork> selectedNetworks = new LinkedList<CyNetwork>();
	
		/*
		if (cn instanceof CyRootNetwork) {
			// This is a "network set" node...
			// When selecting root node, all of the subnetworks are selected.
			CyRootNetwork root = (CyRootNetwork) cn; 
					//((NetworkTreeNode) node.getFirstChild()).getNetwork()).getRootNetwork();

			// Creates a list of all selected networks
			List<CySubNetwork> subNetworks = root.getSubNetworkList();
			
			for (CySubNetwork sn : subNetworks) {
				if (netMgr.networkExists(sn.getSUID()))
					selectedNetworks.add(sn);
			}
			
			// Determine the current network
			if (!selectedNetworks.isEmpty())
				cn = ((NetworkTreeNode) node.getFirstChild()).getNetwork();
		} else { */
			// Regular multiple networks selection...
			try {
				// Create a list of all selected networks
				for (int i = tree.getMinSelectionRow(); i <= tree.getMaxSelectionRow(); i++) {
					NetworkTreeNode tn = (NetworkTreeNode) tree.getPathForRow(i).getLastPathComponent();
					
					if (tn != null && tn.getUserObject() != null && tree.isRowSelected(i))
						selectedNetworks.add(tn.getNetwork());
				}
			} catch (Exception ex) {
				logger.error("Error creating the list of selected networks", ex);
			}
	//	}
		
		final List<CyNetworkView> selectedViews = new ArrayList<CyNetworkView>();
		
		for (final CyNetwork n : selectedNetworks) {
			final Collection<CyNetworkView> views = netViewMgr.getNetworkViews(n);
			
			if (!views.isEmpty())
				selectedViews.addAll(views);
		}
		
		// No need to set the same network again. It should prevent infinite loops.
		// Also check if the network still exists (it could have been removed by another thread).
		if (cn == null || netMgr.networkExists(cn.getSUID())) {
			if (cn == null || !cn.equals(appMgr.getCurrentNetwork()))
				appMgr.setCurrentNetwork(cn);
		
			CyNetworkView cv = null;
			
			// Try to get the first view of the current network
			final Collection<CyNetworkView> cnViews = cn != null ? netViewMgr.getNetworkViews(cn) : null;
			cv = (cnViews == null || cnViews.isEmpty()) ? null : cnViews.iterator().next();
			
			if (cv == null || !cv.equals(appMgr.getCurrentNetworkView()))
				appMgr.setCurrentNetworkView(cv);
			
			appMgr.setSelectedNetworks(selectedNetworks);
			appMgr.setSelectedNetworkViews(selectedViews);
		}
	}
	
	// // Private Methods // //
	
	private void addNetwork(final CyNetwork network) {
		// first see if it is not in the tree already
		if (this.network2nodeMap.get(network) == null) {
			NetworkTreeNode parentTreeNode = null;
			CyRootNetwork parentNetwork = null;
			
			// In current version, ALL networks are created as Subnetworks.
			// So, this should be always true.
			if (network instanceof CySubNetwork) {
				parentNetwork = ((CySubNetwork) network).getRootNetwork();
				parentTreeNode = treeNodeMap.get(parentNetwork.getSUID());
			}

			if (parentTreeNode == null){
				final String rootNetName = parentNetwork.getRow(parentNetwork).get(CyNetwork.NAME, String.class);
				parentTreeNode = new NetworkTreeNode(rootNetName, parentNetwork);
				nameTables.put(parentNetwork.getDefaultNetworkTable(), parentNetwork);
				network2nodeMap.put(parentNetwork, parentTreeNode);
			}

			// Actual tree node for this network
			String netName = network.getRow(network).get(CyNetwork.NAME, String.class);

			if (netName == null) {
				logger.error("Network name is null--SUID=" + network.getSUID());
				netName = "? (SUID: " + network.getSUID() + ")";
			}

			final NetworkTreeNode dmtn = new NetworkTreeNode(netName, network);
			network2nodeMap.put(network, dmtn);
			parentTreeNode.add(dmtn);

			if (treeNodeMap.values().contains(parentTreeNode) == false)
				root.add(parentTreeNode);

			// Register top-level node to map
			if (parentNetwork != null)
				treeNodeMap.put(parentNetwork.getSUID(), parentTreeNode);

			if (netViewMgr.viewExists(network))
				dmtn.setNodeColor(Color.BLACK);

			treeNodeMap.put(network.getSUID(), dmtn);
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ignoreTreeSelectionEvents = true;
					// apparently this doesn't fire valueChanged
					final JTree tree = getNetTable().getTree();
					tree.collapsePath(new TreePath(new TreeNode[] { root }));
					
					tree.updateUI();
					final TreePath path = new TreePath(dmtn.getPath());
					
					tree.expandPath(path);
					tree.scrollPathToVisible(path);
					getNetTable().doLayout();
					
					ignoreTreeSelectionEvents = false;
				}
			});
		}
		
		nameTables.put(network.getDefaultNetworkTable(), network);
		nodeEdgeTables.put(network.getDefaultNodeTable(), network);
		nodeEdgeTables.put(network.getDefaultEdgeTable(), network);
	}
	
	/**
	 * Remove a network from the panel.
	 */
	private void removeNetwork(final CyNetwork network) {
		nameTables.values().removeAll(Collections.singletonList(network));
		nodeEdgeTables.values().removeAll(Collections.singletonList(network));
		treeNodeMap.remove(network.getSUID());
		final NetworkTreeNode node = network2nodeMap.remove(network);
		
		if (node == null)
			return;
		
		final List<NetworkTreeNode> removedChildren = new ArrayList<NetworkTreeNode>();
		final Enumeration<?> children = node.children();
		
		if (children.hasMoreElements()) {
			while (children.hasMoreElements())
				removedChildren.add((NetworkTreeNode) children.nextElement());
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ignoreTreeSelectionEvents = true;
				
				for (final NetworkTreeNode child : removedChildren) {
					child.removeFromParent();
					root.add(child);
				}
				
				final NetworkTreeNode parentNode = (NetworkTreeNode) node.getParent();
				node.removeFromParent();

				if (parentNode.isLeaf()) {
					// Remove from root node
					final CyNetwork parentNet = parentNode.getNetwork();
					nameTables.values().removeAll(Collections.singletonList(parentNet));
					nodeEdgeTables.values().removeAll(Collections.singletonList(parentNet));
					network2nodeMap.remove(parentNet);
					treeNodeMap.remove(parentNet.getSUID());
					
					parentNode.removeFromParent();
				}
				
				getNetTable().getTree().updateUI();
				getNetTable().repaint();
				
				ignoreTreeSelectionEvents = false;
			}
		});
	}

	/**
	 * Update selected row.
	 */
	private final void updateNetworkTreeSelection() {
		final List<TreePath> paths = new ArrayList<TreePath>();
		final List<CyNetwork> selectedNetworks = appMgr.getSelectedNetworks();
		
		for (final CyNetwork network : selectedNetworks) {
			final NetworkTreeNode node = this.network2nodeMap.get(network);
			
			if (node != null) {
				final TreePath tp = new TreePath(node.getPath());
				paths.add(tp);
			}
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ignoreTreeSelectionEvents = true;
				getNetTable().getTree().getSelectionModel().setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
				ignoreTreeSelectionEvents = false;
		
				int maxRow = 0;
				final JTree tree = netTable.getTree();
		
				for (final TreePath tp : paths) {
					final int row = tree.getRowForPath(tp);
					maxRow = Math.max(maxRow, row);
				}
				
				final int row = maxRow;
		
				tree.scrollRowToVisible(row);
				getNetTable().repaint();
				
				updateNetworkViewTable(selectedNetworks);
			}
		});
	}

	private void updateNetworkViewTable(final Collection<CyNetwork> networks) {
//		final List<CyNetwork> networks = appMgr.getSelectedNetworks();
		final List<CyNetworkView> views = new ArrayList<CyNetworkView>();
//		final List<CyNetworkView> views = appMgr.getSelectedNetworkViews();
		
		Collections.sort(views, new Comparator<CyNetworkView>() {
			@Override
			public int compare(final CyNetworkView o1, final CyNetworkView o2) {
				final String t1 = o1.getVisualProperty(BasicVisualLexicon.NETWORK_TITLE);
				final String t2 = o2.getVisualProperty(BasicVisualLexicon.NETWORK_TITLE);
				return collator.compare(t1, t2);
			}
		});
		
		// Get all network views of the selected networks
		for (final CyNetwork net : networks)
			views.addAll(netViewMgr.getNetworkViews(net));
		
		// Update the Views table model 
		final DefaultTableModel model = (DefaultTableModel) getViewTable().getModel();
		model.setRowCount(0);
		
		final CyNetworkView curView = appMgr.getCurrentNetworkView();
		int selectionIndex = -1;
		int count = 0;
		
		for (final CyNetworkView view : views) {
			final Collection<RenderingEngine<?>> engines = renderingEngineMgr.getRenderingEngines(view);
			RenderingEngine<?> engine = null;
			
			for (final RenderingEngine<?> re : engines) {
				if (view.equals(re.getViewModel())) {
					engine = re;
					break;
				}
			}
			
			model.addRow(new Object[]{ view, engine });
			
			if (view.equals(curView))
				selectionIndex = count;
			
			count++;
		}
		
		final int mySelectionIndex = selectionIndex;
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				getViewTable().setModel(model);
				model.fireTableDataChanged();
				
				if (mySelectionIndex >= 0)
					getViewTable().setRowSelectionInterval(mySelectionIndex, mySelectionIndex);
			}
		});
	}
	
	private void selectAllSubnetwork(){
		if (selectedRootSet == null)
			return;
		
		final List<CyNetwork> selectedNetworks = new LinkedList<CyNetwork>();
		CyNetwork cn = null;
		NetworkTreeNode node = null;
		
		for (final CyRootNetwork root : selectedRootSet) {
			// This is a "network set" node...
			// When selecting root node, all of the subnetworks are selected.
			node = this.network2nodeMap.get(root);
			
			// Creates a list of all selected networks
			List<CySubNetwork> subNetworks = root.getSubNetworkList();
			
			for (CySubNetwork sn : subNetworks) {
				if (netMgr.networkExists(sn.getSUID()))
					selectedNetworks.add(sn);
			}
			
			cn = root;
		}
		
		// Determine the current network
		if (!selectedNetworks.isEmpty())
			cn = ((NetworkTreeNode) node.getFirstChild()).getNetwork();
		
		final List<CyNetworkView> selectedViews = new ArrayList<CyNetworkView>();
		
		for (final CyNetwork n : selectedNetworks) {
			final Collection<CyNetworkView> views = netViewMgr.getNetworkViews(n);
			
			if (!views.isEmpty())
				selectedViews.addAll(views);
		}
		
		// No need to set the same network again. It should prevent infinite loops.
		// Also check if the network still exists (it could have been removed by another thread).
		if (cn == null || netMgr.networkExists(cn.getSUID())) {
			if (cn == null || !cn.equals(appMgr.getCurrentNetwork()))
				appMgr.setCurrentNetwork(cn);
		
			CyNetworkView cv = null;
			
			// Try to get the first view of the current network
			final Collection<CyNetworkView> cnViews = cn != null ? netViewMgr.getNetworkViews(cn) : null;
			cv = (cnViews == null || cnViews.isEmpty()) ? null : cnViews.iterator().next();
			
			if (cv == null || !cv.equals(appMgr.getCurrentNetworkView()))
				appMgr.setCurrentNetworkView(cv);
			
			appMgr.setSelectedNetworks(selectedNetworks);
			appMgr.setSelectedNetworkViews(selectedViews);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addFactory(final TaskFactory factory, final Map props, final JPopupMenu popup) {
		final CyAction action;
		
		if (props.containsKey("enableFor"))
			action = new TaskFactoryTunableAction(taskMgr, factory, props, appMgr, netViewMgr);
		else
			action = new TaskFactoryTunableAction(taskMgr, factory, props);

		final JMenuItem item = new JMenuItem(action);

		Double gravity = 10.0;
		
		if (props.containsKey(ServiceProperties.MENU_GRAVITY))
			gravity = Double.valueOf(props.get(ServiceProperties.MENU_GRAVITY).toString());
		
		actionGravityMap.put(item, gravity);
		
		popupMap.put(factory, item);
		popupActions.put(factory, action);
		
		final int menuIndex = getMenuIndexByGravity(popup, item);
		popup.insert(item, menuIndex);
		popup.addPopupMenuListener(action);
	}

	private int getMenuIndexByGravity(final JPopupMenu popup, final JMenuItem item) {
		Double gravity = actionGravityMap.get(item);
		Double gravityX;

		for (int i = 0; i < popup.getComponentCount(); i++) {
			gravityX = actionGravityMap.get(popup.getComponent(i));

			if (gravity < gravityX)
				return i;
		}

		return netPopup.getComponentCount();
	}
	
	private void removeFactory(final TaskFactory factory) {
		final JMenuItem item = popupMap.remove(factory);
		final CyAction action = popupActions.remove(factory);
		
		if (factory instanceof NetworkViewTaskFactory) {
			if (item != null)
				viewPopup.remove(item);
			if (action != null)
				viewPopup.removePopupMenuListener(action);
		} else {
			if (item != null)
				netPopup.remove(item);
			if (action != null)
				netPopup.removePopupMenuListener(action);
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T extends CyNetwork> Set<T> getSelectionNetworks(Class<T> type) {
		final Set<T> nets = new LinkedHashSet<T>();
		final JTree tree = getNetTable().getTree();
		final TreePath[] selectionPaths = tree.getSelectionPaths();
		
		if (selectionPaths != null) {
			for (final TreePath tp : selectionPaths) {
				final CyNetwork n = ((NetworkTreeNode) tp.getLastPathComponent()).getNetwork();
				
				if (n != null && type.isAssignableFrom(n.getClass()))
					nets.add((T) n);
			}
		}
		
		return nets;
	}
	
	// // Private Classes // //
	
	/**
	 * This class listens to mouse events from the Network TreeTable, if the mouse event
	 * is one that is canonically associated with a popup menu (ie, a right
	 * click) it will pop up the menu with option for destroying view, creating
	 * view, and destroying network (this is platform specific apparently)
	 */
	private final class NetPopupListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		// On Windows, popup is triggered by mouse release, not press 
		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		/**
		 * if the mouse press is of the correct type, this function will maybe
		 * display the popup
		 */
		private final void maybeShowPopup(final MouseEvent e) {
			// Ignore if not valid trigger.
			if (!e.isPopupTrigger())
				return;

			// get the row where the mouse-click originated
			final int row = getNetTable().rowAtPoint(e.getPoint());
			if (row == -1)
				return;

			final JTree tree = getNetTable().getTree();
			final TreePath treePath = tree.getPathForRow(row);
			Long networkID = null;
			
			try {
				final CyNetwork clickedNet = ((NetworkTreeNode) treePath.getLastPathComponent()).getNetwork();
				
				if (clickedNet instanceof CyRootNetwork) {
					networkID = null;
					selectedRoot = (CyRootNetwork) clickedNet;
				} else {
					networkID = clickedNet.getSUID();
					selectedRoot = null;
				}
			} catch (NullPointerException npe) {
				// The tree root does not represent a network, ignore it.
				return;
			}

			if (networkID != null) {
				final CyNetwork network = netMgr.getNetwork(networkID);
				
				if (network != null) {
					// if the network is not selected, select it
					final List<CyNetwork> selectedList = appMgr.getSelectedNetworks();
					
					if (selectedList == null || !selectedList.contains(network)) {
						appMgr.setCurrentNetwork(network);
						appMgr.setSelectedNetworks(Arrays.asList(new CyNetwork[]{ network }));
						final Collection<CyNetworkView> netViews = netViewMgr.getNetworkViews(network);
						appMgr.setSelectedNetworkViews(new ArrayList<CyNetworkView>(netViews));
					}
					
					// Always repaint, because the rendering of the tree selection
					// may be out of sync with the AppManager one
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							getNetTable().repaint();
						}
					});
					
					// enable/disable any actions based on state of system
					for (CyAction action : popupActions.values())
						action.updateEnableState();

					// then popup menu
					netPopup.show(e.getComponent(), e.getX(), e.getY());
				}
			} else if (selectedRoot != null) {
				// if the right-clicked root-network is not selected, select it (other selected items will be unselected)
				selectedRootSet = getSelectionNetworks(CyRootNetwork.class);
				
				if (!selectedRootSet.contains(selectedRoot)) {
					final NetworkTreeNode node = network2nodeMap.get(selectedRoot);
					
					if (node != null) {
						appMgr.setCurrentNetwork(null);
						appMgr.setSelectedNetworks(null);
						appMgr.setSelectedNetworkViews(null);
						
						selectedRootSet = Collections.singleton(selectedRoot);
						final TreePath tp = new TreePath(new NetworkTreeNode[]{ root, node });
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								ignoreTreeSelectionEvents = true;
								tree.getSelectionModel().setSelectionPaths(new TreePath[]{ tp });
								getNetTable().repaint();
								ignoreTreeSelectionEvents = false;
							}
						});
					}
				}
				
				editRootNetworTitle.setEnabled(selectedRootSet.size() == 1);
				rootPopupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
}
