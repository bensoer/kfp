package lib.db

import tools.AddressPair
import tools.Logger
import java.io.File
import java.net.InetSocketAddress
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by bensoer on 04/03/16.
 */

class SQLitePersistanceAdaptor(val DBNAME:String):IPersistanceAdaptor{

    private val lock:Lock = ReentrantLock();
    private var connection:Connection? = null;
    private val CREATE_ADDRESS_PAIR_TABLE =
        "CREATE TABLE IF NOT EXISTS ADDRESSPAIRS " +
        "(ID          INTEGER     PRIMARY KEY AUTOINCREMENT," +
        " LOCALPORT   TEXT        NOT NULL, " +
        " DESTIP      TEXT        NOT NULL, " +
        " DESTPORT    INTEGER     NOT NULL" +
        " TYPE        TEXT        NOT NULL)";


    init {
        this.connectToDatabase();
    }

    private fun connectToDatabase(){
        if(this.connection == null || this.connection!!.isClosed){
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + this.DBNAME);
            } catch ( e:Exception ) {
                Logger.log("SQLitePersistanceAdaptor - Failed To Connect To SQLite Database");
                e.printStackTrace();
            }

            Logger.log("SQLitePersistanceAdaptor - Connection Succeeded");
            this.setupTables();
        }
    }

    private fun disconnectFromDatabase(){
        if(this.connection != null){
            this.connection!!.close();
        }
    }

    private fun databaseExists():Boolean{
        val file: File = File(DBNAME);
        return file.exists();
    }

    private fun setupTables(){
        if(this.databaseExists()){
            val statement = this.connection!!.createStatement();
            statement.executeUpdate(this.CREATE_ADDRESS_PAIR_TABLE);
        }
    }

    override fun loadAll(): Set<AddressPair> {
        this.lock.lock();
        this.connectToDatabase();

        var collection = LinkedHashSet<AddressPair>();
        val statement = this.connection!!.createStatement();

        val sql =
            "SELECT * FROM ADDRESSPAIRS";
        val results = statement.executeQuery(sql);


        while(!results.isAfterLast){

            val localPort = results.getInt("LOCALPORT");
            val destIP = results.getString("DESTIP");
            val destPort = results.getInt("DESTPORT");
            val type = results.getString("TYPE");

            val inetAddr = InetSocketAddress(destIP, destPort)
            val addressPair = AddressPair(localPort = localPort, dest = inetAddr, type = type);

            collection.add(addressPair);

            results.next();
        }

        this.disconnectFromDatabase();
        this.lock.unlock();
        return collection;
    }

    override fun deleteAddress(addressPair: AddressPair) {
        this.lock.lock();
        this.connectToDatabase();
        val localPort = addressPair.localPort;
        val destination = addressPair.dest;
        val type = addressPair.type;

        val statement = this.connection!!.createStatement();

        val sql =
                "DELETE FROM ADDRESSPAIRS WHERE LOCALPORT = '" +
                        localPort + "' AND DESTIP = '" + destination.hostString +
                        "'" + " AND DESTPORT = " + destination.port + " AND TYPE = '" + type + "'";
        statement.executeUpdate(sql);
        statement.close();
        this.disconnectFromDatabase();
        this.lock.unlock();
    }

    override fun saveAddress(addressPair: AddressPair) {
        this.lock.lock();
        this.connectToDatabase();
        val localPort = addressPair.localPort;
        val destination = addressPair.dest;
        val type = addressPair.type;

        val statement = this.connection!!.createStatement();
        val sql =
                "INSERT INTO ADDRESSPAIRS (LOCALPORT,DESTIP,DESTPORT,TYPE) " +
                "VALUES (" +
                        "'" + localPort + "'," +
                        "'" + destination.hostString + "'," +
                        "'" + destination.port + "'" +
                        "'" + type + "'" +
                        ");";

        statement.executeUpdate(sql);
        statement.close();
        this.disconnectFromDatabase();
        this.lock.unlock();

    }


}