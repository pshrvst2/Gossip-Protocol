import java.io.Serializable;

/**
 * 
 */

/**
 * @author pshrvst2
 *
 *@Info This class will store information which each node passes to other node in Gossip.
 */
public class NodeData implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String nodeId = "";
	private long heartBeat = 0l;
	private long lastRecordedTime = 0l;
	private boolean isActive = true;
	
	public NodeData() 
	{
		super();
	}
	
	public NodeData(String nodeId, long heartBeat, long lastRecordedTime) 
	{
		//super();
		this.nodeId = nodeId;
		this.heartBeat = heartBeat;
		this.lastRecordedTime = lastRecordedTime;
	}

	public NodeData(String nodeId, long heartBeat, long lastRecordedTime,
			boolean isActive) 
	{
		this.nodeId = nodeId;
		this.heartBeat = heartBeat;
		this.lastRecordedTime = lastRecordedTime;
		this.isActive = isActive;
	}

	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	public long getHeartBeat() {
		return heartBeat;
	}
	public void setHeartBeat(long heartBeat) {
		this.heartBeat = heartBeat;
	}
	public void increaseHeartBeat()
	{
		this.heartBeat += 1;
	}
	public long getLastRecordedTime() {
		return lastRecordedTime;
	}
	public void setLastRecordedTime(long lastRecordedTime) {
		this.lastRecordedTime = lastRecordedTime;
	}
	public boolean isActive() {
		return isActive;
	}
	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}
	
}
