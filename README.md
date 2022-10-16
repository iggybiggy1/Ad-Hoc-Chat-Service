## Integration Project group #59
This code is an implementation to the challenge provided by 2022 integration project. It's goal is to provide 
a user with a chat-based service applied for up to four nodes in an ad-hoc network. This document provides a quick guide 
how to use it.

## Initial Startup
In order to start the system, a user needs to start the MyProtocol class, initially it is going to show 
```
  ~~ Welcome ~~
Hello User X. Setting up the network... 

>User X: Please wait while the network is being established... 
```
Our system can operate with up to four nodes, each being granted a unique address with the class Addressing. 
There is a chance that the address for two or more nodes can turn out to be the same. Fortunately the nodes 
after the start-up, start sending their addressing and routing information until the network fully converges, and if one node senses that the address of the other node is the same, it will send an addressing change request. 
This unfortunately may not always work, and in that case the system needs to be restarted.

The start-up for 4 nodes can take up to 60 seconds, after which the following message should appear:
```
ROUTING INFO WAS RECEIVED
	Dest:	Cost:	NextHop:
V		A		Z
X		B		V
Y		C		Y
Z		D		X
```
Where letters V,X,Y,Z are addresses, and A,B,C,D are link costs. This table will differ, depending on the chosen topology of the network.
If all nodes are reachable without intermediate forwarding, then all costs will be 1 (excluding the cost to our own node, which is 0).

After roughly 60 seconds, and seeing the aforementioned output, the user is ready to input commands.  

## Usage 
In order to start with using the system, the user can use -Help commands, to see possible commands. The output should look like:
```
 ~~ MENU ~~
Welcome User X. If you want to use a command, please start with '-' and don't include spaces. 
Commands you may use:
-Help
-Message
-UserList
-About
-Quit
```
If a user wants to send a message, they can use an -m or -Message command, followed by their input. Since the code is equipped with fragmentation, the message can be arbitrary large
(just bear in mind that the longer the message is, the longer it will take to fully transmit it to all other nodes). After a message is sent, all other users should see the message (it may be that the receiving side gets such output) :
```
Received message from User X: ........

PACKET NOT FOR ME

PACKET NOT FOR ME
```
It means that the packet were retransmitted, but the system received the data after first time. Sometimes there is a chance that the first message may not be received by all of the nodes. Fortunately after the first message, sending any kinds of messages should work without an issue.
This applies to both: messages below 28 characters, and messages above 28 characters (where fragmentation occurs). It is a serious inconvenience, but the team was not able 
to determine the source of the issue.

If the user wishes to see all 
currently available nodes, they can use the command -Userlist. It is going to print out the list of currently available nodes in the form
```
>User X is available
>User Y is available
>User Z is available
>User V is available
```
Of course the size of the list will be adequate to number of available nodes. During initial addressing and routing, the 
reachability information is also shared. Additionally, everytime a message is broadcast, each node that received the message 
updates the availability of the sending node. 

If the user wishes to quit, they can do it with command  -Quit. The system shall 
send the message with finFlag set, which is going to inform all other nodes that the current node is no more available, and the system shall terminate. Whenever a node receives a message with finFlag set to 1, it shall remove the source address of that packet, from it's list of currently available nodes.

This concludes the usage of the system, as it is a relatively simple. Hopefully, the system meets all necessary passing requirements.

## Contributions 
All contributions of each team-member can be seen on the gitlab repository 

[Group #59 repository](https://gitlab.utwente.nl/s2536528/integration-project-group-59/)

### Team 59