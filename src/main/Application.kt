package main

import gui.GUI
import tools.AddressPair
import tools.Logger


/**
 * Created by bensoer on 28/02/16.
 */

/** PROGRAM MAIN ENTRANCE **/

fun main(args: Array<String>){
    Logger.log("PortForwarder - Initializing Program");


    println("Hello world!");

    Logger.log("HELLO FROM LOGGER");

}

private val GUIEventHandler = object: GUI.IListener{

    override fun insert(addressPair: AddressPair)
    {
        println("insert($addressPair)")
    }

    override fun delete(addressPair: AddressPair)
    {
        println("delete($addressPair)")
    }

}