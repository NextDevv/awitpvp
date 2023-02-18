package awitmc.plugins.awitpvp.commands

import awitmc.plugins.awitpvp.Awitpvp
import awitmc.plugins.awitpvp.economy.Bank
import awitmc.plugins.awitpvp.functions.getKitLevel
import awitmc.plugins.awitpvp.functions.msg
import awitmc.plugins.awitpvp.functions.toPlayerOrNull
import awitmc.plugins.awitpvp.models.Kit
import awitmc.plugins.awitpvp.models.Vip
import awitmc.plugins.awitpvp.ui.KitShop
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*
import kotlin.collections.HashMap

val cooldown: HashMap<UUID, Long> = hashMapOf()

@Suppress("SENSELESS_COMPARISON")
class AdminCommands : CommandExecutor {
    override fun onCommand(
        sender: CommandSender?,
        command: Command?,
        label: String?,
        args: Array<out String>?
    ): Boolean {
        if(sender !is Player) {
            sender?.sendMessage("Only players can use this command.")
            return true
        }

        val player = sender as Player

        if(args.isNullOrEmpty()) {
            player.msg("&cUsage:&f /awitpvp <command>")
            return true
        }
        when(args[0].lowercase()) {
            "admin" -> {
                when(args[1].toLowerCase()) {
                    "vip" -> {
                        if(args.size < 3) {
                            player.msg("&a[?] &fSub-commands for &a[vip]&f: create, delete, list, view &a[vip], add-player")
                            return true
                        }

                        when(args[2].toLowerCase()) {
                            "create" -> {
                                if(args.size < 4) {
                                    player.sendMessage("&c[!] &fUsage: /awitpvp admin create <name>")
                                    return true
                                }

                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val vip = Vip(Awitpvp.getVips().size+1, name, listOf(), listOf())
                                val registered = Awitpvp.registerVip(vip)
                                if(registered) {
                                    player.msg("&a[?] &fCreated a new VIP with name &f$name")
                                }else player.msg("&c[!] &fThe VIP already exists")
                            }
                            "delete" -> {
                                if (args.size < 4) {
                                    player.sendMessage("&c[!] &fUsage: /awitpvp admin delete <name/id>")
                                    return true
                                }

                                var id = 0
                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                try {
                                    id = Integer.parseInt(args[4])
                                }catch (ignore: NumberFormatException) {}

                                val vip = Awitpvp.getVips().firstOrNull { it.name == name } ?: Awitpvp.getVips().firstOrNull { it.id == id }
                                if(vip!= null) {
                                    Awitpvp.unregisterVip(vip)
                                    Awitpvp.addDeletedFile(vip.name)
                                    player.msg("&a[?] &fDeleted a VIP with name &a$name")
                                }else {
                                    player.msg("&c[!] &fUnable to delete or find the vip &c$name")
                                }
                            }
                            "list" -> {

                                val list = Awitpvp.getVips()

                                player.msg("List of&a vips&f:")
                                for(vip in list) {
                                    player.msg("&a[${vip.id}] &fName: &a${vip.name}")
                                }
                            }
                            "view" -> {
                                if(args.size < 4) {
                                    player.msg("&c[!] &fUsage: /awitpvp admin view <name>")
                                    return true
                                }
                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val vip = Awitpvp.getVips().firstOrNull { it.name == name } ?: Awitpvp.getVips().firstOrNull { it.id == Integer.parseInt(args[3]) }
                                if(vip!= null) {
                                    player.msg("&fName: &a${vip.name}")
                                    player.msg("&fID: &a${vip.id}")
                                    player.msg("&fKits: &a${vip.kits}")
                                } else player.msg("&c[!] &fUnable to find the vip &c$name")
                            }

                            "add-player" -> {
                                if (args.size < 5) {
                                    player.msg("&c[!] &fUsage: /awitpvp admin add-player <name> <vip>")
                                    return true
                                }
                                var name = ""
                                for(i in 4 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val vip = Awitpvp.getVips().firstOrNull { it.name == name }?: Awitpvp.getVips().firstOrNull { it.id == Integer.parseInt(args[3]) }
                                if(vip!= null) {
                                    val target = args[3].toPlayerOrNull()
                                    if(target==null) {
                                        player.msg("&c[!] &fUnable to find the target player")
                                        return true
                                    }
                                    val success = Awitpvp.addPlayerToVip(target, vip)
                                    if(success) {
                                        player.msg("&a[+] &fAdded a new player &a${target.name} to the vip &a${vip.name}")
                                    }else player.msg("&c[!] &fUnable to add the player to the vip &c${vip.name}")
                                }else player.msg("&c[!] &fUnable to find the vip &c$name")
                            }

                            "remove-player" -> {
                                if (args.size < 5) {
                                    player.msg("&c[!] &fUsage: /awitpvp admin add-player <vip> <name>")
                                    return true
                                }
                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val vip = Awitpvp.getVips().firstOrNull { it.name == name }?: Awitpvp.getVips().firstOrNull { it.id == Integer.parseInt(args[3]) }
                                if(vip!= null) {
                                    val target = args[3].toPlayerOrNull()
                                    if(target==null) {
                                        player.msg("&c[!] &fUnable to find the target player")
                                        return true
                                    }
                                    val success = Awitpvp.removePlayerFromVip(target, vip)
                                    if(success) {
                                        player.msg("&a[+] &fRemoved the player &a${target.name} to the vip &a${vip.name}")
                                    }else player.msg("&c[!] &fUnable to remove the player to the vip &c${vip.name}")
                                }else player.msg("&c[!] &fUnable to find the vip &c$name")
                            }

                            else -> {player.msg("&a[?] &fSub-commands for &a[vip]&f: create &a[vip]&f, delete &a[vip]&f, list, view &a[vip]&f, add-player &a[vip]&f &a[name]&f, remove-player &a[vip]&f &a[name]&f")}
                        }
                    }

                    "balance" -> {
                        if(args.size < 3) {
                            player.msg("&a[?]&f Sub-commands: add, remove, set")
                            return true
                        }

                        when(args[2].lowercase()) {
                            "add" -> {
                                if (args.size < 5) {
                                    player.msg("&a[?]&f Usage: /awitpvp balance add <name> <amount>")
                                    return true
                                }
                                val target = args[3].toPlayerOrNull()
                                if(target==null) {
                                    player.msg("&a[!] &fUnable to find the target player")
                                    return true
                                }
                                var amount = 0.0
                                try {
                                    amount = Integer.parseInt(args[4]).toDouble()
                                }catch (e:NumberFormatException) {
                                    player.msg("&a[!] &fInvalid amount value")
                                    return true
                                }
                                Bank.addCash(target, amount)
                                player.msg("&a[+] &fAdded the player &a${target.name}")
                                return true
                            }

                            "set" -> {
                                if (args.size < 5) {
                                    player.msg("&a[?]&f Usage: /awitpvp balance set <name> <amount>")
                                    return true
                                }
                                val target = args[3].toPlayerOrNull()
                                if(target==null) {
                                    player.msg("&a[!] &fUnable to find the target player")
                                    return true
                                }
                                var amount = 0.0
                                try {
                                    amount = Integer.parseInt(args[4]).toDouble()
                                }catch (e:NumberFormatException) {
                                    player.msg("&a[!] &fInvalid amount value")
                                    return true
                                }
                                Bank.setCash(target, amount)
                                player.msg("&a[+] &fSet the player &a${target.name} balance")
                                return true
                            }

                            "remove" -> {
                                if (args.size < 5) {
                                    player.msg("&a[?]&f Usage: /awitpvp balance set <name> <amount>")
                                    return true
                                }
                                val target = args[3].toPlayerOrNull()
                                if(target==null) {
                                    player.msg("&a[!] &fUnable to find the target player")
                                    return true
                                }
                                var amount = 0.0
                                try {
                                    amount = Integer.parseInt(args[4]).toDouble()
                                }catch (e:NumberFormatException) {
                                    player.msg("&a[!] &fInvalid amount value")
                                    return true
                                }
                                Bank.removeCash(target, amount)
                                player.msg("&a[+] &fRemoved cash from the player &a${target.name} balance")
                                return true
                            }
                        }
                    }

                    "kit" -> {
                        if(args.size < 3) {
                            player.msg("&a[?] &fSub-commands for &a[kit]&f: create, delete, list, view &a[vip]&f, add-kit &a[vip]&f, remove-kit &a[vip]&f")
                            return true
                        }

                        when(args[2].lowercase()) {
                            "create" -> {
                                if (args.size < 4) {
                                    player.msg("&a[?] &fUsage:&a /awitpvp admin create &a[name]")
                                    return true
                                }
                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val kit = Kit(name, null, null, false, listOf(), null, listOf())
                                val registered = Awitpvp.registerKit(kit)
                                if(registered) {
                                    player.msg("&a[+] &fCreated a new kit &a${kit.name}")
                                }else player.msg("&a[!] &fThe kit already exists")
                            }

                            "delete" -> {
                                if (args.size < 4) {
                                    player.msg("&a[?] &fUsage:&a /awitpvp admin delete &a[name]")
                                    return true
                                }
                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val kit = Awitpvp.getKit(name)
                                if (kit == null) {
                                    player.msg("&a[!] &fUnable to find the kit &a$name")
                                    return true
                                }
                                Awitpvp.unregisterKit(kit)
                                Awitpvp.addDeletedFile(kit.name)
                                player.msg("&a[+] &fDeleted &a${kit.name}")
                            }

                            "list" -> {
                                val list = Awitpvp.getKits()
                                if (list.isEmpty()) {
                                    player.msg("&a[!] &fNo kit found")
                                    return true
                                }
                                for (kit in list) {
                                    player.msg("&a[+] &f${kit.name}")
                                }
                                player.msg("&a[?] &fYou can use the following command to view the kit: /awitpvp admin kit view")
                            }

                            "force-delete" -> {
                                if (args.size < 4) {
                                    player.msg("&a[?] &fUsage:&a /awitpvp admin kit force-delete &a[name]")
                                    return true
                                }
                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val success = Awitpvp.forceDeleteKit(name)
                                if(success) {
                                    player.msg("&a[+] &fDeleted &a${name}")
                                }else player.msg("&a[!] &fUnable to delete the kit &a$name")
                            }

                            "set-price" -> {
                                if (args.size < 5) {
                                    player.msg("&a[?] &fUsage:&a /awitpvp admin kit set-price &a[price] &a[name]")
                                    player.msg("&a[?] &fTo make that no-one can buy the kit make the price 'null'")
                                    return true
                                }
                                var name = ""
                                for(i in 4 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                var price:Double? = null
                                try {
                                    price = Integer.parseInt(args[3]).toDouble()
                                }catch (e: NumberFormatException){
                                    if(args[4].lowercase() != "null"){
                                        player.msg("&a[!] &fUnable to parse the price &a${args[3]}")
                                        return true
                                    }
                                }

                                val kit = Awitpvp.getKit(name)
                                if (kit == null) {
                                    player.msg("&c[!] &fUnable to find the kit &c$name")
                                    return true
                                }
                                kit.price = price
                                val saved = Awitpvp.saveKit(kit)
                                if (saved) {
                                    player.msg("&a[+] &fPrice changed for the kit &a${kit.name}")
                                }else player.msg("&a[!] &fFailed to save and change the price for the kit &a${kit.name}")
                            }

                            "add-item" -> {
                                if (args.size < 4) {
                                    player.msg("&a[?] &fUsage:&a /awitpvp admin kit add-item <kit>")
                                    return true
                                }
                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val kit = Awitpvp.getKit(name)
                                if (kit == null) {
                                    player.msg("&c[!] &fUnable to find the kit &c$name")
                                    return true
                                }
                                val item = player.inventory.itemInHand
                                if(item.type == Material.AIR) {
                                    player.msg("&c[!] &fUnable to add an item to the kit")
                                    player.msg("&c[!] &fReason: The item in the hand is AIR")
                                    return true
                                }
                                val added = Awitpvp.addItemToKit(kit, item)
                                if(added) {
                                    player.msg("&a[+] &fAdded an item to the kit &a${kit.name}")
                                }else player.msg("&c[!] &fFailed to add an item to the kit")
                            }

                            "remove-item" -> {
                                if (args.size < 4) {
                                    player.msg("&a[?] &fUsage:&a /awitpvp admin kit add-item <kit>")
                                    return true
                                }
                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val kit = Awitpvp.getKit(name)
                                if (kit == null) {
                                    player.msg("&c[!] &fUnable to find the kit &c$name")
                                    return true
                                }
                                val item = player.inventory.itemInHand
                                if(item.type == Material.AIR) {
                                    player.msg("&c[!] &fUnable to remove the item from the kit")
                                    player.msg("&c[!] &fReason: The item in the hand is AIR")
                                    return true
                                }
                                val added = Awitpvp.removeItemFromKit(kit, item)
                                if(added) {
                                    player.msg("&a[+] &fRemoved the item from the kit &a${kit.name}")
                                }else player.msg("&c[!] &fFailed to remove an item from the kit")
                            }

                            "assign-vip" -> {
                                if (args.size < 5) {
                                    player.msg("&a[?] &fUsage:&a /awitpvp admin kit assign-vip <vip> <kit>")
                                    return true
                                }
                                var name = ""
                                for(i in 4 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val kit = Awitpvp.getKit(name)
                                if (kit == null) {
                                    player.msg("&c[!] &fUnable to find the kit &c$name")
                                    return true
                                }
                                val vip = Awitpvp.getVip(args[3])
                                if (vip == null) {
                                    player.msg("&c[!] &fUnable to find the vip &c${args[3]}")
                                    return true
                                }
                                val added = Awitpvp.addVipToKit(vip, kit)
                                if(added) {
                                    player.msg("&a[+] &fAdded a vip to the kit &a${kit.name}")
                                }else player.msg("&c[!] &fUnable to add the vip")
                            }

                            "remove-vip" -> {
                                if (args.size < 5) {
                                    player.msg("&a[?] &fUsage:&a /awitpvp admin kit assign-vip <vip> <kit>")
                                    return true
                                }
                                var name = ""
                                for(i in 3 until args.size) {
                                    name += args[i] + " "
                                }
                                name = name.trimEnd()
                                val kit = Awitpvp.getKit(name)
                                if (kit == null) {
                                    player.msg("&c[!] &fUnable to find the kit &c$name")
                                    return true
                                }
                                val vip = Awitpvp.getVip(args[3])
                                if (vip == null) {
                                    player.msg("&c[!] &fUnable to find the vip &c${args[3]}")
                                    return true
                                }
                                val added = Awitpvp.removeVipFromKit(vip, kit)
                                if(added) {
                                    player.msg("&a[+] &fRemoved vip from the kit &a${kit.name}")
                                }else player.msg("&c[!] &fUnable to add the vip")
                            }
                        }
                    }
                }
            }

            "claim" -> {
                if(args.size < 2) {
                    player.msg("&a[?] &fUsage:&a /awitpvp claim <kit>")
                    return true
                }
                var name = ""
                for(i in 1 until args.size) {
                    name += args[i] + " "
                }
                name = name.trimEnd()
                val kit = Awitpvp.getKit(name)
                if(kit == null) {
                    player.msg("&c[!] &fImpossibile trovare il kit")
                    println("no kit")
                    return true
                }
                if(kit.price == null || Bank.getCash(player) < kit.price!!) {
                    player.msg("&c[!]&f Non puoi comprare il kit!")
                    println("low cash")
                    return true
                }
                if(kit.vipAccess != null) {
                    println("require vip")
                    val playerVips = Awitpvp.getPlayerVips(player)
                    if(playerVips.isEmpty()){
                        player.msg("&c[!] &fNon hai vip per ottenere il kit!")
                        return true
                    }
                    playerVips.forEach {
                        if(Awitpvp.kitIncludesVip(kit, it)) {
                            // Check if the cool down is over (cooldown = 5min)
                            if(!cooldown.containsKey(player.uniqueId) || System.currentTimeMillis() - cooldown[player.getUniqueId()]!! > 60000) {
                                cooldown[player.uniqueId] = System.currentTimeMillis()
                                Awitpvp.giveKitToPlayer(kit, player)
                                player.msg("&a[+]&f Kit ottenuto!")
                            }else {
                                player.msg("&c[!] &fManca ancora ${60 - ((System.currentTimeMillis() - cooldown[player.uniqueId]!!) / 1000)} secondi")
                            }
                        }
                    }
                }

                if(kit.levelToUnlock != null && kit.levelToUnlock!! > player.getKitLevel()) {
                    println("low level")
                    player.msg("&c[!] &fNon hai un livello sufficiente")
                    return true
                }

                if(!cooldown.containsKey(player.uniqueId) || System.currentTimeMillis() - cooldown[player.getUniqueId()]!! > 60000) {
                    cooldown[player.uniqueId] = System.currentTimeMillis()
                    var playerMoney = Bank.getCash(player)
                    playerMoney -= if(kit.price!!.toInt() == -1) 0 else kit.price!!.toInt()
                    Bank.setCash(player, playerMoney.toDouble())
                    Awitpvp.giveKitToPlayer(kit, player)
                    player.msg("&a[+]&f Kit ottenuto!")
                }else {
                    player.msg("&c[!] &fManca ancora ${60 - ((System.currentTimeMillis() - cooldown[player.uniqueId]!!) / 1000)} secondi")
                }
            }

            "bounty" -> {
                if(args.size < 2) {
                    player.msg("&a[?]&f La tua bounty è di ${Awitpvp.getBounty(player)}")
                    return true
                }

                val target = args[1].toPlayerOrNull()
                if(target == null) {
                    player.msg("&c[!]&f Player non trovato")
                    return true
                }

                val bounty = Awitpvp.getBounty(target)
                player.msg("&a[$]&f La bounty di ${target.name} è di $${bounty}!")
            }

            "kit-shop" -> {
                KitShop.openShopUI(player)
            }

            "balance" -> {
                if(args.size < 2) {
                    player.msg("&a[$]&f Hai &a$${Bank.getCash(player)}&f in banca")
                    return true
                }
                val target = args[1].toPlayerOrNull()
                if(target == null) {
                    player.msg("&c[!]&f Player non trovato")
                    return true
                }
                val balance = Bank.getCash(target)
                player.msg("&a[$]&f Il player &a$${target.name}&f ha $${balance}!")
            }

            "add-bounty" -> {
                if(args.size < 3) {
                    player.msg("&c[!]&f Usage: /awitpvp add-bounty <bounty> <player>")
                    player.msg("&c[!]&f Oppure /bounty <bounty> <player>")
                    return true
                }

                val target = args[2].toPlayerOrNull()
                if(target == null) {
                    player.msg("&c[!]&f Player non trovato")
                    return true
                }

                val bountyToSet: Double
                try {
                    bountyToSet = Integer.parseInt(args[1]).toDouble()
                }catch (e:NumberFormatException) {
                    player.msg("&c[!] &fNumero non valido")
                    return true
                }

                if(Bank.getCash(player) < bountyToSet) {
                    player.msg("&c[!] &fNon hai abbastanza soldi!")
                    return true
                }

                Awitpvp.increaseBounty(target, bountyToSet)
                Bank.removeCash(player, bountyToSet)
                player.msg("&a[+]&f Bounty di &a${target.name}&f è stata aumentata di &a$${bountyToSet}!")
                if(target.isOnline)
                    target.msg("&c[!]&f Qualcuno ha messo una taglia su di te dà &c$${bountyToSet}&f!")
            }
        }


        return true
    }

}