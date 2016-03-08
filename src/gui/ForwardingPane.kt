package gui

import com.sun.javafx.collections.ObservableSetWrapper
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.collections.SetChangeListener
import javafx.geometry.Insets
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.*
import lib.net.NetLibrary
import tools.AddressPair
import java.net.InetSocketAddress
import java.util.*
import kotlin.concurrent.thread

class ForwardingPane:GridPane()
{

    private val COL_INDEX_LOCAL_PORT:Int = 1
    private val COL_INDEX_SEPARATOR:Int = 2
    private val COL_INDEX_DST_ADDR:Int = 3
    private val COL_INDEX_COLON:Int = 4
    private val COL_INDEX_DST_PORT:Int = 5

    private val COLON_LABEL_TEXT:String = ":";
    private val SEPARATOR_TEXT:String = "->"

    private val forwardingEntries:MutableList<ForwardingEntry> = LinkedList()
    private val forwardingEntryObserver = ForwardingEntryObserver()

    var listener:Listener? = null

    private val _addressPairs = LinkedHashSet<AddressPair>()
    val addressPairs = ObservableSetWrapper(_addressPairs)

    init
    {
        // configure aesthetic properties
        padding = Insets(Dimens.KEYLINE_SMALL.toDouble())
        hgap = Dimens.KEYLINE_SMALL.toDouble()
        vgap = Dimens.KEYLINE_SMALL.toDouble()

        // configure grid constraints
        columnConstraints.add(ColumnConstraints())
        columnConstraints.add(ColumnConstraints())
        columnConstraints.add(ColumnConstraints())
        columnConstraints.add(ColumnConstraints())
        columnConstraints.add(ColumnConstraints())
        columnConstraints.add(ColumnConstraints())

        val lastColumn = ColumnConstraints()
        lastColumn.isFillWidth = true
        lastColumn.hgrow = Priority.ALWAYS
        columnConstraints.add(lastColumn)

        // add gui controls
        add(ForwardingEntry())

        // begin observing for events
        addressPairs.addListener(AddressPairObserver())
    }

    private fun add(forwardingEntry:ForwardingEntry) = synchronized(this)
    {
        // add nodes to layout
        add(forwardingEntry.localPortTextField,COL_INDEX_LOCAL_PORT,forwardingEntries.size)
        add(Label(SEPARATOR_TEXT),COL_INDEX_SEPARATOR,forwardingEntries.size)
        add(forwardingEntry.dstAddrTextField,COL_INDEX_DST_ADDR,forwardingEntries.size)
        add(Label(COLON_LABEL_TEXT),COL_INDEX_COLON,forwardingEntries.size)
        add(forwardingEntry.dstPortTextField,COL_INDEX_DST_PORT,forwardingEntries.size)

        // book keeping
        forwardingEntry.stateObserver = forwardingEntryObserver
        forwardingEntries.add(forwardingEntry)
    }

    private fun remove(forwardingEntry:ForwardingEntry) = synchronized(this)
    {
        // book keeping
        forwardingEntry.stateObserver = null
        val toRetain = forwardingEntries.minus(forwardingEntry)

        // replace all existing forwarding entries
        children.clear()
        forwardingEntries.clear()
        toRetain.forEach {add(it)}
    }

    interface Listener
    {
        fun added(addressPair:AddressPair)
        fun removed(addressPair:AddressPair)
    }

    private inner class AddressPairObserver:SetChangeListener<AddressPair>
    {
        override fun onChanged(change:SetChangeListener.Change<out AddressPair>)
        {
            Platform.runLater()
            {
                if (change.wasAdded())
                {
                    val lastForwardingEntry = forwardingEntries.last()
                    lastForwardingEntry.dstAddrTextField.text = change.elementAdded.dest.hostName
                    lastForwardingEntry.dstPortTextField.text = change.elementAdded.dest.port.toString()
                    lastForwardingEntry.localPortTextField.text = change.elementAdded.localPort.toString()
                    add(ForwardingEntry())
                }
                else
                {
                    val forwardingEntry = forwardingEntries
                        .find {it.localPort == change.elementRemoved.localPort && it.dstSockAddr == change.elementRemoved.dest}
                    if (forwardingEntry != null) remove(forwardingEntry)
                }
            }
        }
    }

    private inner class ForwardingEntryObserver:ForwardingEntry.Observer
    {
        override fun onDataChanged(observee:ForwardingEntry)
        {
            synchronized(this)
            {
                // remove the entry if it is empty and not the last one
                if (observee != forwardingEntries.last() &&
                    observee.dstPortTextField.text.isBlank() &&
                    observee.dstAddrTextField.text.isBlank() &&
                    observee.localPortTextField.text.isBlank())
                {
                    remove(observee)
                }

                // if the last forwarding entry is not empty, add a new one
                if (forwardingEntries.last().dstPortTextField.text.isNotBlank() ||
                    forwardingEntries.last().dstAddrTextField.text.isNotBlank() ||
                    forwardingEntries.last().localPortTextField.text.isNotBlank())
                {
                    add(ForwardingEntry())
                }

                // go through all forwarding entries, and set the error flags
                // for entries that have duplicate localPorts
                val usedLocalPorts = LinkedHashSet<Int>()
                forwardingEntries.forEach()
                {
                    val localPort = it.localPort
                    if (localPort in usedLocalPorts)
                    {
                        it.error = it.error.copy(localPort = true)
                    }
                    else if (localPort != null)
                    {
                        it.error = it.error.copy(localPort = false)
                        usedLocalPorts.add(localPort)
                    }
                }

                // sync up the addressPairs map: resolve current address pairs
                val allAddressPairs = forwardingEntries
                    .filter {it.error.localPort == false && it.error.dstSockAddr == false}
                    .map {AddressPair(it.localPort!!.toInt(),it.dstSockAddr!!)}

                // sync up the addressPairs map: remove old entries
                val toRemove = _addressPairs.filter {it !in allAddressPairs}
                toRemove.forEach {listener?.removed(it)}
                _addressPairs.removeAll(toRemove)

                // sync up the addressPairs map: add new entries
                val toAdd = allAddressPairs.filter {it !in _addressPairs}
                toAdd.forEach {listener?.added(it)}
                _addressPairs.addAll(toAdd)
            }
        }
    }
}

