package lib.net

import java.nio.ByteBuffer

/**
 * Created by bensoer on 04/03/16.
 */

/**
 * SocketRead is a data object class representing a Read of data from a given socket. It holds the ByteBuffer
 * containing the data read and an Int representing the number of bytes read from the socket read function. If
 * the socket was terminated, the bytesRead value should be -1
 */
data class SocketRead(val data: ByteBuffer, val bytesRead:Int);