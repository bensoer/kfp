package gui

import com.sun.javafx.collections.ObservableMapWrapper
import javafx.collections.MapChangeListener
import tools.AddressPair
import tools.ConnStats
import java.util.LinkedHashMap

class GUI(val listener:GUI.IListener)
{
    /**
     * map of [AddressPair]s and their associated [ConnStats] displayed on the
     * [GUI]. just use it like a normal map, and the [GUI] will magically be
     * updated. updating entries in this map will not trigger the [listener]'s
     * methods to be called.
     */
    val addressPairs:MutableMap<AddressPair,ConnStats>
        get() = _addressPairs
    private val _addressPairs = ObservableMapWrapper(LinkedHashMap<AddressPair,ConnStats>())

    init
    {
        _addressPairs.addListener(MapChangeListener()
        {
            change ->
            // update the GUI
        })
    }

    /**
     * interface that [GUI] observers must implement to be notified by the [GUI]
     * upon user interaction.
     */
    interface IListener
    {
        /**
         * called when the user adds a new [AddressPair] to the [GUI].
         */
        fun insert(addressPair:AddressPair)

        /**
         * called when the user removes an existing [AddressPair] from the
         * [GUI].
         */
        fun delete(addressPair:AddressPair)
    }
}