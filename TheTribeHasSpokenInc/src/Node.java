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
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

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
	public final static int _portSender = 2001;
	public final static int _portReceiver = 2000;
	public static String _introducerIp = "172.22.151.17";
	public static boolean _listenerThreadStop = false;
	public static String _machineIp = "";
	public static String _machineId= "";
	public static int _TfailInMilliSec = 3000;
	public static int _TCleanUpInMilliSec = 3000;
	public static TimeUnit unit = MILLISECONDS;
	public static int _lossRate =0;
	
	
	//public static List<NodeData> _gossipList = Collections.synchronizedList(new ArrayList<NodeData>());
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
			
			_machineIp = InetAddress.getLocalHost().getHostAddress().toString();
			
			boolean flag = true;
			while(flag)
			{
				System.out.println("\tWelcome to TheTribeHasSpokenInc!");
				System.out.println("\tPress 1 to join");
				System.out.println("\tPress 2 for system info");
				System.out.println("\t!!Press any other key to throw yourself out of the island!!");
				BufferedReader readerKeyboard = new BufferedReader(new InputStreamReader(System.in));
				String option = readerKeyboard.readLine();
				if(option.equalsIgnoreCase("1"))
					flag = false;
				else if (option.equalsIgnoreCase("2"))
				{
					System.out.println("\tYou are at machine: "+_machineIp);
				}
				else
				{
					System.out.println("\tYou are out of the island now! Good Bye!!");
					return;
				}
			}
			
			//Concatenate the ip address with time stamp.
			Long currTimeInMiliSec = System.currentTimeMillis();
			_machineId = _machineIp + ":" + currTimeInMiliSec;
			
			_logger.info("Machine IP: "+_machineIp+" and Machine ID: "+_machineId);
			_logger.info("Adding it's entry in the Gossip list!");
			//System.out.println(machineId);
			NodeData node = new NodeData(_machineId, 1, currTimeInMiliSec, true);
			_gossipMap.put(_machineId, node);
			//_gossipList.add(node);
			
			
			//check for introducer
			checkIntroducer(_machineIp);
			
			//Now open your socket and listen to other peers.
			gossipListener = new ListenerThread(_portReceiver);
			gossipListener.start();
			
			// logic to send periodically
			ScheduledExecutorService _schedulerService = Executors.newScheduledThreadPool(3);
			_schedulerService.scheduleAtFixedRate(new SenderThread(_portReceiver), 0, 1000, unit);
			
			// logic to scan the list and perform necessary actions.
			_schedulerService.scheduleAtFixedRate(new ListScanThread(), 0, 100, unit);
			
			//logic to check whether the introducer is trying to rejoin again
			if(_machineIp != _introducerIp)
			{
				// we will check this occasionally
				_schedulerService.scheduleAtFixedRate(new IntroducerRejoinThread(), 0, 5000, unit);
			}
			
			flag = true;
			while(flag)
			{
				System.out.println("\nHere are your options: ");
				System.out.println("Type 'list' to view the current membership list.");
				System.out.println("Type 'quit' to quit the group and close servers");
				System.out.println("Type 'info' to know your machine details");
				System.out.println("Type 'change' following the desired loss rate to change the current loss rate = '"+_lossRate + "%' ==> EX: 'change:30'");
				
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
									+delim+temp.getHeartBeat()+"\t"
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
					Node._gossipMap.get(_machineId).setActive(false);
					Node._gossipMap.get(_machineId).increaseHeartBeat();
					flag = false;
					Thread.sleep(1001);
					_schedulerService.shutdownNow();
					//gossipListener.stop();
					
				}
				else if(userCmd.equalsIgnoreCase("info"))
				{
					NodeData temp = _gossipMap.get(_machineId);
					String delim = "\t||\t";
					System.out.println("*********MachineId********"+delim+"**Last Seen**"+delim+"Hearbeat"+delim+"Is Active?");
					System.out.println(temp.getNodeId()
							+delim+temp.getLastRecordedTime()
							+delim+temp.getHeartBeat()+"\t"
							+delim+temp.isActive());
					_logger.info(temp.getNodeId()
							+delim+temp.getLastRecordedTime()
							+delim+temp.getHeartBeat()+""
							+delim+temp.isActive());
				}
				// logic here to enable the user to change the loss rate in command line
				else if(userCmd.toLowerCase().trim().startsWith("change"))
				{
					try 
					{
						String[] array =  userCmd.split(":");
						// we r so careful here
						if(array[0].trim().equalsIgnoreCase("change"))
						{
							int lr = Integer.valueOf(array[1]);
							if (lr >= 0 & lr < 31)
							{
								_logger.info("The loss rate has been changed from "+Node._lossRate+" to "+lr);
								Node._lossRate = lr;								
							}
							else 
							{
								System.out.println("The acceptable loss rate should be in range [0,30]");
							}
						}
					}
					catch(Exception e)
					{
						_logger.error(e);
						System.out.println("Invalid command");
					}
					
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
		} catch (InterruptedException e) 
		{
			_logger.error(e);
			e.printStackTrace();
		}
		finally
		{
			System.out.println("Good Bye!");
			_logger.info("Good Bye!");
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
				//if this is the case, either the introducer is the first time initialized or trying to rejoin the existing group
				// so we try to contact all the member to contact all the member add itself to the list and retrieve the existing
				// list from any alive members
				socket = new DatagramSocket();
				int length = 0;
				byte[] buf = null;
				
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			    ObjectOutputStream objOpStream = new ObjectOutputStream(byteArrayOutputStream);
			    //objOpStream.writeObject(_gossipList);
			    HashMap<String, NodeData> map = new HashMap<String, NodeData>();
			    for (HashMap.Entry<String, NodeData> record : _gossipMap.entrySet())
				{
			    	map.put(record.getKey(), record.getValue());
				}
			    objOpStream.writeObject(map);
			    buf = byteArrayOutputStream.toByteArray();
			    length = buf.length;
			    
			    DatagramPacket dataPacket = new DatagramPacket(buf, length);
				dataPacket.setAddress(InetAddress.getByName(_introducerIp));
				dataPacket.setPort(_portReceiver);
				int retry = 3;
				//try three times as UDP is unreliable. At least one message will reach :)
				while(retry > 0)
				{
					socket.send(dataPacket);
					--retry;
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
		finally
		{
			if(socket != null)
				socket.close();
			_logger.info("Exiting from the method checkIntroducer.");
		}
		//return retVal;
	}

}
