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

private var gui:GUI? = null
private var guiAddressPairs:MutableMap<AddressPair,ConnStats>? = null
private var window:Window? = null
private val releasedOnSetupFinished = CountDownLatch(1)

class GUI
{
    private val _addressPairs = Collections.synchronizedMap(LinkedHashMap<AddressPair,ConnStats>())

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
                window!!.forwardingPane.addressPairs.add(change.key)
            }
            else
            {
                window!!.forwardingPane.addressPairs.remove(change.key)
            }
        })
        return@run map
    }

    /**
     * main loop of the [GUI]. this function blocks when executed; beware!
     */
    val mainLoop = {Application.launch(Window::class.java)}

    /**
     * elements in this set will be notified upon user interaction with [GUI].
     */
    val listeners:MutableSet<IListener> = LinkedHashSet()

    init
    {
        // set the static reference to the gui so other classes in this file may
        // access it...
        gui = this
        guiAddressPairs = _addressPairs
    }

    fun awaitInitialized()
    {
        releasedOnSetupFinished.await()
    }

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

class Window:Application()
{
    val forwardingPane:ForwardingPane by lazy {ForwardingPane()}

    init
    {
        window = this
    }

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
                guiAddressPairs!!.put(addressPair,ConnStats(0))
                gui!!.listeners.forEach {it.insert(addressPair)}
            }

            override fun removed(addressPair:AddressPair)
            {
                guiAddressPairs!!.remove(addressPair)
                gui!!.listeners.forEach {it.delete(addressPair)}
            }
        }

        releasedOnSetupFinished.countDown()
    }
}
