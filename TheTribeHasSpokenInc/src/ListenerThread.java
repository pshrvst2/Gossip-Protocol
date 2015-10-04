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
					String sentence = new String( receivedPacket.getData());
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
						
						if(record.getValue().isActive())
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
						}
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
}
