package lib.net

import tools.Logger
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.Charset


/**
 * Created by bensoer on 28/02/16.
 */

/**
 * NetLibrary is a helper library class with a number fo generic static methods for helping create SocketChannels
 * ServerSocketChannels and read/write/transfer data between these given channels
 */
class NetLibrary{

    /** Allows for Static Access **/
    companion object{
        /**
         * lowest port number on a typical machine.
         */
        val MIN_PORT:Int = 0x0000

        /**
         * highest port number on a typical machine.
         */
        val MAX_PORT:Int = 0xFFFF

        /**
         * createClientSocket creates a SocketChannel from the passed in hostNAme and portNumber parameters.
         * After which it then attempts with this channel to connect to the host passed. If the connection
         * fails this method returns null. On success, the SocketChannel is returned
         */
        fun createClientSocket(hostName:String, portNumber:Int): SocketChannel? {
            Logger.log("NetLibrary - Attemping to connect to $hostName on port $portNumber");
            try{

                val address = InetSocketAddress(hostName, portNumber);
                val channel: SocketChannel = SocketChannel.open(address);

                while(!channel.finishConnect()){
                    //we wait boys
                }

                return channel;

            }catch(uhe: UnknownHostException){
                Logger.log("NetLibrary - Unable To resolve Host Of: $hostName");
                uhe.printStackTrace();
                return null;

            }catch(ioe: IOException){
                Logger.log("NetLibrary - Unable To Access IO");
                ioe.printStackTrace();
                return null;
            }
        }

        /**
         * createClientSocket creates a SocketChannel using the passed in InetSocketAddress. It then attempts
         * to connect to the host the passed in address referres to. If the connection fails, this method returns
         * null. If it is successful, this method will return the SocketChannel
         */
        fun createClientSocket(address:InetSocketAddress): SocketChannel? {
            Logger.log("NetLibrary - Attemping to connect to ${address.hostString} on port ${address.port}");
            try{


                val channel: SocketChannel = SocketChannel.open(address);

                while(!channel.finishConnect()){
                    //we wait boys
                }

                return channel;

            }catch(uhe: UnknownHostException){
                Logger.log("NetLibrary - Unable To resolve Host Of: ${address.hostName}");
                uhe.printStackTrace();
                return null;

            }catch(ioe: IOException){
                Logger.log("NetLibrary - Unable To Access IO");
                ioe.printStackTrace();
                return null;
            }
        }

        /**
         * createServerSocket creates a ServerSocketChannel using the passed in port number to bind to. If the
         * port is unable to be bound to, the function will return null. If it is successful it will return the
         * ServerSocketChannel
         */
        fun createServerSocket(portNumber: Int): ServerSocketChannel? {
            Logger.log("NetLibrary - Attempting to Create A Server Socket on port $portNumber");

            try{

                //create an address of here
                val localAddress = InetSocketAddress("localhost", portNumber);
                //create a channel
                val channel = ServerSocketChannel.open();

                //enable reuse of address
                val socketOption = StandardSocketOptions.SO_REUSEADDR;
                channel.setOption(socketOption, true);

                //bind the address
                channel.bind(localAddress);

                //return the address
                return channel;

            }catch(ioe: IOException){
                Logger.log("NetLibrary - Unable To Access IO");
                ioe.printStackTrace();
                return null;
            }
        }

        /**
         * readFromSocket is a helper method that reads data from the passed in SocketChannel using the passed
         * in ByteBuffer. The ByteBuffer is responsible for specifying how much to read from the channel before
         * returning it. The read function will hang until data is read from the channel. After reading from the
         * channel readFromSocket will return a SocketRead data object containing the ByteBuffer in read mode, and
         * the number of bytes that were read from the channel
         */
        fun readFromSocket(channel: SocketChannel, buffer: ByteBuffer): SocketRead{

            //read in all to fill up the buffer ?
            val bytesRead:Int = channel.read(buffer);

            //flip it cuz why not
            buffer.flip();

            //convert it to a string for this example ?
            /*val bytes: ByteArray = buffer.array();
            val string:String = String(bytes, Charset.forName("UTF-8"));

            buffer.clear();
            return string;*/

            return SocketRead(buffer, bytesRead);
        }

        /**
         * writeToSocket writes the content from the passed in ByteBuffer into the passed in SocketChannel.
         * The method will block until all data in the buffer has been written. Note this method assumes the passed
         * in ByteBuffer is in read mode
         */
        fun writeToSocket(channel: SocketChannel, buffer: ByteBuffer){

            //buffer.flip();
            while(buffer.hasRemaining()){
                channel.write(buffer);
            }
        }

        /**
         * transferDataFromChannels is a helper method that transfers data from the passed in sourceChannel
         * to the destinationChannel. The method supplies its own ByteBuffer with 1024 bytes of space in which
         * it will read from the sourceChannel until empty or the ByteBuffer is full and then write it all to the
         * destinationChannel. This will only happen once ina  single call to the transferDataFromChannels method
         */
        fun transferDataFromChannels(sourceChannel: SocketChannel, destinationChannel: SocketChannel):Int{
            Logger.log("NetLibrary - Transfering Data From Channels")

            var buffer:ByteBuffer = ByteBuffer.allocate(1024);
            val readOut = NetLibrary.readFromSocket(sourceChannel,buffer);

            Logger.log("NetLibrary - Read In From Source Stream. Go This Data");
            println(readOut);

            //Logger.log("NetLibrary - Now Sending it To ${remoteAddress!!.}")
            NetLibrary.writeToSocket(destinationChannel, readOut.data);

            return readOut.bytesRead;

        }
    }

}

