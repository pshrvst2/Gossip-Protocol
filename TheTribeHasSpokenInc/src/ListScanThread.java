import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * 
 */

/**
 * @author pshrvst2
 * @INFO Class to scan the membership thread and perform necessary actions.
 *
 */
public class ListScanThread extends Thread {
	
	public Logger _logger = Logger.getLogger(ListScanThread.class);
	
	public ListScanThread() 
	{
		// Default constructor : Do nothing
	}
	
	public void run()
	{
		for (HashMap.Entry<String, NodeData> record : Node._gossipMap.entrySet())
		{
			String nodeId = record.getKey();
			if(!record.getValue().isActive() & ((System.currentTimeMillis() - record.getValue().getLastRecordedTime()) >= Node._TCleanUpInMilliSec))
			{
				Node._gossipMap.remove(nodeId);
			}
			else if(record.getValue().isActive() & ((System.currentTimeMillis() - record.getValue().getLastRecordedTime()) >= Node._TfailInMilliSec))
			{
				Node._gossipMap.get(nodeId).setActive(false);
				Node._gossipMap.get(nodeId).setLastRecordedTime(System.currentTimeMillis());
			}
		}
	}

}
