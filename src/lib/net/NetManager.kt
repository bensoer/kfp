package lib.net

import gui.GUI
import javafx.collections.ObservableMap
import lib.AddressMapper
import tools.AddressPair
import tools.Logger
import java.net.Socket
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

    fun setObservableMap(){

    }

    //start the fun boys
    override fun run(){

        val allKnownPorts = this.addressMapper.getAllPortMappings();
        val iterator = allKnownPorts.iterator();

        //iterate over all persisted addresses
        while(iterator.hasNext()){
            val addressPair = iterator.next();

            //create a listening socket for each one
            val socketChannel = NetLibrary.createServerSocket(addressPair.localPort);

            //if it succeeded register it with select
            if(socketChannel != null) {
                this.select.registerServerChannel(socketChannel!!);
            }
        }

        //wait for something to happen
        while(true){
            Logger.log("Waiting For Events");
            val numberOfEvents = this.select.waitForEvent()
            Logger.log("Back From Events");

            val readyChannelKeys = this.select.getReadyChannels();
            val keyIterator = readyChannelKeys.iterator();

            while(keyIterator.hasNext()){
                val key = keyIterator.next();

                if(this.select.isANewConnection(key)){

                    //get the channel
                    val channel = this.select.getChannelForKey(key);
                    //downcast it since it can accept. It's a ServerSocketChannel ?
                    val serverSocketChannel = channel as ServerSocketChannel;

                    //accept the connection
                    Logger.log("Now Accepting Connection");
                    //serverSocketChannel.configureBlocking(true);
                    val serverSocketSession = serverSocketChannel.accept();
                    Logger.log("Connection Accepted");
                    serverSocketChannel.configureBlocking(false);


                    //register this new socket
                    //val srcKeys = this.select.registerChannel(serverSocketSession);
                    val srcKeys = this.select.registerChannel(serverSocketChannel);

                    //lookup where this is supposed to go
                    val localPort = serverSocketChannel.socket().localPort;
                    val addressPair = this.addressMapper.getPortMapping(localPort);

                    //create a connection with where its supposed to go
                    val clientSocketChannel = NetLibrary.createClientSocket(addressPair!!.dest);
                    //register socket with select
                    val destKeys = this.select.registerChannel(clientSocketChannel!!);

                    //hash the keys to eachothers sockets
                    addressMapper.createSocketMapping(srcKeys,clientSocketChannel);
                    //addressMapper.createSocketMapping(destKeys,serverSocketSession);
                    addressMapper.createSocketMapping(destKeys,serverSocketSession!!);


                }else if(this.select.hasDataToRead(key)){

                    //get the source socket - were gonna be balsy and cast
                    val dataSourceChannel = key.channel() as SocketChannel;

                    //use socket mapper to find the other socket that we need
                    val dataDestSocket = addressMapper.getSocketChannel(key);
                    if(dataDestSocket != null){
                        NetLibrary.transferDataFromChannels(dataSourceChannel, dataDestSocket);
                    }else{
                        throw NullPointerException("The dataDestSocket Returned Null!");
                        break;
                    }

                }
            }


        }
    }



}