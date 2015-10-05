                                CS425 – MP2 Report

Group 31 members:
1.	Xiaoming Chen (xchen135)			2.      Piyush Shrivastava (pshrvst2)

Design
The idea here is to build a membership group among seven available CS VM cluster machines. We implement Gossip membership protocol to accomplish this. To make our solution scalable, we have made use of multi-threading and scheduler services available in java. The protocol begins with an ‘introducer’ who is an initiator in the membership. The ip address of the introducer is shared across all the seven machines. Please be noted that all machines know of just one ip address and that is of the introducer. This is unlike MP1, where everyone knew everyone.

Algorithm used to join group:
1.	Each machine has a unique id (ip: timestamp) and initially asks the user whether he/she wishes to join the membership. The user then selects the option to join.
2.	The logic then checks whether the machine being used is the introducer or not. If it’s not the introducer, it sends UDP packets to the introducer. The packet includes concurrent membership list (concurrent HashMap to be precise) converted into a byte array buffer data. We definitely want to make sure that we reach to the introducer here, so we send the packets thrice increasing our chances of communicating with the introducer even in a high packet loss network.
3.	The introducer listens to the incoming messages and as soon as it receives a packet from a new member, it adds it onto its membership list.
4.	Each machine has a separated thread controlled by a scheduler to send its membership list randomly to any two members in the list. We take care in the code that a machine doesn’t send heartbeats to itself. Since the introducer now has an entry of a new machine in its list, it sends the new machine its membership list.
5.	Before sending the membership list, the machine increments its heartbeat counter.
6.	Next, the new machine receives its first packet and it starts updating its own list.
7.	All machines in the cluster follow steps1 through 6 and now we have a group of seven machines in the membership.
8.	It’s important to note here that each machine sends its membership list every second to any two random members who are active in its list. The time to mark a node as inactive is set to 3 seconds and the time to delete a member from the list is 6 seconds. We implement two different time variables for marking inactive and actual delete so that we overcome the stale member list issue. Also, having machine id as a concatenated string of ip and timestamp allows us to differentiate two different instances coming from the same machine.

Algorithm for membership list update
A machine cannot be dependent on clocks alone as there can be some clock skew or draft. Therefore, to update its membership list, all machines refer the heartbeat received in the received membership list. If heartbeat received it greater than the heartbeat it has for the member, it updates the heartbeat and the last recorded timestamps in its local membership list. If the machine receives a member as inactive with a higher heartbeat, then it marks it as inactive. Also, all machines have a separate scheduler running every 100ms to check for inactive and dead nodes. The list update function for each member in the list is handled through multi-threading which allows our solution to be highly scalable and not being dependent on the existing number of nodes N.

Algorithm for Introducer Rejoin
We have a scheduler which spawns a thread every 5 seconds to check whether it’s has the introducer in its list or nor. In case it doesn’t find the introducer in the list, it sends him heartbeat hoping that it will be alive at some point in the future.  This logic allows us to have one group all the time. If we don’t have this feature, there will be a formation of two groups: one having older members and the other having the introducer and the nodes joining later.

Integration with MP1
Like MP1, we have used log4j in our MP2 solution to log the activities occurring at the respective machines. The logs collected have information of nodes joining, leaving, marking other node as active and deleting the node entry from the list. That apart we also log the thread name or the class name in each log statement. In MP1, we only had to edit the filename. With MP1 we were easily able to detect scenarios like deleting a member from the member list. 
