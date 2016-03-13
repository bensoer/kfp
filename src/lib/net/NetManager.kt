package lib.net

import com.sun.javaws.exceptions.InvalidArgumentException
import com.sun.xml.internal.fastinfoset.util.StringArray
import gui.GUI
import javafx.collections.ObservableMap
import lib.net.AddressMapper
import tools.AddressPair
import tools.Logger
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

/**
 * Created by bensoer on 04/03/16.
 */

class NetManager(val addressMapper: AddressMapper): Thread(){


    private val select:Select;
    private var keepGoing = true;

    init {
        this.select = Select();
    }

    var listener: IListener? = null;
    interface IListener {

        fun bytesTransfered(sourceAddress:InetSocketAddress, localPort:Int, bytesTransferred:Int);

        fun connectionOpened();

        fun connectionClosed();
    }

    fun addMapping(addressPair:AddressPair):Boolean{

        val serverSocketChannel = NetLibrary.createServerSocket(addressPair.localPort);
        if(serverSocketChannel == null){
            Logger.log("NetManager - ERROR - Failed To Create A Server Socket For New Port");
            return false;
        }else{
            this.addressMapper.addPortMapping(addressPair);
            this.select.registerServerChannel(serverSocketChannel);
            Logger.log("NetManager - Mapping Added Complete");
            return true;
        }
        Logger.log("why am i here");
    }

    fun removeMapping(addressPair:AddressPair):Boolean{
        val keysSet = this.select.getAllKeys();

        val iterator = keysSet.iterator();
        while(iterator.hasNext()){
            val key = iterator.next();

            println("Number of Keys: ${keysSet.size}");

            if(this.select.getChannelForKey(key) is ServerSocketChannel){
                val channel = this.select.getChannelForKey(key) as ServerSocketChannel;
                val listeningPort = channel.socket().localPort

                println("Listening Port: $listeningPort");

                //if this ServerSocket is listening on the same port then we know to delete it
                if(listeningPort == addressPair.localPort){

                    //close the channel
                    channel.close();
                    //cancel the key
                    key.cancel();
                    //remove from iterator ?
                    //iterator.remove();

                    //remove from database
                    this.addressMapper.deletePortMapping(addressPair);

                    return true;
                }
            }
        }

        Logger.log("NetMapper - ERROR - Failed To Delete Port Mapping");
        return false;

    }

    //shut this bad boy down
    fun terminate(){
        Logger.log("NetManager - System Terminating");
        Logger.log("NetManager - Disabled While Loop From Continueing");
        this.keepGoing = false;

        Logger.log("NetManager - Now Closing All Channels and Keys");
        //get all keys
        var allKeys = this.select.getAllKeys();
        var iterator = allKeys.iterator();
        while(iterator.hasNext()){
            val key = iterator.next();

            //close all sockets
            val channel = this.addressMapper.getSocketChannel(key);
            channel?.close();

            //cancel all keys
            key.cancel();

        }

        Logger.log("NetManager - Closing The Select Engine");
        //delete select
        this.select.close();

        Logger.log("NetManager - Deleting All Socket Mappings");
        //delete all socket mappings
        this.addressMapper.clearAllSocketMappings();
    }

