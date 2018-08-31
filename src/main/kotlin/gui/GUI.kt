package gui

import gui.stats.StatsUpdate
import gui.stats.BytesForwardedStatsUpdate
import gui.stats.ConnectionCountStatsUpdate

import javafx.application.Application
import javafx.application.Platform
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import tools.AddressPair
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.LinkedTransferQueue
import kotlin.concurrent.thread

private var _gui:GUI? = null
    set(value) = synchronized(GUI.Companion)
    {
        if (field != null) throw IllegalStateException("GUI should only be constructed once!")
        field = value
    }

val gui:GUI get() = _gui ?: throw IllegalStateException("must execute GUI.mainLoop before accessing the gui")

class GUI:Application()
{
    companion object
    {

        private val bytesToAggregatorQueue = LinkedTransferQueue<StatsUpdate>()
        private val aggregatorThread = AggregatorThread(bytesToAggregatorQueue)

        /**
         * main loop of the [GUI]. this function blocks when executed; beware!
         */
        val mainLoop =
            {
                Thread.currentThread().name = "guiLooper"
                Application.launch(GUI::class.java)

                aggregatorThread.stop()
                gui.executeOnListener({gui.listener?.exit()})
            }

        private val releasedOnApplicationStarted = CountDownLatch(1)

        fun awaitInitialized()
        {
            releasedOnApplicationStarted.await()
        }
    }

    init
    {
        _gui = this
    }

    /**
     * map of [AddressPair]s and their associated [ConnStats] displayed on the
     * [GUI]. just use it like a normal map, and the [GUI] will magically be
     * updated. modifying this map will not trigger [IListener] methods to be
     * called.
     */
    val addressPairs:ObservableSet<AddressPair> get() = forwardingPane.addressPairs

    private val forwardingPane:ForwardingPane by lazy {ForwardingPane()}

    private val statisticsPane:StatisticsPane by lazy {StatisticsPane()}



    /**
     * executed from [Application.launch]. sets up and displays the application
     * window.
     */
    override fun start(primaryStage:Stage)
    {
        // configure the stage (the window)
        primaryStage.title = "Port Forwarder"

        // configure the scene (inside the window)
        val borderPane = BorderPane()
        primaryStage.scene = Scene(borderPane,1024.0,480.0)
        primaryStage.minWidth = 1024.0
        primaryStage.minHeight = 480.0
        primaryStage.scene.stylesheets.add(CSS.FILE_PATH)

        borderPane.center = ScrollPane(forwardingPane)
        borderPane.bottom = statisticsPane

        // display the window
        primaryStage.show()

        // hook stuff up to each other: forwarding pane
        forwardingPane.listener = forwardingPaneListener

        // release count down latch...
        releasedOnApplicationStarted.countDown()


    }



    //do aggregate and thread handling here!

    //fun bytesForwarded(connection:InetSocketAddress,port:Int,numBytes:Int) = statisticsPane.bytesForwarded(connection,port,numBytes)
    fun bytesForwarded(connection:InetSocketAddress, port:Int, numBytes:Int){

        bytesToAggregatorQueue.add(BytesForwardedStatsUpdate(connection, port, numBytes))

    }

    //fun connectionOpened() = statisticsPane.connectionOpened()
    fun connectionOpened(){
        bytesToAggregatorQueue.add(ConnectionCountStatsUpdate(1))
    }

    //fun connectionClosed() = statisticsPane.connectionClosed()
    fun connectionClosed(){
        bytesToAggregatorQueue.add(ConnectionCountStatsUpdate(-1))
    }

    /**
     * elements in this set will be notified upon user interaction with [GUI].
     */
    var listener:IListener? = null

    private val listenerEventQueue = LinkedBlockingQueue<()->Unit>()

    private var listenerThread:Thread = Thread()

    private fun executeOnListener(function:()->Unit)
    {
        // enqueue functions into the event queue
        listenerEventQueue.add(function)

        // create the listener thread if it's not running
        if (!listenerThread.isAlive)
        {
            listenerThread = thread(name = "listenerThread")
            {
                while (true)
                {
                    val callback:(()->Unit)? = listenerEventQueue.poll()
                    if (callback != null)
                    {
                        callback()
                    }
                    else
                    {
                        return@thread
                    }
                }
            }
        }
    }

    /**
     * interface that [GUI] observers must implement to be notified by the [GUI]
     * upon user interaction.
     */
    interface IListener
    {
        /**
         * called when the user adds a new [AddressPair] to the [GUI].
         *
         * @return true if the persistence operation was successful; false
         * otherwise
         */
        fun insert(addressPair:AddressPair):Boolean

        /**
         * called when the user removes an existing [AddressPair] from the
         * [GUI].
         *
         * @return true if the persistence operation was successful; false
         * otherwise
         */
        fun delete(addressPair:AddressPair):Boolean

        /**
         * called when the [GUI]'s main thread is terminated.
         */
        fun exit()
    }

    private val forwardingPaneListener = object:ForwardingPane.IListener
    {
        override fun added(addressPair:AddressPair)
        {
            gui.executeOnListener()
            {
                if (listener?.insert(addressPair) == false)
                {
                    Platform.runLater()
                    {
                        val alert = Alert(Alert.AlertType.ERROR)
                        alert.title = "Port Forwarder"
                        alert.headerText = "Persistence Failure"
                        alert.contentText =
                            """OHHHH MY GAAWWWD!!! Failed to insert entry: "${addressPair.localPort} -> ${addressPair.dest}" into persistent storage; it will be removed from the address pair list.

You can try:
    • reentering the data
    • replacing your hard drive"""
                        alert.showAndWait()

                        addressPairs.remove(addressPair)
                    }
                }
            }
        }

        override fun removed(addressPair:AddressPair)
        {
            gui.executeOnListener()
            {
                // todo: check the return result
                if (listener?.delete(addressPair) == false)
                {
                    Platform.runLater()
                    {
                        val alert = Alert(Alert.AlertType.ERROR)
                        alert.title = "Port Forwarder"
                        alert.headerText = "Persistence Failure"
                        alert.contentText =
                            """OH NOOOOO! Failed to remove entry: "${addressPair.localPort} -> ${addressPair.dest}" from persistent storage; it will be re-inserted into the address pair list.

You can try:
    • removing the entry again
    • giving up"""
                        alert.showAndWait()

                        addressPairs.add(addressPair)
                    }
                }
            }
        }
    }
}
