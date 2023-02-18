package awitmc.plugins.awitpvp.events

import awitmc.plugins.awitpvp.Awitpvp
import awitmc.plugins.awitpvp.database.DataBase
import awitmc.plugins.awitpvp.economy.Bank
import awitmc.plugins.awitpvp.functions.msg
import awitmc.plugins.awitpvp.functions.tac
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Team
import java.util.*
import kotlin.collections.HashMap

class PlayerListener:Listener {
    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        val player = e.player ?: return
        val data = DataBase()

        if(!data.hasData(player)){
            data.createData(player, 1, 1000.00, 0.0, 0)
        }else {
            val kills = data.getKills(player)
            val bounty = data.getBounty(player)
            if(Awitpvp.getBounty(player) == 0.0){
                Awitpvp.setBounty(player, bounty)
            }
            if(Awitpvp.getKillCount(player) == 0) {
                Awitpvp.setKillCount(player, kills)
            }
        }
        scoreboard(player)
    }

    fun updateValue(player: Player) {
        val scoreboard = player.scoreboard
        scoreboard.getTeam("cash").suffix = "   &d${Awitpvp.format(Bank.getCash(player).toLong())}".tac()
        scoreboard.getTeam("kills").suffix = "   &d${Awitpvp.getKillCount(player)}".tac()
    }

    private fun scoreboard(player:Player) {
        object : BukkitRunnable() {
            val scoreboard = Bukkit.getScoreboardManager().newScoreboard
            val objective = scoreboard.registerNewObjective("awitpvp", "dummy")
            val user = scoreboard.registerNewTeam("user")
            val score2 = scoreboard.registerNewTeam("playerName")
            val score4 = scoreboard.registerNewTeam("kills")
            val score5 = scoreboard.registerNewTeam("cash")

            override fun run() {
                if(!player.isOnline)
                    this.cancel()

                objective.displayName = "&l&dAWIT &l&fPVP".tac()
                objective.displaySlot = DisplaySlot.SIDEBAR

                score2.addEntry("&f".tac())
                score2.suffix = "   &d${player.name}".tac()
                val score7 = objective.getScore("&f".tac())
                score7.score = 7
                val score8 = objective.getScore("&1".tac())
                score8.score = 6
                val score9 = objective.getScore("&2".tac())
                score9.score = 3
                val score11 = objective.getScore("&d".tac())
                score11.score = 1
                val score3 = objective.getScore("Kills &8»".tac())
                score3.score = 2
                score4.addEntry("&d".tac())
                score4.suffix = "   &d${Awitpvp.getKillCount(player)}".tac()
                score5.addEntry("&8".tac())
                score5.suffix = "   $&d${Awitpvp.format(Bank.getCash(player).toLong())}".tac()
                val score6 = objective.getScore("Soldi &8»".tac())
                score6.score = 5
                val score10 = objective.getScore("&8".tac())
                score10.score = 4
                val score = objective.getScore("Player &8»      ".tac())
                score.score = 9

                player.scoreboard = scoreboard
            }
        }.runTaskTimer(Awitpvp.get(), 40, 40)
    }

    @EventHandler
    fun onPlayerDeath(e: PlayerDeathEvent) {
        val player = e.entity ?: return
        val killer = player.killer

        if(killer != null) {
            val bounty = if(Awitpvp.getBounty(player) == 0.0) 100.0 else Awitpvp.getBounty(player)
            Bank.addCash(killer, bounty)
            killer.msg("&a[$]&f Hai ucciso ${player.name} e hai ricevuto &a$$bounty!")
            Awitpvp.addKillCount(killer, 1)
            Awitpvp.increaseBounty(killer, 50.0)
            Awitpvp.removeBounty(player)
            Bukkit.broadcastMessage("&d[!]&f La taglia su &d${player.name}&f di &d${Awitpvp.format(bounty.toLong())}&f è stata riscattata da &d${killer.name}".tac())
            updateValue(killer)
        }
    }

    @EventHandler
    fun onCommandProcess(e: PlayerCommandPreprocessEvent) {
        var command = e.message
        if(command.contains("/kit claim"))
            command = command.replace("/kit claim", "/awitpvp claim")
        if(command.contains("/kit"))
            command = command.replace("/kit", "/awitpvp kit-shop")
        if(command.contains("/bounty"))
            command = command.replace("/bounty", "/awitpvp add-bounty")
        if(command.contains("/sbounty"))
            command = command.replace("/sbounty", "/awitpvp bounty")

        e.message = command
    }
}