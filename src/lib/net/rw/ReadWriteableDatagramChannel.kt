package lib.net.rw

import lib.net.rw.IReadWritableChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * Created by bensoer on 14/03/16.
 */

class ReadWriteableDatagramChannel(private val datagramChannel: DatagramChannel): IReadWritableChannel {

    private var socketAddress:InetSocketAddress? = null;

    override fun read(buffer: ByteBuffer):Int {
        this.socketAddress = datagramChannel.receive(buffer) as InetSocketAddress;
        return buffer.position();
    }

    override fun write(buffer: ByteBuffer) {
        datagramChannel.write(buffer);
    }

    override fun isDatagram():Boolean{
        return true;
    }

    override fun getSourceAddress():InetSocketAddress?{
        return this.socketAddress;
    }



}