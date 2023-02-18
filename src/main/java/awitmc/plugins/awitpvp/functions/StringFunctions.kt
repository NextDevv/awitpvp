package awitmc.plugins.awitpvp.functions

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player

fun String.tac(code: Char = '&'): String {
    return ChatColor.translateAlternateColorCodes(code, this)
}

fun Any.toInt():Int = Integer.parseInt(this.toString().replace(".0", "")) ?: 0

fun String.toPlayer(): Player {
    return Bukkit.getPlayer(this)
}

fun String.toPlayerOrNull(): Player? {
    return Bukkit.getPlayer(this) ?: null
}

fun String.toExactPlayer(): Player {
    return Bukkit.getPlayerExact(this)
}