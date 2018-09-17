package gui

import gui.stats.StatsUpdate
import tools.Logger
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

class AggregatorThread(private val bytes2AggregateQueue: BlockingQueue<StatsUpdate>):Runnable {


    private var keepRunning = true
    private var resetTriggered = false

    override fun run() {
        Logger.log("AggregatorThread - Running")
        while(keepRunning){
            val statUpdate = bytes2AggregateQueue.poll(5, TimeUnit.SECONDS)

            if(statUpdate != null){
                if(resetTriggered){
                    statUpdate.reset()
                    resetTriggered = false
                }
                statUpdate.update()
            }
        }
    }


    fun stop(){
        keepRunning = false
    }

    fun resetTime(){
        resetTriggered = true
    }




}