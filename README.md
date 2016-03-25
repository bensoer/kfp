# 8005-fp-portforwarder

Created By Eric Tsang and Ben Soer

8005-fp-portforwarder is a Kotlin based Port Forwarding application that allows users to redirect UDP or TCP
traffic to a designated host on a specific IP. The project runs multi-threaded with a GUI allowing realtime
responsiveness to changes applied on port forwards. Additionaly the system monitors traffic quanities and
the number of connections made through the various ports that are forwarded. Users observing the GUI can
watch in real-time the amount of traffic passing through the Port Forwarder at any gvien time.

# Setup

Before you can run the application you will need to make sure you
have Kotlin installed on your machine. You can download Kotlin from
[here on github](https://github.com/JetBrains/kotlin/releases/tag/1.0.1-1).

You will also need to have the latest Java 8 installed in order for the GUI and
live analysis functionality to work, AKA the port forwarder does not work without
Java 8. You can [download Java 8 here](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

For the next steps it is assumed that both the commands `kotlinc` and `java` can
both be called from any location on the terminal

To setup 8005-fp-portforwarder do the following:
1. Clone the Project / Download the Project
2. `cd` into the `src` folder
3. call `kotlinc main.Application.kt -include-runtime -d PortForwarder.jar`
4. call `java -jar PortForwarder.jar`
5. The program should begin initializing

# Usage

The Port Forwarding Application is quite straight forward. All 
activities are carried out on the GUI.

## Add An Entry
To add a port forwarding entry simply enter in the entry list:
1. the port to listen on, followed by
2. the protocol to be used (either TCP or UDP) 
3. the destination host and port that the port forwarder will forward 
all of its traffic to. 
4. wait for the text entries to turn white

## Remove An Entry
To remove a forwarding entry simply delete it from the list or make it
invalid by changing any of the entry boxes to an invalid one.

## Update An Entry
Update an entry by simply changing the values in the entry boxes you would
like to change. The port forwarder will update your changes after a second
of no activity on the entry and that the new entry is deemed valid.

## View Real-Time Statistics
To be able to observe the real-time stats, make a connection to the port
forwarder that involves data to pass through the port forwarder. The
port forwarder will immediately start displaying stats as the data passes
through the port forwarder

## Notes. Tips. Tricks.
* If you don't know the IP of the host you can also enter in domain 
names (eg facebook.com) instead of an IP and the port forwarder will 
automatically DNS resolve it to its appropriate IP.

* If the text entries do not turn white in Step 4, it means the entered 
ports, IP or domain entered are invalid for the forwarding entry you 
are trying to make. The port forwarder will only add records when a 
valid forwarding entry is added. 

* If you are entering a domain name for an IP, the text entries may not 
turn white because the port forwarder was unable to make a DNS lookup
on the domain. This could be caused by no internet connection, a blocked
DNS lookup, or your local DNS does not yet contain the A record at the
time of the query.

* If a TCP entry is removed or updated, any already existing connections
through the original mapping will be kept active until they formally 
terminate

* If a UDP entry is removed or updated, any already existing UDP
connections through the original mapping will be cut off and completely
removed

* On a failure to insert, update or remove a TCP/UDP entry, an error 
prompt will appear and the port forwarder will be reverted to its 
original state before the change occurred.