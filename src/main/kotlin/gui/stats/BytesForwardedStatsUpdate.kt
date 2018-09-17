package gui.stats

import java.net.InetSocketAddress

class BytesForwardedStatsUpdate(private val connection: InetSocketAddress,
                                private val port:Int,
                                private val numBytes:Int): StatsUpdate {

    private var resetTriggered = false

    override fun update() {
        StatsData.bytesForwarded += numBytes

        //calculate the pie data
        if(StatsData.bytesPerPortData.containsKey(port.toString()) && !resetTriggered){
            val currentBytes = StatsData.bytesPerPortData.getOrDefault(port.toString(), 0.0)
            StatsData.bytesPerPortData[port.toString()] = (currentBytes + numBytes)
        }else{
            StatsData.bytesPerPortData[port.toString()] = numBytes.toDouble()
            if(resetTriggered){
                resetTriggered = false
            }
        }

        if(StatsData.bytesPerConnectionData.containsKey("${connection.hostString}:${connection.port}") && !resetTriggered){
            val bytesPerConnection = StatsData.bytesPerConnectionData.getOrDefault("${connection.hostString}:${connection.port}", 0.0)
            StatsData.bytesPerConnectionData["${connection.hostString}:${connection.port}"] = (bytesPerConnection + numBytes)
        }else{
            StatsData.bytesPerConnectionData["${connection.hostString}:${connection.port}"] = numBytes.toDouble()
            if(resetTriggered){
                resetTriggered = false
            }
        }

    }

    override fun reset(){
        resetTriggered = true
    }
}