package org.cytoscape.task.internal.export.network;

import org.cytoscape.io.write.CyNetworkViewWriterManager;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ExportNetworkViewTaskFactory extends AbstractNetworkViewTaskFactory {

	private CyNetworkViewWriterManager writerManager;

	public ExportNetworkViewTaskFactory(CyNetworkViewWriterManager writerManager) {
		this.writerManager = writerManager;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(2,new CyNetworkViewWriter(writerManager, view));
	}

}
