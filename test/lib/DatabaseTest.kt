package lib

import lib.db.IPersistanceAdaptor
import lib.db.SQLitePersistanceAdaptor
import tools.AddressPair
import java.net.InetSocketAddress
import java.util.*

/**
 * Created by bensoer on 04/03/16.
 */

fun main(args:Array<String>){


    var persistance:IPersistanceAdaptor = SQLitePersistanceAdaptor("test-storage.db");

    val ap = AddressPair(localPort = 839021, dest = InetSocketAddress("localhost", 88));
    val ap2 = AddressPair(localPort = 83921, dest = InetSocketAddress("localhost", 88));
    val ap3 = AddressPair(localPort = 8021, dest = InetSocketAddress("localhost", 88));

    val data = persistance.loadAll();

    println("Loading for Empty Data");
    println(data);

    persistance.saveAddress(ap);

    println("Loading for Loaded Data");
    val newData = persistance.loadAll();
    println(newData.size);
    println(newData);

    persistance.saveAddress(ap2);
    persistance.saveAddress(ap3);
    val newData2 = persistance.loadAll();
    println(newData2.size);
    println(newData2);






}