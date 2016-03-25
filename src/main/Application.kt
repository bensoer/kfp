package main

import gui.GUI
import gui.gui
import lib.db.SQLitePersistanceAdaptor
import lib.net.AddressMapper
import lib.net.NetManager
import tools.AddressPair
import tools.Logger
import java.net.InetSocketAddress
import kotlin.concurrent.thread


/**
 * Created by bensoer on 28/02/16.
 */

/** PROGRAM MAIN ENTRANCE **/

fun main(args: Array<String>){
    Logger.log("PortForwarder - Initializing Program");

    val storage = SQLitePersistanceAdaptor("portmap.db");
    val addressMapper = AddressMapper(storage);
    val netManager = NetManager(addressMapper);

    val guiEventHandler = GUIEventHandler(netManager);

    //setup the GUI
    thread { GUI.mainLoop() }
    GUI.awaitInitialized()

    val netManagerEventHandler = NetManagerEventHandler(gui);
    netManager.listener = netManagerEventHandler;

    gui.addressPairs.addAll(storage.loadAll());

    // adds a listener to GUI so we will be notified when user interacts with it
    gui.listener = guiEventHandler;

    //start network thread
    netManager.start();
    //wait for network thread to die
    netManager.join();
}

private class GUIEventHandler(val netManager: NetManager): GUI.IListener{

    override fun insert(addressPair: AddressPair):Boolean
    {
        Logger.log("GUIEventHandler - Inserting");
        return this.netManager.addMapping(addressPair);
    }

    override fun delete(addressPair: AddressPair):Boolean
    {
        Logger.log("GUIEventHandler - Deleting");
        return this.netManager.removeMapping(addressPair);
    }

    override fun exit(){
        Logger.log("GUIEventHandler - Closing");
        this.netManager.terminate();
    }

}

private class NetManagerEventHandler(val gui:GUI):NetManager.IListener{

    override fun bytesTransfered(sourceAddress: InetSocketAddress, localPort: Int, bytesTransferred: Int) {
        this.gui.bytesForwarded(sourceAddress,localPort,bytesTransferred);
    }

    override fun connectionOpened() {
        this.gui.connectionOpened();
    }

    override fun connectionClosed() {
        this.gui.connectionClosed();
    }


}