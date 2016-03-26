package lib.net

import com.sun.javaws.exceptions.InvalidArgumentException
import com.sun.xml.internal.fastinfoset.util.StringArray
import gui.GUI
import javafx.collections.ObservableMap
import lib.net.AddressMapper
import lib.net.rw.ReadWriteableDatagramChannel
import tools.AddressPair
import tools.Logger
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import lib.net.rw.ReadWriteableSocketChannel
import java.nio.ByteBuffer

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

        if(addressPair.type.equals("UDP")){


            val serverSocketChannel = NetLibrary.createUDPServerSocket(addressPair.localPort);
            if(serverSocketChannel == null){
                Logger.log("NetManager - ERROR - Failed To Create A UDP Server Socket For New Port");
                return false;
            }else{
                this.addressMapper.addPortMapping(addressPair);
                this.select.registerChannel(serverSocketChannel);
                Logger.log("NetManager - UDP Port Mapping Add Complete");
                return true;
            }

        }else if(addressPair.type.equals("TCP")){

            val serverSocketChannel = NetLibrary.createServerSocket(addressPair.localPort);
            if(serverSocketChannel == null){
                Logger.log("NetManager - ERROR - Failed To Create A Server Socket For New Port");
                return false;
            }else{
                this.addressMapper.addPortMapping(addressPair);
                this.select.registerServerChannel(serverSocketChannel);
                Logger.log("NetManager - TCP Port Mapping Add Complete");
                return true;
            }

        }else{
            Logger.log("NetManager - Requested Mapping Does Not Use An Accepted Protocol Type. Can't Create Mapping");
            return false;
        }

    }

    fun removeMapping(addressPair:AddressPair):Boolean{
        val keysSet = this.select.getAllKeys();

        val iterator = keysSet.iterator();
        while(iterator.hasNext()){
            val key = iterator.next();

            if(this.select.getChannelForKey(key) is ServerSocketChannel){
                Logger.log("NetManager:removeMapping - Found ServerSocketChannel to Remove");
                val channel = this.select.getChannelForKey(key) as ServerSocketChannel;
                val listeningPort = channel.socket().localPort

                //if this ServerSocket is listening on the same port then we know to delete it
                if(listeningPort == addressPair.localPort){
                    Logger.log("NetManager:removeMapping - Found Matching Local Ports");

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
            }else if(this.select.getChannelForKey(key) is DatagramChannel){
                Logger.log("NetManager:removeMapping - Found DatagramChannel to Remove");
                val channel = this.select.getChannelForKey(key) as DatagramChannel;
                val listeningPort = channel.socket().localPort;

                //if this DatagramSocket is listening on the same port then we know to delete it
                if(listeningPort == addressPair.localPort){
                    Logger.log("NetManager:removeMapping - Found Matching Local Ports");
                    //close the channel
                    channel.close();
                    //cancel the key
                    key.cancel();

                    //remove port mapping from database
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

            if(key.channel() is SocketChannel){
                val channel = key.channel() as SocketChannel;
                channel.close();

            }else if(key.channel() is DatagramChannel){
                val channel = key.channel() as DatagramChannel;
                channel.close();
            }else{
                Logger.log("NetManager - ERROR - An Invalid Channel Was Detected During Termination. Unable To" +
                        " Properly Close It.");
            }


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
                        keyIterator.remove();
                        continue;
                    }
                    Logger.log("NetManager - Connection Accepted");


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

                        keyIterator.remove();

                        Logger.log("NetManager - Cleanup Complete. We Are Still Running");
                        continue;

                    //else the complete connection is successful. Register everything for flows
                    }else{

                        Logger.log("NetManager - Registering The Channel");
                        //register this new socket
                        val srcKeys = this.select.registerChannel(serverSocketSession);

                        //register socket with select
                        val destKeys = this.select.registerChannel(clientSocketChannel);

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
                        Logger.log("NetManager - Data Is From A SocketChannel");

                        val dataSourceChannel = key.channel() as SocketChannel;
                        //use socket mapper to find the other socket that we need
                        val dataDestChannel = addressMapper.getSocketChannel(key);
                        if(dataDestChannel != null){
                            val bytesTransferred = NetLibrary.transferDataFromChannels(ReadWriteableSocketChannel(dataSourceChannel), ReadWriteableSocketChannel(dataDestChannel));

                            listener?.bytesTransfered(dataSourceChannel.remoteAddress as InetSocketAddress, dataSourceChannel.socket().localPort, bytesTransferred)
                            if(bytesTransferred == -1){
                                Logger.log("NetManager - Negative TCP Bytes Transferred. Connection Assumed Closed. Cleaning Up");

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

                                keyIterator.remove();

                                //tell statistics of closed socket
                                this.listener?.connectionClosed();
                                continue;
                            }
                        }else{
                            throw NullPointerException("NetworkManager - The TCP dataDestSocket Returned Null!");
                        }




                    }else if(key.channel() is DatagramChannel){
                        Logger.log("NetManager - Data Is From A DatagramChannel");

                        val dataSourceChannel = key.channel() as DatagramChannel;
                        val rwDataSourceChannel = ReadWriteableDatagramChannel(dataSourceChannel);

                        val socketRead = NetLibrary.readFromSocket(rwDataSourceChannel, ByteBuffer.allocate(2048));

                        val dataSourceRemoteAddress = socketRead.sourceAddress;

                        //we gotta check whether a mapping exists in a couple places
                        //is this mapping going from C -> PF -> S ?
                        var dataDestChannel:DatagramChannel? = addressMapper.getDatagramChannel(dataSourceRemoteAddress!!);
                        if(dataDestChannel == null){
                            //or is this a mapping going from S -> PF -> C ?
                            dataDestChannel = addressMapper.getUDPMapping(dataSourceChannel);
                        }

                        //if there is still no mapping. then this must be new
                        if(dataDestChannel == null){

                            //means the mapping doesn't exist. assumed a new connection ?

                            //connect it back to the host so we can auto respond properly
                            dataSourceChannel.connect(dataSourceRemoteAddress);

                            val localPort = dataSourceChannel.socket().localPort;
                            val addressPair = this.addressMapper.getPortMapping(localPort, "UDP");

                            dataDestChannel = NetLibrary.createUDPClientSocket(addressPair!!.dest);

                            //if we fail to connect to the forwarding location, closeup shop for this channel
                            if(dataDestChannel == null){
                                Logger.log("NetManager - Unable To Connect To UDP Forwarding Server. Terminating Connection");

                                //close the source socket
                                dataSourceChannel.close();
                                //cancel the source key
                                key.cancel();

                                keyIterator.remove();

                                Logger.log("NetManager - Cleanup Complete. We Are Still Running");
                                continue;

                            }else{

                                //val dataDestLocalAddress = dataDestChannel.socket().localSocketAddress;
                                //register with select
                                val destKeys = this.select.registerChannel(dataDestChannel);

                                //create mappings for future
                                this.addressMapper.createDatagramMapping(dataSourceRemoteAddress, dataDestChannel);

                                this.addressMapper.createUDPMapping(dataDestChannel,dataSourceChannel);
                                //this.addressMapper.createDatagramMapping(dataDestLocalAddress, dataSourceChannel);

                                this.listener?.connectionOpened();

                                //because this is a new UDP channel, its stateless and thus this packet has data to
                                //be transfered aswell. forward this packet along
                                NetLibrary.writeToSocket(ReadWriteableDatagramChannel(dataDestChannel), socketRead.data);
                                val bytesTransfered = socketRead.bytesRead;
                                listener?.bytesTransfered(dataSourceChannel.remoteAddress as InetSocketAddress, dataSourceChannel.socket().localPort, bytesTransfered);
                                if(bytesTransfered == -1){
                                    Logger.log("NetManager - Negative UDP Bytes Transferred. Connection Assumed Closed. Cleaning Up");
                                    //transfer is done, disconnect

                                    //close the source channel
                                    this.addressMapper.deleteUDPMappings(dataSourceChannel);
                                    dataSourceChannel.close();
                                    key.cancel();

                                    //close the destination channel
                                    this.addressMapper.deleteUDPMappings(dataDestChannel);
                                    dataDestChannel.close();

                                    this.listener?.connectionClosed();
                                }

                            }

                        }else{

                            //means the mapping is known and this is a known connection

                            //val bytesTransfered = NetLibrary.transferDataFromChannels(ReadWriteableDatagramChannel(dataSourceChannel), ReadWriteableDatagramChannel(dataDestChannel));

                            NetLibrary.writeToSocket(ReadWriteableDatagramChannel(dataDestChannel), socketRead.data);
                            val bytesTransfered = socketRead.bytesRead;
                            listener?.bytesTransfered(dataSourceChannel.remoteAddress as InetSocketAddress, dataSourceChannel.socket().localPort, bytesTransfered);
                            if(bytesTransfered == -1){
                                Logger.log("NetManager - Negative UDP Bytes Transferred. Connection Assumed Closed. Cleaning Up");
                                //transfer is done, disconnect

                                //close the source channel
                                this.addressMapper.deleteUDPMappings(dataSourceChannel);
                                dataSourceChannel.close();
                                key.cancel();

                                //close the destination channel
                                this.addressMapper.deleteUDPMappings(dataDestChannel);
                                dataDestChannel.close();

                                this.listener?.connectionClosed();

                            }
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