private class ForwardingEntry()
{
    companion object
    {
        /**
         * text used as prompts on [TextField]s.
         */
        private const val LOCAL_PORT_PROMPT = "Local Port"
        private const val DST_ADDR_PROMPT = "IP Address"
        private const val DST_PORT_PROMPT = "Port Number"

        /**
         * milliseconds to delay
         */
        private const val VALIDATION_DELAY_MILLIS = 200L
    }

    var localPort:Int? = null

    var dstSockAddr:InetSocketAddress? = null

    /**
     * [TextField] for the local port on this host that remote hosts will
     * connect to.
     */
    val localPortTextField = IntTextField(true)

    /**
     * [TextField] for the destination host that connections to the local port
     * will be forwarded to.
     */
    val dstAddrTextField = TextField()

    /**
     * [TextField] for the destination port on the destination host that
     * connections to the local port will be forwarded to.
     */
    val dstPortTextField = IntTextField(true)

    var error:ErrorDetails = ErrorDetails(false,false)

        set(value)
        {
            assert(Platform.isFxApplicationThread())
            if (value.localPort != field.localPort)
            {
                if (value.localPort)
                {
                    localPortTextField.styleClass.add(CSS.WARNING_CONTROL)
                }
                else
                {
                    localPortTextField.styleClass.remove(CSS.WARNING_CONTROL)
                }
            }

            if (value.dstSockAddr != field.dstSockAddr)
            {
                if (value.dstSockAddr)
                {
                    dstAddrTextField.styleClass.add(CSS.WARNING_CONTROL)
                    dstPortTextField.styleClass.add(CSS.WARNING_CONTROL)
                }
                else
                {
                    dstAddrTextField.styleClass.remove(CSS.WARNING_CONTROL)
                    dstPortTextField.styleClass.remove(CSS.WARNING_CONTROL)
                }
            }
            field = value
        }

    data class ErrorDetails(val localPort:Boolean,val dstSockAddr:Boolean)

    var stateObserver:ForwardingEntry.Observer? = null

    private var validationThread:Thread = Thread()

    init
    {
        // reassign instance variables to run their setters
        error = ErrorDetails(true,true)

        // set on action code
        localPortTextField.textProperty().addListener(InvalidationListener{validateAndNotify()})
        dstAddrTextField.textProperty().addListener(InvalidationListener{validateAndNotify()})
        dstPortTextField.textProperty().addListener(InvalidationListener{validateAndNotify()})

        // add prompt text to text fields
        localPortTextField.promptText = LOCAL_PORT_PROMPT
        dstAddrTextField.promptText = DST_ADDR_PROMPT
        dstPortTextField.promptText = DST_PORT_PROMPT

        // configure mins and maxs of port text fields
        localPortTextField.min = NetLibrary.MIN_PORT
        localPortTextField.max = NetLibrary.MAX_PORT
        dstPortTextField.min = NetLibrary.MIN_PORT
        dstPortTextField.max = NetLibrary.MAX_PORT
    }

    private fun validateAndNotify() = synchronized(this)
    {
        // interrupt the previous thread so it will abort its callback operation
        validationThread.interrupt()

        // begin the validation on the validation thread
        validationThread = thread()
        {
            // sleep a bit because validation takes a long time, and we want to
            // make sure that use user has stopped typing before we go ahead and
            // try to validate the input
            try
            {
                Thread.sleep(VALIDATION_DELAY_MILLIS)
            }
            catch(ex:InterruptedException)
            {
                return@thread
            }

            // try to resolve addresses
            localPort = try {localPortTextField.text.toInt()} catch(ex:Exception) {null}
            dstSockAddr = validateAddress(dstAddrTextField.text,dstPortTextField.text)

            // update the error feedback on the GUI
            if (localPort == null)
                error = error.copy(localPort = true)
            else
                error = error.copy(localPort = false)
            if (dstSockAddr == null)
                error = error.copy(dstSockAddr = true)
            else
                error = error.copy(dstSockAddr = false)

            // set instance variable sock addresses
            if (!Thread.interrupted())
            {
                Platform.runLater()
                {
                    stateObserver?.onDataChanged(this@ForwardingEntry)
                }
            }
        }
    }

    private fun validateAddress(host:String,port:String):InetSocketAddress?
    {
        try
        {
            // try to resolve addresses
            val sockAddr = InetSocketAddress(host,port.toInt())

            // if addresses were not resolved, input is invalid; throw
            if (sockAddr.isUnresolved) throw IllegalArgumentException()

            return sockAddr
        }
        catch(ex:Exception)
        {
            return null
        }
    }

    interface Observer
    {
        fun onDataChanged(observee:ForwardingEntry);
    }
}
