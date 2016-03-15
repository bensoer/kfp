package lib.net.rw

import java.nio.ByteBuffer

/**
 * Created by bensoer on 14/03/16.
 */

interface IReadWritableChannel{

    fun read(buffer: ByteBuffer):Int;

    fun write(buffer:ByteBuffer);
}