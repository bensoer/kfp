package gui

import tools.AddressPair
import tools.ConnStats
import java.net.InetSocketAddress
import kotlin.concurrent.thread


/**
 * small example of running the gui on the current thread.
 */
fun main(args: Array<String>)
{
    // instantiate the GUI
    val gui = GUI()

    // adds a listener to GUI so we will be notified when user interacts with it
    gui.listeners.add(listener)

    // runs the GUI's main loop on this thread. this causes the window to be
    // created and displayed and stuff
    thread {gui.mainLoop()}

    gui.awaitInitialized()
    gui.addressPairs.put(AddressPair(80,InetSocketAddress("steve",800)),ConnStats(0))
}

/**
 * [GUI] observer. notified upon user interaction with the [GUI].
 */
private val listener = object:GUI.IListener
{
    override fun insert(addressPair:AddressPair)
    {
        println("insert($addressPair)")
    }

    override fun delete(addressPair:AddressPair)
    {
        println("delete($addressPair)")
    }
}
