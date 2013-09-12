package org.cytoscape.internal.view;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

public class ViewCellRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = -3195110112562107174L;

	@Override
	public Component getTableCellRendererComponent(final JTable table, final Object value,
			final boolean isSelected, final boolean hasFocus, final int row, final int column) {
		if (isSelected) {
			setForeground(NetworkPanel.FONT_COLOR);
			setBackground(NetworkPanel.SELECTION_BG_COLOR);
		} else {
			setForeground(NetworkPanel.FONT_COLOR);
			setBackground(Color.WHITE);
		}

		String title = null;
		String toolTip = null;
		
		if (column == 0 && value instanceof CyNetworkView) {
			final CyNetworkView netView = (CyNetworkView) value;
			
			if (netView != null) {
				title = netView.getVisualProperty(BasicVisualLexicon.NETWORK_TITLE);
				
				if (title == null || title.trim().isEmpty()) {
					final CyRow cyRow = netView.getModel().getDefaultNetworkTable().getRow(netView.getModel().getSUID());
					title = cyRow != null ? cyRow.get(CyNetwork.NAME, String.class) : null;
					
					if (title == null)
						title = "?";
				}
			}
		} else if (column == 1 && value instanceof RenderingEngine) {
			final RenderingEngine<?> engine = (RenderingEngine<?>) value;
			toolTip = title = engine.getRendererId();
			
			if ("org.cytoscape.ding".equalsIgnoreCase(title)) {
				title = "Cytoscape 2D";
				toolTip = title + " (" + engine.getRendererId() + ")";
			}
		}
		
		setText(title);
		setToolTipText(toolTip);
		
		return this;
	}
}
