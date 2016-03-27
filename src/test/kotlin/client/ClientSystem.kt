package client

import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.CharBuffer

/**
 * Created by bensoer on 05/03/16.
 */

fun main(args:Array<String>){


    val socket = Socket("localhost", 3030);

    val outstream = socket.outputStream;
    val out = PrintWriter(outstream);

    out.print("HELLO");
    out.flush();

    val instream = socket.inputStream;
    val input = InputStreamReader(instream);

    val buffer = CharBuffer.allocate(25);
    val bytesRead = input.read(buffer);

    buffer.flip();
    println(bytesRead);
    println(buffer);

    //socket.shutdownInput();
    //socket.shutdownOutput();
    //socket.close();

}