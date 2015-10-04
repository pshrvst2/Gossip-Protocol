import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * 
 */

/**
 * @author pshrvst2
 * @Info Listen to other Peers through Gossip protocol.
 *
 */
public class ListenerThread extends Thread 
{
	public static Logger _logger = Logger.getLogger(ListenerThread.class);
	private int port;
	/**
	 * 
	 */
	public ListenerThread(int port) 
	{
		this.port = port;
	}

	public void run()
	{
		_logger.info("Listener thread is activated! Listening ....");
		byte[] data = new byte[4096];
		DatagramSocket listernerSocket;
		try 
		{
			listernerSocket = new DatagramSocket(port);
			while(!Node._listenerThreadStop)
			{
				try 
				{
					DatagramPacket receivedPacket = new DatagramPacket(data, data.length);
					listernerSocket.receive(receivedPacket);
					//String sentence = new String( receivedPacket.getData());
					//System.out.println("RECEIVED: " + sentence);
					int port = receivedPacket.getPort();
					InetAddress ipAddress = receivedPacket.getAddress();
					_logger.info("Received packet from: "+ipAddress+" at port: "+port);

					byte[] receivedBytes = receivedPacket.getData();
					ByteArrayInputStream bais = new ByteArrayInputStream(receivedBytes);
					ObjectInputStream objInpStream = new ObjectInputStream(bais);
					@SuppressWarnings("unchecked")
					HashMap<String, NodeData> map = (HashMap<String, NodeData>) objInpStream.readObject();


					for (HashMap.Entry<String, NodeData> record : map.entrySet())
					{

						String machineId = record.getKey().trim();
						Thread updateThread = new MemberUpdateThread(machineId, record.getValue());
						updateThread.start();
						/*if(record.getValue().isActive())
						{

							if(!Node._gossipMap.containsKey(machineId))
							{
								_logger.info("Added a new machine: "+machineId);
								Node._gossipMap.put(machineId, map.get(machineId));
								//Node._gossipMap.get(machineId).setLastRecordedTime(System.currentTimeMillis());
							}
							else
							{
								NodeData existingNode = Node._gossipMap.get(machineId);
								NodeData recvNode = record.getValue();
								if(existingNode.getLastRecordedTime() < recvNode.getLastRecordedTime())
								{
									_logger.info("Changing the entries for machine: "+machineId);
									Node._gossipMap.get(machineId).setLastRecordedTime(recvNode.getLastRecordedTime());
									Node._gossipMap.get(machineId).setHeartBeat(recvNode.getHeartBeat());
								}
								else
								{
									// the system is probably dead, Mark it as in active.
									_logger.info("Marking "+machineId+" as in active");
									//Node._gossipMap.get(machineId).setActive(false);
								}
							}
						}*/
					}

				}
				catch (IOException e) 
				{
					_logger.error(e);
					e.printStackTrace();
				}
				catch (ClassNotFoundException e) 
				{
					_logger.error(e);
					e.printStackTrace();
				}                
			}
		}
		catch (SocketException e1)
		{
			_logger.error(e1);
			e1.printStackTrace();
		}
	}

	public class MemberUpdateThread extends Thread 
	{
		public Logger _logger = Logger.getLogger(MemberUpdateThread.class);
		private String id = "";
		private NodeData nodeData = null;

		public MemberUpdateThread(String id, NodeData record)
		{
			this.id = id;
			this.nodeData = record;
		}

		public void run()
		{
			// Every record has its thread to check for the updates.
			// Case when the member is still alive on the received list.
			if(nodeData.isActive())
			{
				//add the new member
				if(!Node._gossipMap.containsKey(id))
				{
					_logger.info("Added a new machine: "+id);
					Node._gossipMap.put(id, nodeData);
					Node._gossipMap.get(id).setLastRecordedTime(System.currentTimeMillis());
				}
				// heartbeat of the process is more than the local copy's heartbeart. That means the process has
				// communicated to other processes in the group that it's alive! Don't kill me plssss!!
				else if(nodeData.getHeartBeat() > Node._gossipMap.get(id).getHeartBeat())
				{
					Node._gossipMap.get(id).increaseHeartBeat();
					Node._gossipMap.get(id).setLastRecordedTime(System.currentTimeMillis());
				}
				// check for this process. is it inactive for a long time? Should I declare it dead?
				else if(System.currentTimeMillis() - Node._gossipMap.get(id).getLastRecordedTime()
						> Node._TfailInMilliSec)
				{
					Node._gossipMap.get(id).setActive(false);
					Node._gossipMap.get(id).setLastRecordedTime(System.currentTimeMillis());
					// use the above set time to delete the member from the list. This should be done by a different class or thread possibly.
				}

			}
			// case when the received list has the member as dead.
			else
			{
				NodeData localCopy = Node._gossipMap.get(id);
				// Consider a scenario: Process sent a heartbeat of "15 and dead" and local has "14 and alive". Clearly, mark him dead!
				// Consider a scenario: Process sent a heartbeat of "14 and dead" and local has "14 and alive". Don't do anything but wait for _TFail. But it may also 
				// 						happen that this was the last info sent regarding this process. So, have a check in scheduler or thread class. 
				if(localCopy.isActive() & (localCopy.getHeartBeat() < nodeData.getHeartBeat()))
				{
					// TODO clash of thoughts here. Piyush wants an additional
					// check on the heartbeat, Kevin disagrees.
					_logger.info("Marking machine id: "+id+ " as Inactive (dead)");
					Node._gossipMap.get(id).setActive(false);
					Node._gossipMap.get(id).setLastRecordedTime(System.currentTimeMillis());
					// We are updating this so that we can compare it with _TCleanUp.
				}
				// The else part has to handled in a scheduler as the last process will never be able to delete the dead member!
				/*else
				{
					if((System.currentTimeMillis() - localCopy.getLastRecordedTime())
							> Node._TfailInMilliSec)
					{
						_logger.info("Removing machine id: "+id+" from membership list");
						Node._gossipMap.remove(id);
					}
				}*/
			}
		}
	}
}

