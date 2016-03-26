package client

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

/**
 * Created by bensoer on 15/03/16.
 */

fun main(args:Array<String>){



    val socket = DatagramSocket(5050);
    //socket.connect(InetSocketAddress("localhost", 5050));

    val response:ByteArray = ByteArray(256);
    val responseDGramPacket = DatagramPacket(response, response.size);

    socket.receive(responseDGramPacket);

    println("Got Response");
    val data = responseDGramPacket.data;
    val newString:String = String(data);

    println(newString);



    println("Now Sending Message");
    val srcAddress = socket.remoteSocketAddress;



    val message = "HELLO BACK";
    val datagramPacket = DatagramPacket(message.toByteArray(), message.toByteArray().size);
    datagramPacket.address = responseDGramPacket.address;
    datagramPacket.port = responseDGramPacket.port;
    println("Sending to: ${responseDGramPacket.address.hostAddress} on port ${responseDGramPacket.port}");
    socket.send(datagramPacket);




}