package lib.net

import gui.GUI
import lib.db.IPersistanceAdaptor
import tools.AddressPair
import java.net.SocketAddress
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.*

/**
 * Created by bensoer on 28/02/16.
 */

class AddressMapper(private val dataStore: IPersistanceAdaptor){

    //private var portMappings:Set<AddressPair>;
    private var socketMappings: HashMap<SelectionKey, SocketChannel> = HashMap();
    private var inverseSocketMappings: HashMap<SocketChannel, SelectionKey> = HashMap();

    private var datagramMappings: HashMap<SocketAddress, DatagramChannel> = HashMap();
    private var inverseDatagramMappings: HashMap<DatagramChannel, SocketAddress> = HashMap();

    init {
        //this.portMappings = dataStore.loadAll()
    }

    fun getAllPortMappings(): Set<AddressPair> {
        return this.dataStore.loadAll();
    }

    fun addPortMapping(addressPair: AddressPair){
        this.dataStore.saveAddress(addressPair);
    }

    fun deletePortMapping(addressPair: AddressPair){
        this.dataStore.deleteAddress(addressPair);
    }

    fun deleteKeyForChannel(channel: SocketChannel){
        val key = this.inverseSocketMappings[channel];

        this.socketMappings.remove(key);
        this.inverseSocketMappings.remove(channel);

    }

    fun getPortMapping(port: Int, type: String): AddressPair? {
        val iterator = this.dataStore.loadAll().iterator();

        while(iterator.hasNext()){
            val ap = iterator.next();

            if(ap.localPort == port && ap.type.equals(type)){
                return ap;
            }
        }
        return null;
    }

    fun createSocketMapping(key: SelectionKey, channel: SocketChannel){
        this.socketMappings.put(key, channel);
        this.inverseSocketMappings.put(channel,key);
    }

    fun createDatagramMapping(key: SocketAddress, channel: DatagramChannel){
        this.datagramMappings.put(key, channel);
        this.inverseDatagramMappings.put(channel, key);
    }

    fun getSocketChannel(key: SelectionKey): SocketChannel?{
        return this.socketMappings[key];
    }

    fun getDatagramChannel(address: SocketAddress): DatagramChannel?{
        return this.datagramMappings[address];
    }

    fun clearAllSocketMappings(){
        this.socketMappings.clear();
        this.inverseSocketMappings.clear();
    }

}