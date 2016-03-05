package lib

import gui.GUI
import lib.db.IPersistanceAdaptor
import tools.AddressPair
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.*

/**
 * Created by bensoer on 28/02/16.
 */

class AddressMapper(private val dataStore:IPersistanceAdaptor): GUI.IListener {

    //private var portMappings:Set<AddressPair>;
    private var socketMappings:HashMap<SelectionKey,SocketChannel> = HashMap();

    init {
        //this.portMappings = dataStore.loadAll()
    }

    fun getAllPortMappings(): Set<AddressPair> {
        return this.dataStore.loadAll();
    }

    fun getPortMapping(port: Int): AddressPair? {
        val iterator = this.dataStore.loadAll().iterator();

        while(iterator.hasNext()){
            val ap = iterator.next();

            if(ap.localPort == port){
                return ap;
            }
        }
        return null;
    }

    fun createSocketMapping(key: SelectionKey, channel: SocketChannel){
        this.socketMappings.put(key, channel);
    }

    fun getSocketChannel(key: SelectionKey):SocketChannel?{
        return this.socketMappings[key];
    }


    /** -- GUI.ILISTENER OVERRIDES -- */


    override fun insert(addressPair: AddressPair) {
        this.dataStore.saveAddress(addressPair);
    }

    override fun delete(addressPair: AddressPair) {
        this.dataStore.deleteAddress(addressPair);
    }

}