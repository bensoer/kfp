package gui.stats

import java.net.InetSocketAddress

class BytesForwardedStatsUpdate(private val connection: InetSocketAddress,
                                private val port:Int,
                                private val numBytes:Int): StatsUpdate {

    override fun update() {
        StatsData.bytesForwarded += numBytes
    }
}