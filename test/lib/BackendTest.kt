package lib

import gui.GUI
import lib.db.IPersistanceAdaptor
import lib.db.SQLitePersistanceAdaptor
import lib.net.NetManager
import tools.AddressPair

/**
 * Created by bensoer on 04/03/16.
 */

fun main(argv:Array<String>){

    val storage = SQLitePersistanceAdaptor("portmap.db");
    val addressMapper = AddressMapper(storage);
    val netManager = NetManager(addressMapper);

    val guiEventHandler = GUIEventHandler(netManager);

    netManager.start();

    netManager.join();

}

private class GUIEventHandler(val netManager:NetManager): GUI.IListener{

    override fun insert(addressPair: AddressPair)
    {
        //this.dataStorage.saveAddress(addressPair);
        //TODO: Return the status of whether it was succesful
        this.netManager.addMapping(addressPair);

    }

    override fun delete(addressPair: AddressPair)
    {
        //TODO: Return the status of whether it was successful
        this.netManager.removeMapping(addressPair);
        //this.dataStorage.deleteAddress(addressPair);
    }

}