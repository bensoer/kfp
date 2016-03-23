package client

import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.nio.CharBuffer

/**
 * Created by bensoer on 15/03/16.
 */

fun main(argv:Array<String>){


    val socket = DatagramSocket(3030);
    socket.connect(InetSocketAddress("localhost", 4040));

    val message = "HELLO";

    val datagramPacket = DatagramPacket(message.toByteArray(), message.toByteArray().size);

    socket.send(datagramPacket);

    val response:ByteArray = ByteArray(256);
    val responseDGramPacket = DatagramPacket(response, response.size);

    socket.receive(responseDGramPacket);

    println("Got Response");
    println(responseDGramPacket.data);
}