    //start the fun boys
    override fun run(){

        Logger.log("NetManager - Creating Listener Sockets For All Known Mappings");

        val allKnownPorts = this.addressMapper.getAllPortMappings();
        val iterator = allKnownPorts.iterator();

        //iterate over all persisted addresses
        while(iterator.hasNext()){
            val addressPair = iterator.next();


            if(addressPair.type.equals("TCP")){
                val socketChannel = NetLibrary.createServerSocket(addressPair.localPort);

                //if it succeeded register it with select
                if(socketChannel != null) {
                    this.select.registerServerChannel(socketChannel);
                }else{
                    throw NullPointerException("NetManager - Failed to Create a TCP Server Socket For New Port");
                }


            }else if(addressPair.type.equals("UDP")){
                val datagramChannel = NetLibrary.createUDPServerSocket(addressPair.localPort);

                //if it succeeded register it with select
                if(datagramChannel != null) {
                    this.select.registerChannel(datagramChannel);
                }else{
                    throw NullPointerException("NetManager - Failed to Create a UDP Server Socket For New Port");
                }

            }else{
                throw IllegalArgumentException("NetManager - No TYPE Supplied for Channel. Unable To Bind");
            }



        }

        //wait for something to happen
        while(keepGoing){
            //Logger.log("Waiting For Events");
            val numberOfEvents = this.select.waitForEvent()
            //Logger.log("Back From Events");

            //double check we were not awoken by a termination
            if(keepGoing == false){
                Logger.log("NetManager:run - Termination Detected. Breaking Early To Avoid Exceptions");
                break;
            }

            if(numberOfEvents == 0){
                continue;
            }

            val readyChannelKeys = this.select.getReadyChannels();
            val keyIterator = readyChannelKeys.iterator() as MutableIterator<SelectionKey>;

            while(keyIterator.hasNext()){
                val key = keyIterator.next();

                if(!key.isValid){
                    Logger.log("NetManager - Invalid Key Detected. Assumed Cancelled Connection. Not Processing");
                    continue;
                }

                if(this.select.isANewConnection(key)){
                    Logger.log("NetManager - Found A New TCP Connection");

                    //get the channel
                    val channel = this.select.getChannelForKey(key);
                    //downcast it since it can accept. It's a ServerSocketChannel ?
                    val serverSocketChannel = channel as ServerSocketChannel;

                    //accept the connection
                    Logger.log("NetManager - Now Accepting Connection");
                    //serverSocketChannel.configureBlocking(true);
                    val serverSocketSession = serverSocketChannel.accept();
                    if(serverSocketSession == null){
                        Logger.log("NetManager - Nothing To Accept From the Connection")
                        continue;
                    }
                    Logger.log("NetManager - Connection Accepted");
                    //serverSocketChannel.configureBlocking(false);


                    Logger.log("NetManager - Registering The Channel");
                    //register this new socket
                    //val srcKeys = this.select.registerChannel(serverSocketSession);
                    val srcKeys = this.select.registerChannel(serverSocketSession);

                    Logger.log("NetManager - Getting Location To The Forwarding Location From Local Port");
                    //lookup where this is supposed to go
                    val localPort = serverSocketChannel.socket().localPort;
                    val addressPair = this.addressMapper.getPortMapping(localPort, "TCP");

                    Logger.log("NetManager - Creating Socket To The Forwarding Location");
                    //create a connection with where its supposed to go
                    val clientSocketChannel = NetLibrary.createClientSocket(addressPair!!.dest);
                    if(clientSocketChannel == null){
                        Logger.log("NetManager - Unable To Connect To Forwarding Server. Terminating Connection");

                        //close the socket
                        serverSocketSession.close();
                        //cancel the keys
                        srcKeys.cancel();

                        Logger.log("NetManager - Cleanup Complete. We Are Still Running");
                        continue;

                    //else the complete connection is successful. Register everything for flows
                    }else{

                        //register socket with select
                        val destKeys = this.select.registerChannel(clientSocketChannel!!);

                        Logger.log("NetManager - Generating Hashing Maps To Each Socket");
                        //hash the keys to eachothers sockets
                        addressMapper.createSocketMapping(srcKeys,clientSocketChannel);
                        addressMapper.createSocketMapping(destKeys,serverSocketSession);
                        //addressMapper.createSocketMapping(destKeys,serverSocketSession);

                        //statistics call
                        this.listener?.connectionOpened();
                    }



                }else if(this.select.hasDataToRead(key)){
                    Logger.log("NetManager - Found Data To Read");

                    //get the source socket - were gonna be balsy and cast
                    if(key.channel() is SocketChannel){

                        val dataSourceChannel = key.channel() as SocketChannel;
                        //use socket mapper to find the other socket that we need
                        val dataDestChannel = addressMapper.getSocketChannel(key);
                        if(dataDestChannel != null){
                            println(dataDestChannel);
                            val bytesTransferred = NetLibrary.transferDataFromChannels(dataSourceChannel, dataDestChannel);

                            listener?.bytesTransfered(dataSourceChannel.remoteAddress as InetSocketAddress, dataSourceChannel.socket().localPort, bytesTransferred)
                            if(bytesTransferred == -1){
                                Logger.log("NetManager - Negative Bytes Transferred. Connection Assumed Closed. Cleaning Up");

                                //Close Date Source Channel
                                dataSourceChannel.close();
                                //remove mappings so that they do no communicate with eachother
                                addressMapper.deleteKeyForChannel(dataDestChannel);
                                //cancel the key so it is removed from select
                                key.cancel();

                                //Close the Data Dest Channel
                                dataDestChannel.close();
                                addressMapper.deleteKeyForChannel(dataSourceChannel);
                                key.cancel();

                                //tell statistics of closed socket
                                this.listener?.connectionClosed();
                                continue;
                            }
                        }else{
                            throw NullPointerException("The dataDestSocket Returned Null!");
                        }




                    }else if(key.channel() is DatagramChannel){

                        val dataSourceChannel = key.channel() as DatagramChannel;
                        val dataSourceRemoteAddress = dataSourceChannel.remoteAddress;

                        val dataDestChannel = addressMapper.getDatagramChannel(dataSourceRemoteAddress);
                        if(dataDestChannel == null){

                            //means the mapping doesn't exist. assumed a new connection ?
                            val localPort = dataSourceChannel.socket().localPort;
                            val addressPair = this.addressMapper.getPortMapping(localPort, "UDP");

                            val dataDestChannel = NetLibrary.createUDPClientSocket(addressPair!!.dest);

                            //if we fail to connect to the forwarding location, closeup shop for this channel
                            if(dataDestChannel == null){
                                Logger.log("NetManager - Unable To Connect To UDP Forwarding Server. Terminating Connection");

                                //close the source socket
                                dataSourceChannel.close();
                                //cancel the source key
                                key.cancel();

                                Logger.log("NetManager - Cleanup Complete. We Are Still Running");
                                continue;

                            }else{

                                //val dataDestLocalAddress = dataDestChannel.socket().localSocketAddress;
                                //register with select
                                val destKeys = this.select.registerChannel(dataDestChannel);

                                //create mappings for future
                                this.addressMapper.createDatagramMapping(dataSourceRemoteAddress, dataDestChannel);
                                //this.addressMapper.createDatagramMapping(dataDestLocalAddress, dataSourceChannel);

                            }

                        }else{

                            //means the mapping is know and this is a known connection



                        }




                    }else{
                        throw IllegalArgumentException("NetManager - No Matching Channel Type For Key's Channel");
                    }



                }

                keyIterator.remove();

            }


        }
    }



}