package org.cytoscape.ding.internal.charts;

public enum Rotation {
	ANTICLOCKWISE(-1),
	CLOCKWISE(1);
	
	private final int code;

	private Rotation(int code) {
		this.code = code;
	}
	
	@Override
	public String toString() {
		return "" + code;
	}
}