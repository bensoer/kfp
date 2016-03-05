package lib.net

import tools.Logger
import java.nio.channels.*
import java.nio.channels.spi.AbstractSelectableChannel

/**
 * Created by bensoer on 02/03/16.
 */

class Select {

    private val selector:Selector = Selector.open();
    private var keyRing: Set<SelectionKey>? = null;

    fun waitForEvent(): Int{

        Logger.log("Inside for Events");
        val readySet:Int = this.selector.select();
        Logger.log("Ello");

        return readySet;

    }

    fun getReadyChannels(): Set<SelectionKey> {
        return this.selector.selectedKeys();
    }

    fun registerChannel(channel: AbstractSelectableChannel) : SelectionKey{

        channel.configureBlocking(false);

        val interestSet = SelectionKey.OP_READ;
        //val key: SelectionKey = channel.register(this.selector, interestSet);
        println(channel.validOps());
        val key: SelectionKey = channel.register(this.selector, interestSet);

        return key;

    }

    fun registerServerChannel(channel: ServerSocketChannel) : SelectionKey{
        Logger.log("Select - Registering Server Channel With Select");

        channel.configureBlocking(false);

        val interestSet = SelectionKey.OP_ACCEPT;
        //val key: SelectionKey = channel.register(this.selector, interestSet);
        println(channel.validOps());
        val key: SelectionKey = channel.register(this.selector, interestSet);

        return key;

    }

    fun isANewConnection(key: SelectionKey): Boolean{
        return key.isAcceptable();
    }

    fun hasDataToRead(key: SelectionKey): Boolean{
        return key.isReadable();
    }

    fun getChannelForKey(key: SelectionKey): SelectableChannel {
        return key.channel();
    }


}