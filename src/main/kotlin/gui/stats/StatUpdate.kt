package gui.stats

import java.net.InetSocketAddress

data class StatUpdate(val connection: InetSocketAddress, val port:Int, val numBytes:Int)