package lib.net

import tools.Logger
import java.nio.channels.*
import java.nio.channels.spi.AbstractSelectableChannel
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by bensoer on 02/03/16.
 */

/**
 * Select is a helper / wrapper class that wraps the Java Selector NIO functionality into a single interactable
 * class, thus to give the client and outer libraries a more legible and strict API to interact with the selector
 */
class Select {

    private val selector:Selector = Selector.open();
    private var keyRing: Set<SelectionKey>? = null;

    private var registerServerLock = ReentrantLock();

    /**
     * waitForEvent is a blocking call that waits for events to occur on the sockets that have been registered
     * with the Select class
     */
    fun waitForEvent(): Int{

        val readySet:Int = this.selector.select();

        return readySet;

    }

    /**
     * getReadyChannels is a helper method used primarily with waitForEvent. When waitForEvent returns, the client
     * is expected to call this method to get a list of the keys belonging to the channels that have had activity
     * occur on them and are ready to be interacted with by the client
     */
    fun getReadyChannels(): Set<SelectionKey> {
        return this.selector.selectedKeys();
    }

    /**
     * registerChannel registers a passed in channel with the select object so that it is included in the loop
     * to be checked for events on its socket. The method registers the channel with the ability to listen for
     * READ events where the client can read from the channel
     */
    fun registerChannel(channel: AbstractSelectableChannel) : SelectionKey{
        Logger.log("Select - Registering Channel With Select");

        channel.configureBlocking(false);

        val interestSet = SelectionKey.OP_READ;
        //val key: SelectionKey = channel.register(this.selector, interestSet);
        //println(channel.validOps());
        val key: SelectionKey = channel.register(this.selector, interestSet);

        return key;

    }

    /**
     * registerServerChannel registers a ServerSocketChannel with the Select object so that it is included in the
     * check loop for events on its socket. The channel is registered to listen for ACCEPT events.
     */
    fun registerServerChannel(channel: ServerSocketChannel) : SelectionKey{
        //ensuring that there is no overlap in this process
        this.registerServerLock.lock();

        Logger.log("Select - Registering Server Channel With Select");

        channel.configureBlocking(false);

        val interestSet = SelectionKey.OP_ACCEPT;
        //val key: SelectionKey = channel.register(this.selector, interestSet);
        //println(channel.validOps());
        val key: SelectionKey = channel.register(this.selector, interestSet);

        this.registerServerLock.unlock();
        return key;

    }

    /**
     * isNewConnection is a helper method for checking if the passed in key's channel is accepting a connection
     * OR has the ability to accept connections
     */
    fun isANewConnection(key: SelectionKey): Boolean{
        return key.isAcceptable();
    }

    /**
     * hasDataToRead is a helper method for checking if the passed in key's channel has data to be read OR has the
     * ability to be read from
     */
    fun hasDataToRead(key: SelectionKey): Boolean{
        return key.isReadable();
    }

    /**
     * getChannelForKey is a helper method that fetches the associated channel with the given key
     */
    fun getChannelForKey(key: SelectionKey): SelectableChannel {
        return key.channel();
    }

    /**
     * getAllKeys is a helper method that returns all keys that have been registered with the select object
     */
    fun getAllKeys() : MutableSet<SelectionKey>{
        return this.selector.keys();
    }


}