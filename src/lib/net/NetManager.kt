package lib.net

import gui.GUI
import javafx.collections.ObservableMap
import lib.AddressMapper
import tools.AddressPair
import java.nio.channels.ServerSocketChannel

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
                this.select.registerChannel(socketChannel!!);
            }
        }

        //wait for something to happen
        while(true){
            val numberOfEvents = this.select.waitForEvent()

            val readyChannelKeys = this.select.getReadyChannels();
            val keyIterator = readyChannelKeys.iterator();

            while(keyIterator.hasNext()){
                val key = keyIterator.next();

                if(this.select.isANewConnection(key)){

                    //get the channel
                    val channel = this.select.getChannelForKey(key);
                    //downcast it since it can accept
                    val serverSocketChannel = channel as ServerSocketChannel;
                    //accept the connection
                    val socket = serverSocketChannel.accept();

                    //lookup where this is supposed to go
                    val localPort = serverSocketChannel.socket().localPort;
                    val addressPair = this.addressMapper.getPortMapping(localPort);

                    //create a connection with where its supposed to go

                    //register socket with select

                    //hash the keys to eachothers sockets


                }else if(this.select.hasDataToRead(key)){




                }
            }


        }
    }



}