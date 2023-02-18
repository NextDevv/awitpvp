package awitmc.plugins.awitpvp.economy

import awitmc.plugins.awitpvp.database.DataBase
import awitmc.plugins.awitpvp.events.PlayerListener
import org.bukkit.entity.Player

class Bank {
    companion object {
        fun getCash(player: Player): Int {
            val data = DataBase()
            return data.getCash(player).toInt()
        }

        fun setCash(player: Player, amount: Double) {
            val data = DataBase()
            data.setCash(player, amount)
            PlayerListener().updateValue(player)
        }

        fun addCash(player: Player, amount: Double) {
            val data = DataBase()
            val cash = data.getCash(player)
            data.setCash(player, cash + amount)
            PlayerListener().updateValue(player)
        }

        fun removeCash(player: Player, amount: Double) {
            val data = DataBase()
            val cash = data.getCash(player)
            data.setCash(player, cash - amount)
            PlayerListener().updateValue(player)
        }
    }
}