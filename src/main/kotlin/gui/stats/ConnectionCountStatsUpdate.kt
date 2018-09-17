package gui.stats

class ConnectionCountStatsUpdate(private val connectionDifference:Int):StatsUpdate {

    private var resetTriggered = false

    override fun update() {

        if(resetTriggered){
            StatsData.currentConnections = connectionDifference
            resetTriggered = true
        }else{
            StatsData.currentConnections += connectionDifference

            if(connectionDifference > 0){
                StatsData.totalConnections += connectionDifference
            }
        }
    }

    override fun reset(){
        resetTriggered = true
    }
}