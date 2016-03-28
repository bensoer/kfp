package client

import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.nio.CharBuffer
import java.nio.charset.Charset

/**
 * Created by bensoer on 15/03/16.
 */

fun main(argv:Array<String>){


    val socket = DatagramSocket(3030);
    socket.connect(InetSocketAddress("localhost", 4040));

    val message:String = "HELLO";
    println("Sending message: $message");
    println("Converted it is: ${message.toByteArray()}");


    val datagramPacket = DatagramPacket(message.toByteArray(Charset.defaultCharset()), message.length);

    socket.send(datagramPacket);

    val response:ByteArray = ByteArray(256);
    val responseDGramPacket = DatagramPacket(response, response.size);

    socket.receive(responseDGramPacket);

    println("Got Response");
    println(String(responseDGramPacket.data));
}