package lib.net

import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel

/**
 * Created by bensoer on 02/03/16.
 */

class Select {

    private val selector = Selector.open();
    private var keyRing: Set<SelectionKey>? = null;

    fun waitForEvent(): Int{

        val readySet:Int = this.selector.select();

        return readySet;

    }

    fun getReadyChannels(): Set<SelectionKey> {
        return this.selector.selectedKeys();
    }

    fun registerChannel(channel: SocketChannel) : SelectionKey{

        channel.configureBlocking(false);

        val interestSet:Int = (SelectionKey.OP_READ or SelectionKey.OP_ACCEPT);
        val key: SelectionKey = channel.register(this.selector, interestSet);

        return key;

    }

    fun isANewConnection(key: SelectionKey): Boolean{
        return key.isAcceptable();
    }

    fun hasDataToRead(key: SelectionKey): Boolean{
        return key.isReadable();
    }

    fun getChannelForKey(key: SelectionKey): SocketChannel {
        return key.channel() as SocketChannel;
    }


}