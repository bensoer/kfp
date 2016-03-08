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

class AddressMapper(private val dataStore:IPersistanceAdaptor){

    //private var portMappings:Set<AddressPair>;
    private var socketMappings:HashMap<SelectionKey,SocketChannel> = HashMap();
    private var inverseSocketMappings:HashMap<SocketChannel,SelectionKey> = HashMap();

    init {
        //this.portMappings = dataStore.loadAll()
    }

    fun getAllPortMappings(): Set<AddressPair> {
        return this.dataStore.loadAll();
    }

    fun addPortMapping(addressPair: AddressPair){
        this.dataStore.saveAddress(addressPair);
    }

    fun deletePortMapping(addressPair:AddressPair){
        this.dataStore.deleteAddress(addressPair);
    }

    fun deleteKeyForChannel(channel: SocketChannel){
        val key = this.inverseSocketMappings[channel];

        this.socketMappings.remove(key);
        this.inverseSocketMappings.remove(channel);

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
        this.inverseSocketMappings.put(channel,key);
    }

    fun getSocketChannel(key: SelectionKey):SocketChannel?{
        return this.socketMappings[key];
    }

}