package gui

import com.sun.javafx.collections.ObservableMapWrapper
import javafx.application.Application
import javafx.collections.MapChangeListener
import javafx.scene.Scene
import javafx.stage.Stage
import tools.AddressPair
import tools.ConnStats
import java.util.Collections
import java.util.LinkedHashMap
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
        val mainLoop = {Application.launch(GUI::class.java)}

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

    private val _addressPairs:MutableMap<AddressPair,ConnStats> = Collections.synchronizedMap(LinkedHashMap<AddressPair,ConnStats>())

    /**
     * map of [AddressPair]s and their associated [ConnStats] displayed on the
     * [GUI]. just use it like a normal map, and the [GUI] will magically be
     * updated. modifying this map will not trigger [IListener] methods to be
     * called.
     */
    val addressPairs:MutableMap<AddressPair,ConnStats> = run()
    {
        val map = ObservableMapWrapper(_addressPairs)
        map.addListener(MapChangeListener()
        {
            change ->
            if (change.wasAdded())
            {
                forwardingPane.addressPairs.add(change.key)
            }
            else
            {
                forwardingPane.addressPairs.remove(change.key)
            }
        })
        return@run map
    }

    val forwardingPane:ForwardingPane by lazy {ForwardingPane()}

    /**
     * executed from [Application.launch]. sets up and displays the application
     * window.
     */
    override fun start(primaryStage:Stage)
    {
        // configure the stage (the window)
        primaryStage.title = "Port Forwarder"

        // configure the scene (inside the window)
        primaryStage.scene = Scene(forwardingPane,640.0,480.0)
        primaryStage.scene.stylesheets.add(CSS.FILE_PATH)

        // display the window
        primaryStage.show()

        // hook stuff up to each other
        forwardingPane.listener = object:ForwardingPane.Listener
        {
            override fun added(addressPair:AddressPair)
            {
                _addressPairs.put(addressPair,ConnStats(0))
                listeners.forEach {it.insert(addressPair)}
            }

            override fun removed(addressPair:AddressPair)
            {
                _addressPairs.remove(addressPair)
                listeners.forEach {it.delete(addressPair)}
            }
        }

        releasedOnApplicationStarted.countDown()
    }

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
    }
}
