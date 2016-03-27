package client

import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.CharBuffer

/**
 * Created by bensoer on 05/03/16.
 */

fun main(argv:Array<String>){

    println("Starting Server");

    val serverSocket = ServerSocket();

    val socketAddress = InetSocketAddress("localhost", 4040);
    serverSocket.bind(socketAddress);

    val socket = serverSocket.accept();

    val writeStream = socket.outputStream;
    val readStream = socket.inputStream;

    val streamReader = InputStreamReader(readStream);

    var buffer = CharBuffer.allocate(5)
    val bytesRead = streamReader.read(buffer);

    buffer.flip();
    println(buffer);

    println("Server Revcieved : $bytesRead");

    println("Server Sending Back");

    val printWriter = PrintWriter(writeStream);
    printWriter.print("HELLO BACK");
    printWriter.flush();

}