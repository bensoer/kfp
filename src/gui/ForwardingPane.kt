package gui

import com.sun.javafx.collections.ObservableListWrapper
import com.sun.javafx.collections.ObservableSetWrapper
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.collections.SetChangeListener
import javafx.geometry.Insets
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.*
import lib.net.NetLibrary
import tools.AddressPair
import java.net.InetSocketAddress
import java.util.*

class ForwardingPane:GridPane()
{

    private val COL_INDEX_LOCAL_PORT:Int = 1
    private val COL_INDEX_PROTOCOL:Int = 2
    private val COL_INDEX_SEPARATOR:Int = 3
    private val COL_INDEX_DST_ADDR:Int = 4
    private val COL_INDEX_COLON:Int = 5
    private val COL_INDEX_DST_PORT:Int = 6

    private val COLON_LABEL_TEXT:String = ":";
    private val SEPARATOR_TEXT:String = "->"

    private val forwardingEntries:MutableList<ForwardingEntry> = LinkedList()
    private val forwardingEntryObserver = ForwardingEntryListener()

    var listener:IListener? = null

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
        add(forwardingEntry.protocolComboBox,COL_INDEX_PROTOCOL,forwardingEntries.size)
        add(Label(SEPARATOR_TEXT),COL_INDEX_SEPARATOR,forwardingEntries.size)
        add(forwardingEntry.dstAddrTextField,COL_INDEX_DST_ADDR,forwardingEntries.size)
        add(Label(COLON_LABEL_TEXT),COL_INDEX_COLON,forwardingEntries.size)
        add(forwardingEntry.dstPortTextField,COL_INDEX_DST_PORT,forwardingEntries.size)

        // book keeping
        forwardingEntry.listener = forwardingEntryObserver
        forwardingEntries.add(forwardingEntry)
    }

    private fun remove(forwardingEntry:ForwardingEntry) = synchronized(this)
    {
        // book keeping
        forwardingEntry.listener = null
        val toRetain = forwardingEntries.minus(forwardingEntry)

        // replace all existing forwarding entries
        children.clear()
        forwardingEntries.clear()
        toRetain.forEach {add(it)}
    }

    interface IListener
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
                    lastForwardingEntry.protocolComboBox.value = change.elementAdded.type
                    add(ForwardingEntry())
                }
                else
                {
                    val forwardingEntry = forwardingEntries
                        .find {it.localPort == change.elementRemoved.localPort && it.dstSockAddr == change.elementRemoved.dest && it.protocol == change.elementRemoved.type}
                    if (forwardingEntry != null) remove(forwardingEntry)
                }
            }
        }
    }

    private inner class ForwardingEntryListener:ForwardingEntry.IListener
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
                    forwardingEntries.last().localPortTextField.text.isNotBlank() ||
                    forwardingEntries.last().protocol != null)
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
                    .filter {it.error.localPort == false && it.error.dstSockAddr == false && it.error.protocol == false}
                    .map {AddressPair(it.localPort!!.toInt(),it.dstSockAddr!!,it.protocol!!)}

                // sync up the addressPairs map: remove old entries
                val toRemove = _addressPairs.filter {it !in allAddressPairs}
                Platform.runLater {toRemove.forEach {listener?.removed(it)}}
                _addressPairs.removeAll(toRemove)

                // sync up the addressPairs map: add new entries
                val toAdd = allAddressPairs.filter {it !in _addressPairs}
                Platform.runLater {toAdd.forEach {listener?.added(it)}}
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
        private const val PROTOCOL_PROMPT = "Protocol"

        /**
         * test used as values in [protocolComboBox].
         */
        private const val PROTOCOL_TCP = "TCP"
        private const val PROTOCOL_UDP = "UDP"

        /**
         * milliseconds to delay
         */
        private const val VALIDATION_DELAY_MILLIS = 1000L
    }

    /**
     * user-specified local port to redirect traffic from.
     */
    var localPort:Int? = null

    /**
     * user-specified address to forward traffic to.
     */
    var dstSockAddr:InetSocketAddress? = null

    /**
     * user-specified traffic type to forward.
     */
    var protocol:String? = null

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

    /**
     * [ComboBox] with a list of valid protocols that the user may select from
     * to choose to forward.
     */
    val protocolComboBox = ComboBox(ObservableListWrapper(listOf(PROTOCOL_TCP,PROTOCOL_UDP)))

    var error:ErrorDetails = ErrorDetails(false,false,false)

        /**
         * styles UI controls based on error flags.
         */
        set(value)
        {
            assert(Platform.isFxApplicationThread())

            if (value.localPort != field.localPort)
                if (value.localPort)
                    localPortTextField.styleClass.add(CSS.WARNING_CONTROL)
                else
                    localPortTextField.styleClass.remove(CSS.WARNING_CONTROL)

            if (value.dstSockAddr != field.dstSockAddr)
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

            if (value.protocol != field.protocol)
                if (value.protocol)
                    protocolComboBox.styleClass.add(CSS.WARNING_CONTROL)
                else
                    protocolComboBox.styleClass.remove(CSS.WARNING_CONTROL)

            field = value
        }

    data class ErrorDetails(val localPort:Boolean,val dstSockAddr:Boolean,val protocol:Boolean)

    var listener:ForwardingEntry.IListener? = null

    private var validateInputTask:ValidateInputTask? = null

    private var validateInputTimer = Timer("validateInputTimer",true)

    init
    {
        // reassign instance variables to run their setters
        error = ErrorDetails(true,true,true)

        // set on action code
        localPortTextField.textProperty().addListener(InvalidationListener{validateAndNotify()})
        dstAddrTextField.textProperty().addListener(InvalidationListener{validateAndNotify()})
        dstPortTextField.textProperty().addListener(InvalidationListener{validateAndNotify()})
        protocolComboBox.valueProperty().addListener(InvalidationListener{validateAndNotify()})

        // add prompt text to text fields
        localPortTextField.promptText = LOCAL_PORT_PROMPT
        dstAddrTextField.promptText = DST_ADDR_PROMPT
        dstPortTextField.promptText = DST_PORT_PROMPT
        protocolComboBox.promptText = PROTOCOL_PROMPT

        // configure mins and maxs of port text fields
        localPortTextField.min = NetLibrary.MIN_PORT
        localPortTextField.max = NetLibrary.MAX_PORT
        dstPortTextField.min = NetLibrary.MIN_PORT
        dstPortTextField.max = NetLibrary.MAX_PORT
    }

    private fun validateAndNotify():Unit = synchronized(this)
    {
        validateInputTask?.cancel()
        validateInputTask = ValidateInputTask()
        validateInputTimer.schedule(validateInputTask,VALIDATION_DELAY_MILLIS)
        validateInputTimer.purge()
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

    interface IListener
    {
        fun onDataChanged(observee:ForwardingEntry);
    }

    private inner class ValidateInputTask:TimerTask()
    {
        override fun run()
        {
            // try to resolve addresses
            localPort = try {localPortTextField.text.toInt()} catch(ex:Exception) {null}
            protocol = protocolComboBox.value
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
            if (protocol == null)
                error = error.copy(protocol = true)
            else
                error = error.copy(protocol = false)

            // set instance variable sock addresses
            Platform.runLater()
            {
                listener?.onDataChanged(this@ForwardingEntry)
            }
        }
    }
}
