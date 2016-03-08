package lib.net

import gui.GUI
import javafx.collections.ObservableMap
import lib.AddressMapper
import tools.AddressPair
import tools.Logger
import java.net.Socket
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

/**
 * Created by bensoer on 04/03/16.
 */

class NetManager(val addressMapper: AddressMapper): Thread(){


    private val select:Select;

    init {
        this.select = Select();
    }

    fun addMapping(addressPair:AddressPair):Boolean{

        val serverSocketChannel = NetLibrary.createServerSocket(addressPair.localPort);
        if(serverSocketChannel == null){
            Logger.log("NetManager - ERROR - Failed To Create A Server Socket For New Port");
            return false;
        }else{
            this.select.registerServerChannel(serverSocketChannel);
            this.addressMapper.addPortMapping(addressPair);
            return true;
        }
    }

    fun removeMapping(addressPair:AddressPair):Boolean{
        val keysSet = this.select.getAllKeys();

        val iterator = keysSet.iterator();
        while(iterator.hasNext()){
            val key = iterator.next();

            if(key.isAcceptable){
                val channel = this.select.getChannelForKey(key) as ServerSocketChannel;
                val listeningPort = channel.socket().localPort

                //if this ServerSocket is listening on the same port then we know to delete it
                if(listeningPort == addressPair.localPort){

                    //close the channel
                    channel.close();
                    //cancel the key
                    key.cancel();
                    //remove from iterator ?
                    iterator.remove();

                    //remove from database
                    this.addressMapper.deletePortMapping(addressPair);

                    return true;
                }
            }
        }

        return false;

    }

    //start the fun boys
    override fun run(){

        Logger.log("NetManager - Creating Listener Sockets For All Known Mappings");

        val allKnownPorts = this.addressMapper.getAllPortMappings();
        val iterator = allKnownPorts.iterator();

        //iterate over all persisted addresses
        while(iterator.hasNext()){
            val addressPair = iterator.next();

            //create a listening socket for each one
            val socketChannel = NetLibrary.createServerSocket(addressPair.localPort);

            //if it succeeded register it with select
            if(socketChannel != null) {
                this.select.registerServerChannel(socketChannel);
            }else{
                Logger.log("NetManager - ERROR - Failed To Create A Server Socket For New Port");
            }
        }

        //wait for something to happen
        while(true){
            Logger.log("Waiting For Events");
            val numberOfEvents = this.select.waitForEvent()
            Logger.log("Back From Events");

            val readyChannelKeys = this.select.getReadyChannels();
            val keyIterator = readyChannelKeys.iterator() as MutableIterator<SelectionKey>;

            while(keyIterator.hasNext()){
                val key = keyIterator.next();

                if(!key.isValid){
                    Logger.log("NetManager - Invalid Key Detected. Assumed Cancelled Connection. Not Processing");
                    continue;
                }

                if(this.select.isANewConnection(key)){
                    Logger.log("NetManager - Found A New Connections");

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

                    Logger.log("NetManager - Creating Socket To The Forwarding Location");
                    //lookup where this is supposed to go
                    val localPort = serverSocketChannel.socket().localPort;
                    val addressPair = this.addressMapper.getPortMapping(localPort);

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
                    }



                }else if(this.select.hasDataToRead(key)){
                    Logger.log("NetManager - Found Data To Read");

                    //get the source socket - were gonna be balsy and cast
                    val dataSourceChannel = key.channel() as SocketChannel;


                    //use socket mapper to find the other socket that we need
                    val dataDestChannel = addressMapper.getSocketChannel(key);
                    if(dataDestChannel != null){
                        println(dataDestChannel);
                        val bytesTransferred = NetLibrary.transferDataFromChannels(dataSourceChannel, dataDestChannel);

                        if(bytesTransferred == -1){
                            Logger.log("NetManager - Negative Bytes Transferred. Connection Assumed Closed. Cleaning Up");

                            dataSourceChannel.close();
                            //remove mappings so that they do no communicate with eachother
                            addressMapper.deleteKeyForChannel(dataDestChannel);
                            //cancel the key so it is removed from select
                            key.cancel();
                            continue;
                        }
                    }else{
                        throw NullPointerException("The dataDestSocket Returned Null!");
                        break;
                    }

                }

                keyIterator.remove();

            }


        }
    }



}