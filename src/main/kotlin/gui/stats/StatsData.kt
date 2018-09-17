package gui.stats

import java.util.*

object StatsData{
    var totalConnections = 0

    var maxConnections = 0

    var currentConnections: Int = 0
        set(value){

            if(value > maxConnections){
                maxConnections = value
            }

            if(value < 0){
                field = 0
            }else{
                field = value
            }
        }

    var bytesSentLastSecond = 0
    var bytesForwarded = 0

    var bytesPerPortData = HashMap<String, Double>()

    var bytesPerConnectionData = HashMap<String, Double>()
}