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

class NetLibrary{

    companion object{
        /**
         * lowest port number on a typical machine.
         */
        val MIN_PORT:Int = 0x0000

        /**
         * highest port number on a typical machine.
         */
        val MAX_PORT:Int = 0xFFFF

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

        fun writeToSocket(channel: SocketChannel, buffer: ByteBuffer){

            //buffer.flip();
            while(buffer.hasRemaining()){
                channel.write(buffer);
            }
        }

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

