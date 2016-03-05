package lib.net

import java.nio.ByteBuffer

/**
 * Created by bensoer on 04/03/16.
 */

data class SocketRead(val data: ByteBuffer, val bytesRead:Int);