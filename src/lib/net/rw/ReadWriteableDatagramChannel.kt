package lib.net.rw

import lib.net.rw.IReadWritableChannel
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * Created by bensoer on 14/03/16.
 */

class ReadWriteableDatagramChannel(private val datagramChannel: DatagramChannel): IReadWritableChannel {

    override fun read(buffer: ByteBuffer):Int {
        return datagramChannel.read(buffer);
    }

    override fun write(buffer: ByteBuffer) {
        datagramChannel.write(buffer);
    }


}