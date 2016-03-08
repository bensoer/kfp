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
    // runs the GUI's main loop on this thread. this causes the window to be
    // created and displayed and stuff
    thread {GUI.mainLoop()}
    GUI.awaitInitialized()

    // adds a listener to GUI so we will be notified when user interacts with it
    gui.listeners = listener

    // add address pairs to GUI
    gui.addressPairs.add(AddressPair(80,InetSocketAddress("localhost",800)))
    gui.addressPairs.add(AddressPair(81,InetSocketAddress("localhost",800)))
    gui.addressPairs.add(AddressPair(82,InetSocketAddress("localhost",800)))
    gui.addressPairs.add(AddressPair(83,InetSocketAddress("localhost",800)))
    gui.addressPairs.add(AddressPair(84,InetSocketAddress("localhost",800)))
    gui.addressPairs.add(AddressPair(85,InetSocketAddress("localhost",800)))

    // remove all address pair after a bit...
    Thread.sleep(10000)
    gui.addressPairs.clear()
}

/**
 * [GUI] observer. notified upon user interaction with the [GUI].
 */
private val listener = object:GUI.IListener
{
    override fun insert(addressPair:AddressPair):Boolean
    {
        println("insert($addressPair)")
        return true
    }

    override fun delete(addressPair:AddressPair):Boolean
    {
        println("delete($addressPair)")
        return true
    }

    override fun exit()
    {
        println("exit()")
    }
}
