package gui

import gui.stats.StatsUpdate
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class AggregatorThread(private val bytes2AggregateQueue: BlockingQueue<StatsUpdate>):Runnable {


    private var keepRunning = true

    override fun run() {
        while(keepRunning){
            val statUpdate = bytes2AggregateQueue.poll(5, TimeUnit.SECONDS)
            statUpdate?.update()
        }
    }


    fun stop(){
        keepRunning = false
    }




}