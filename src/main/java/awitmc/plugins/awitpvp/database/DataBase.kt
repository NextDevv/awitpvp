package awitmc.plugins.awitpvp.database

import JsonFile.JsonFile
import JsonFile.JsonFile.Methods.get
import awitmc.plugins.awitpvp.Awitpvp
import org.bukkit.configuration.Configuration
import org.bukkit.entity.Player
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*

class DataBase {
    fun getLevel(player: Player): Int {
        return try {
            val statement = connection!!.prepareStatement("SELECT U_LEVEL FROM AwitPVP_V1 WHERE UUID =?")
            statement.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                return resultSet.getInt("U_LEVEL")
            }
            statement.close()
            0
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun setKills(player: Player, kills: Int) {
        return try {
            val statement = connection!!.prepareStatement("UPDATE AwitPVP_V1 SET KILLS =? WHERE UUID =?")
            statement.setInt(1, kills)
            statement.setString(2, player.uniqueId.toString())
            statement.executeUpdate()
            statement.close()
        }catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun getKills(player: Player): Int {
        return try {
            val statement = connection!!.prepareStatement("SELECT KILLS FROM AwitPVP_V1 WHERE UUID =?")
            statement.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                return resultSet.getInt("KILLS")
            }
            statement.close()
            0
        }catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun setBounty(player: Player, amount: Double) {
        return try {
            val statement = connection!!.prepareStatement("UPDATE AwitPVP_V1 SET BOUNTY =? WHERE UUID =?")
            statement.setDouble(1, amount)
            statement.setString(2, player.uniqueId.toString())
            statement.execute()
            statement.close()
        }catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun getBounty(player: Player): Double {
        return try {
            val statement = connection!!.prepareStatement("SELECT BOUNTY FROM AwitPVP_V1 WHERE UUID =?")
            statement.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                return resultSet.getDouble("BOUNTY")
            }
            statement.close()
            0.0
        }catch (e: SQLException) {
            e.printStackTrace()
            0.0
        }
    }

    fun hasData(player: Player): Boolean {
        return try {
            val statement = connection!!.prepareStatement("SELECT * FROM AwitPVP_V1 WHERE UUID =?")
            statement.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                return true
            }
            statement.close()
            false
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
    }

    fun createData(player: Player, level: Int, cash: Double, bounty: Double, kills: Int) {
        try {
            val statement = Objects.requireNonNull(
                connection
            )!!.prepareStatement("INSERT INTO AwitPVP_V1(UUID, USER,U_LEVEL, CASH, BOUNTY, KILLS) VALUES(?,?,?,?,?,?)")
            statement.setString(1, player.uniqueId.toString())
            statement.setString(2, player.name)
            statement.setInt(3, level)
            statement.setDouble(4, cash)
            statement.setDouble(5, bounty)
            statement.setInt(6, kills)
            statement.execute()
            statement.close()
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
    }

    fun updateData(player: Player, level: Int, cash: Double, bounty: Double, kills: Int) {
        try {
            val statement = Objects.requireNonNull(
                connection
            )
                ?.prepareStatement("UPDATE AwitPVP_V1 SET U_LEVEL = ?, CASH = ?, BOUNTY = ?, KILLS = ? WHERE UUID = ?")
            statement!!.setInt(1, level)
            statement.setDouble(2, cash)
            statement.setDouble(3, bounty)
            statement.setInt(4, kills)
            statement.setString(5, player.uniqueId.toString())
            statement.executeUpdate()
            statement.close()
        } catch (e: SQLException) {
            println(prefix + "Unable to update player database: " + e.message)
            e.printStackTrace()
        }
    }

    fun setCash(player: Player, cash: Double) {
        try {
            val statement = Objects.requireNonNull(
                connection
            )!!.prepareStatement("UPDATE AwitPVP_V1 SET CASH =? WHERE UUID =?")
            statement.setDouble(1, cash)
            statement.setString(2, player.uniqueId.toString())
            statement.executeUpdate()
            statement.close()
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    fun getCash(player: Player): Double {
        try {
            val statement = Objects.requireNonNull(
                connection
            )
                ?.prepareStatement("SELECT CASH FROM AwitPVP_V1 WHERE UUID =?")
            statement!!.setString(1, player.uniqueId.toString())
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                return resultSet.getDouble("CASH")
            }
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
        return -1.0
    }

    fun deleteData(player: Player) {
        try {
            val statement = connection
                ?.prepareStatement("DELETE FROM AwitPVP_V1 WHERE UUID = ?")
            statement!!.setString(1, player.uniqueId.toString())
            statement.executeUpdate()
            statement.close()
        } catch (e: SQLException) {
            println(prefix + "Unable to delete player database: " + e.message)
        }
    }

    fun closeConnection() {
        try {
            Objects.requireNonNull(connection)!!.close()
        } catch (e: SQLException) {
            println(prefix + "Unable to close connection: " + e.message)
        }
    }

    companion object {
        var connection: Connection? = null
            get() {
                if (field != null) {
                    return field
                }
                if (file == null || !file!!.exists()) {
                    println("Configuration file not found")
                    println("Connection to database terminated")
                    return null
                }
                val host = file!!.getString("host")
                val name = file!!.getString("name")
                val username = file!!.getString("username")
                val password = file!!.getString("password")
                if (host == "") {
                    plugin.logger.warning("The database info is empty plase fill it.")
                    return null
                }
                val url = "jdbc:mysql://$host/$name?characterEncoding=utf8"
                val url2 =
                    "jdbc:mysql://u16337_NHjun5v66k:4tINWb43fMdCer8b^@E7d.!!@sql1.revivenode.com:3306/s16337_HiddenChests?characterEncoding=utf8"
                try {
                    field = DriverManager.getConnection(url, username, password)
                    println(prefix + "Connected to the database")
                } catch (e: SQLException) {
                    println(prefix + "Failed to connect to the database: " + e.message)
                }
                return field
            }
            private set
        var prefix = "[AwitPVP] (DataBase) "
        private var plugin: Awitpvp = Awitpvp.get()
        var config: Configuration = plugin.config
        private var file: JsonFile? = JsonFile["database"]
        fun InitializeDatabase() {
            try {
                val statement = connection!!.createStatement()
                val sql = "CREATE TABLE IF NOT EXISTS AwitPVP_V1(UUID varchar(36), USER text, U_LEVEL int, CASH double, BOUNTY double, KILLS int);"
                statement.execute(sql)
                println(prefix + "Successfully created the table in the database")
                statement.close()
            } catch (e: SQLException) {
                println(prefix + "Failed to create the table in the database: " + e.message)
                e.printStackTrace()
            }
        }
    }
}