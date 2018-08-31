package gui.stats

class ConnectionCountStatsUpdate(private val connectionDifference):StatsUpdate {

    override fun update() {
        StatsData.currentConnections += connectionDifference
    }
}