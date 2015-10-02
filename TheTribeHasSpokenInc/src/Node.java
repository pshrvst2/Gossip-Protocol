import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * 
 */

/**
 * @author pshrvst2
 * @Info This is the main class in the application.It has the duty to register itself, send and receive hearbeats
 * from other peers in the group.  
 *
 */
public class Node 
{
	// Naming convention, variables which begin with _ are class members.
	public static Logger _logger = Logger.getLogger(Node.class);
	public final static int _portReceiver = 2000;
	public final static int _portSender = 2001;
	public static String _introducerIp = "192.17.11.57";
	public static boolean _listenerThreadStop = false;
	
	public static List<NodeData> _gossipList = Collections.synchronizedList(new ArrayList<NodeData>());
	// Thread safe data structure needed to store the details of all the machines in the 
	// Gossip group. Concurrent hashmap is our best option as it can store string, nodeData. 
	public static ConcurrentHashMap<String, NodeData> _gossipMap = new ConcurrentHashMap<String, NodeData>();

	/**
	 * @param args To ensure : Server init has to be command line.
	 */
	public static void main(String[] args)
	{
		Thread gossipListener = null;
		try 
		{
			if(initLogging())
			{
				_logger.info("Logging is succesfully initialized! Refer log file CS425_MP2_node.log");
				System.out.println("Logging is succesfully initialized! Refer log file CS425_MP2_node.log");
			}
			else
			{
				_logger.info("Logging could not be initialized!");
				System.out.println("Logging could not be initialized!");
			}

			String machineIp = "";
			machineIp = InetAddress.getLocalHost().getHostAddress().toString();
			
			//Concatenate the ip address with time stamp.
			Long currTimeInMiliSec = System.currentTimeMillis();
			String machineId = machineIp + ":" + currTimeInMiliSec;
			
			_logger.info("Machine IP: "+machineIp+" and Machine ID: "+machineId);
			_logger.info("Adding it's entry in the Gossip list!");
			//System.out.println(machineId);
			NodeData node = new NodeData(machineId, 1, currTimeInMiliSec, true);
			_gossipMap.put(machineId, node);
			//_gossipList.add(node);
			
			
			//check for introducer
			checkIntroducer(machineIp);
			
			//Now open your socket and listen to other peers.
			gossipListener = new ListenerThread(_portReceiver);
			gossipListener.start();
			
			boolean flag = true;
			while(flag)
			{
				System.out.println("Here are your options: ");
				System.out.println("Type 'list' to view the current membership list.");
				System.out.println("Type 'quit' to quit the group and close servers");
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				String userCmd = reader.readLine();
				if(userCmd.equalsIgnoreCase("list"))
				{
					if(!_gossipMap.isEmpty())
					{
						String delim = "\t||\t";
						System.out.println("*********MachineId********"+delim+"**Last Seen**"+delim+"Hearbeat"+delim+"Is Active?");
						_logger.info("User want to list the current members");
						_logger.info("*********MachineId********"+delim+"**Last Seen**"+delim+"Hearbeat"+delim+"Is Active?");
						for (HashMap.Entry<String, NodeData> record : _gossipMap.entrySet())
						{
							NodeData temp = record.getValue();
							System.out.println(record.getKey()
									+delim+temp.getLastRecordedTime()
									+delim+temp.getHeartBeat()+"\t"
									+delim+temp.isActive());
							_logger.info(record.getKey()
									+delim+temp.getLastRecordedTime()
									+delim+temp.getHeartBeat()+""
									+delim+temp.isActive());
						}
					}
				}
				else if(userCmd.equalsIgnoreCase("quit"))
				{
					// send a good bye message to the Introducer so that you are quickly observed by 
					// all nodes that you are leaving.
					System.out.println("Terminating");
					_logger.info("Terminating");
					_listenerThreadStop = true;
					flag = false;
					//gossipListener.stop();
					
				}
			}
			
			
		} 
		catch (UnknownHostException e) 
		{
			_logger.error(e);
			e.printStackTrace();
		} catch (IOException e) 
		{
			_logger.error(e);
			e.printStackTrace();
		}
		finally
		{
			//
		}

	}

	public static boolean initLogging() 
	{
		try 
		{
			PatternLayout lyt = new PatternLayout("[%-5p] %d %c.class %t %m%n");
			RollingFileAppender rollingFileAppender = new RollingFileAppender(lyt, "CS425_MP2_node.log");
			rollingFileAppender.setLayout(lyt);
			rollingFileAppender.setName("LOGFILE");
			rollingFileAppender.setMaxFileSize("64MB");
			rollingFileAppender.activateOptions();
			Logger.getRootLogger().addAppender(rollingFileAppender);
			return true;
		} 
		catch (Exception e) 
		{
			// do nothing, just return false.
			// We don't want application to crash is logging is not working.
			return false;
		}
	}
	
	public static void checkIntroducer(String ip)
	{
		_logger.info("Checking for the introducer.");
		//boolean retVal = false;
		DatagramSocket socket = null;
		try
		{
			if(!ip.equalsIgnoreCase(_introducerIp))
			{
				//if this is the case, introduce yourself to the Introducer
				// so that you are registered to its list and start participating.
				socket = new DatagramSocket();
				int length = 0;
				byte[] buf = null;
				
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			    ObjectOutputStream objOpStream = new ObjectOutputStream(byteArrayOutputStream);
			    //objOpStream.writeObject(_gossipList);
			    objOpStream.writeObject(_gossipMap);
			    buf = byteArrayOutputStream.toByteArray();
			    length = buf.length;
			    
			    DatagramPacket dataPacket = new DatagramPacket(buf, length);
				dataPacket.setAddress(InetAddress.getByName(_introducerIp));
				dataPacket.setPort(_portSender);
				int retry = 5;
				//try five times as UDP is unreliable. At least one message will reach :)
				while(retry > 0)
				{
					socket.send(dataPacket);
					--retry;
					//Thread currentTheard = Thread.currentThread();
					Thread.sleep(1000);
				}
			}
		}
		catch(SocketException ex)
		{
			_logger.error(ex);
			ex.printStackTrace();
		}
		catch(IOException ioExcep)
		{
			_logger.error(ioExcep);
			ioExcep.printStackTrace();
		} 
		catch (InterruptedException e) 
		{
			_logger.error(e);
			e.printStackTrace();
		}
		finally
		{
			if(socket != null)
				socket.close();
			_logger.info("Exiting from the method checkIntroducer.");
		}
		//return retVal;
	}

}
