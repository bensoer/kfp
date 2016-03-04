package gui

import com.sun.javafx.collections.ObservableMapWrapper
import javafx.application.Application
import javafx.collections.MapChangeListener
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import tools.AddressPair
import tools.ConnStats
import java.net.InetSocketAddress
import java.util.LinkedHashMap
import java.util.LinkedHashSet

private var gui:GUI? = null

class GUI
{
    /**
     * map of [AddressPair]s and their associated [ConnStats] displayed on the
     * [GUI]. just use it like a normal map, and the [GUI] will magically be
     * updated. modifying this map will not trigger [IListener] methods to be
     * called.
     */
    val addressPairs:MutableMap<AddressPair,ConnStats>
        get() = _addressPairs

    private val _addressPairs = ObservableMapWrapper(LinkedHashMap<AddressPair,ConnStats>())

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

        // add listener to map to update GUI upon map's modification
        _addressPairs.addListener(MapChangeListener()
        {
            change ->
            // update the GUI
        })
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
    /**
     * executed from [Application.launch]. sets up and displays the application
     * window.
     */
    override fun start(primaryStage:Stage)
    {
        // configure the stage (the window)
        primaryStage.title = "Port Forwarder"

        // configure the scene (inside the window)
        primaryStage.scene = Scene(PrimaryScene(),640.0,480.0)

        // display the window
        primaryStage.show()
    }
}

private const val ITEM_PADDING = 20.0

private class PrimaryScene:VBox(ITEM_PADDING)
{
    init
    {
        // position children in middle of layout
        alignment = Pos.CENTER

        // label
        val label = Label()
        children.add(label)

        // button "Click Me!"
        val b1 = Button()
        b1.text = "Click Me!"
        b1.setOnAction()
        {
            event ->
            label.text = "you clicked me!"
            gui?.listeners?.forEach {it.insert(AddressPair(InetSocketAddress(5),InetSocketAddress(5)))}
        }
        children.add(b1)

        // button "Don't Click Me!"
        val b2 = Button()
        b2.text = "Don't Click Me!"
        b2.setOnAction()
        {
            event ->
            label.text = "I told you not to click me!"
            gui?.listeners?.forEach {it.delete(AddressPair(InetSocketAddress(5),InetSocketAddress(5)))}
        }
        children.add(b2)
    }
}
