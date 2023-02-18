package awitmc.plugins.awitpvp.functions

import awitmc.plugins.awitpvp.database.DataBase
import org.bukkit.entity.Player

fun Player.msg(msg: String = "") {
    this.sendMessage(msg.tac())
}

fun Player.getKitLevel():Int {
    val data = DataBase()
    return data.getLevel(this)
}