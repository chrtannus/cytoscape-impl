package org.cytoscape.ding.internal.charts.pie;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cytoscape.ding.internal.charts.AbstractChartLayer;
import org.cytoscape.ding.internal.charts.CustomPieSectionLabelGenerator;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.PieDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.Rotation;

public class PieLayer extends AbstractChartLayer<PieDataset> {
	
	/** Just to prevent the circle's border from being cropped */
	public static final double INTERIOR_GAP = 0.004;
	
	private final Map<String, String> labels;
	private final double startAngle;

	// ==[ CONSTRUCTORS ]===============================================================================================
	
	public PieLayer(final Map<String, List<Double>> data,
					final List<String> itemLabels,
					final boolean showLabels,
					final List<Color> colors,
					final double startAngle,
					final Rectangle2D bounds) {
        super(data, itemLabels, null, null, showLabels, false, false, colors, null, bounds);
        this.startAngle = startAngle;
        this.labels = new HashMap<String, String>();
	}
	
	// ==[ PRIVATE METHODS ]============================================================================================
	
	@Override
	protected PieDataset createDataset() {
		final List<Double> values = data.isEmpty() ? null : data.values().iterator().next();
		final PieDataset dataset = createPieDataset(values);
		
		if (showItemLabels && itemLabels != null) {
			final List<?> keys = dataset.getKeys();
			
			for (int i = 0; i < keys.size(); i++) {
				final String k = (String) keys.get(i);
				final String label = itemLabels.size() > i ? itemLabels.get(i) : null;
				labels.put(k, label);
			}
        }
		
		return dataset;
	}
    
	@Override
	protected JFreeChart createChart(final PieDataset dataset) {
		JFreeChart chart = ChartFactory.createPieChart(
				null, // chart title
				dataset, // data
				false, // include legend
				false, // tooltips
				false); // urls
		
        chart.setAntiAlias(true);
        chart.setBorderVisible(false);
        chart.setBackgroundPaint(TRANSPARENT_COLOR);
        chart.setBackgroundImageAlpha(0.0f);
        chart.setPadding(new RectangleInsets(0.0, 0.0, 0.0, 0.0));
        
		final PiePlot plot = (PiePlot) chart.getPlot();
		plot.setCircular(false);
		plot.setStartAngle(startAngle);
		plot.setDirection(Rotation.CLOCKWISE);
		plot.setOutlineVisible(false);
		plot.setInsets(new RectangleInsets(0.0, 0.0, 0.0, 0.0));
		plot.setInteriorGap(INTERIOR_GAP);
		plot.setBackgroundPaint(TRANSPARENT_COLOR);
		plot.setBackgroundAlpha(0.0f);
		plot.setShadowPaint(TRANSPARENT_COLOR);
		plot.setShadowXOffset(0.0);
		plot.setShadowYOffset(0.0);
		plot.setLabelGenerator(showItemLabels ? new CustomPieSectionLabelGenerator(labels) : null);
		plot.setSimpleLabels(true);
		plot.setLabelFont(plot.getLabelFont().deriveFont(labelFontSize));
		plot.setLabelBackgroundPaint(TRANSPARENT_COLOR);
		plot.setLabelOutlinePaint(TRANSPARENT_COLOR);
		plot.setLabelShadowPaint(TRANSPARENT_COLOR);
		plot.setLabelPaint(labelColor);
		plot.setLabelFont(plot.getLabelFont().deriveFont(1.0f));
		
		final BasicStroke stroke =
				new BasicStroke((float)borderWidth/LINE_WIDTH_FACTOR, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		
		final List<?> keys = dataset.getKeys();
		
		for (int i = 0; i < keys.size(); i++) {
			final String k = (String) keys.get(i);
			final Color c = colors.size() > i ? colors.get(i) : Color.LIGHT_GRAY;
			plot.setSectionPaint(k, c);
			plot.setSectionOutlinePaint(k, borderColor);
			plot.setSectionOutlineStroke(k, stroke);
		}
		
		return chart;
	}
}