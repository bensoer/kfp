package gui

import javafx.application.Application
import javafx.collections.ObservableSet
import javafx.collections.SetChangeListener
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import tools.AddressPair
import java.net.InetSocketAddress
import java.util.LinkedHashSet
import java.util.concurrent.CountDownLatch

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

        /**
         * main loop of the [GUI]. this function blocks when executed; beware!
         */
        val mainLoop =
            {
                Application.launch(GUI::class.java)
                gui.listeners.forEach {it.exit()}
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
    // todo: hook up with backend
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
        primaryStage.scene = Scene(borderPane,900.0,480.0)
        primaryStage.scene.stylesheets.add(CSS.FILE_PATH)

        borderPane.center = ScrollPane(forwardingPane)
        borderPane.bottom = statisticsPane

        // display the window
        primaryStage.show()

        // hook stuff up to each other: forwarding pane
        forwardingPane.listener = forwardingPaneListener

        // hook stuff up to each other: user
        addressPairs.addListener(SetChangeListener()
        {
            change ->
            if (change.wasAdded())
            {
                forwardingPane.addressPairs.add(change.elementAdded)
            }
            else
            {
                forwardingPane.addressPairs.remove(change.elementRemoved)
            }
        })

        // release count down latch...
        releasedOnApplicationStarted.countDown()
    }

    // todo: hook up with backend
    fun bytesForwarded(connection:InetSocketAddress,port:Int,numBytes:Int) = statisticsPane.bytesForwarded(connection,port,numBytes)

    // todo: hook up with backend
    fun connectionOpened() = statisticsPane.connectionOpened()

    // todo: hook up with backend
    fun connectionClosed() = statisticsPane.connectionClosed()

    /**
     * elements in this set will be notified upon user interaction with [GUI].
     */
    val listeners:MutableSet<IListener> = LinkedHashSet()

    /**
     * interface that [GUI] observers must implement to be notified by the [GUI]
     * upon user interaction.
     */
    interface IListener
    {
        /**
         * called when the user adds a new [AddressPair] to the [GUI].
         */
        fun insert(addressPair:AddressPair)

        /**
         * called when the user removes an existing [AddressPair] from the
         * [GUI].
         */
        fun delete(addressPair:AddressPair)

        /**
         * called when the [GUI]'s main thread is terminated.
         */
        fun exit()
    }

    private val forwardingPaneListener = object:ForwardingPane.Listener
    {
        override fun added(addressPair:AddressPair)
        {
            listeners.forEach {it.insert(addressPair)}
        }

        override fun removed(addressPair:AddressPair)
        {
            listeners.forEach {it.delete(addressPair)}
        }
    }
}
