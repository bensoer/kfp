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
    val guiEventHandler = GUIEventHandler(storage);
    val addressMapper = AddressMapper(storage);
    val netManager = NetManager(addressMapper);

}

private class GUIEventHandler(val dataStorage:IPersistanceAdaptor): GUI.IListener{

    override fun insert(addressPair: AddressPair)
    {
        this.dataStorage.saveAddress(addressPair);
    }

    override fun delete(addressPair: AddressPair)
    {
        this.dataStorage.deleteAddress(addressPair);
    }

}