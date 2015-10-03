
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;




public class SenderThread extends Thread
{
	public static Logger _logger = Logger.getLogger(ListenerThread.class);
	private int port;
	private static String _machineIp;
	
	public SenderThread(int port) 
	{
		this.port = port;
		_machineIp = Node._machineIp;
	}

	
	public void run()
	{
		
		_logger.info("Sender thread is activated! sending...");
		//byte[] data = new byte[1024];
		DatagramSocket senderSocket;
		try
		{
			senderSocket = new DatagramSocket();
			int length = 0;
			byte[] buf = null;
			
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			ObjectOutputStream objOpStream = new ObjectOutputStream(byteArrayOutputStream);

			Set<String> ip2bSent = get2RandomIpAddresses(_machineIp);
			
			// update the heart-beat and time stamp before send out the membership list
			updateHearbeatAndTimeStamp();
			objOpStream.writeObject(Node._gossipMap);
			buf = byteArrayOutputStream.toByteArray();
			length = buf.length;
		
			for(String ip : ip2bSent)
			{
				DatagramPacket dataPacket = new DatagramPacket(buf, length);
				dataPacket.setAddress(InetAddress.getByName(ip));
				dataPacket.setPort(port);
				senderSocket.send(dataPacket);
			}
		}
		catch(SocketException e1)
		{
			_logger.error(e1);
			e1.printStackTrace();
		}
		catch(Exception e)
		{
			_logger.error(e);
			e.printStackTrace();
		}
		

	
	}
	
	public static Set<String> get2RandomIpAddresses(String machineId)
	{		
		HashMap <String, NodeData> gossipMap = new HashMap <String, NodeData>();
		gossipMap.putAll(Node._gossipMap);
		// take out the local id
		int len = gossipMap.size() -1 ;
		
		// retrieve the ip list from membership list
		String[] retVal = new String[len];
		int i = 0;
		for (HashMap.Entry<String, NodeData> rec : gossipMap.entrySet())
		{
			String machinId = rec.getKey();
			String[] temp = machinId.split(":");
			if (!temp[0].equals(machineId))
			retVal[i] = temp[0];
			++i;
		}
		
		// get two random ip address 
		Set<String> ips = new HashSet<String>(); 				
		while (ips.size()<2)
		{
			int index = (int)(Math.random()*(len-1));
			ips.add(retVal[index]);
		}		
		return ips;
	}
	
	public static void updateHearbeatAndTimeStamp()
	{
		Node._gossipMap.get(_machineIp).setLastRecordedTime(System.currentTimeMillis());
		Node._gossipMap.get(_machineIp).increaseHeartBeat();
	}
}
