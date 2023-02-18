package awitmc.plugins.awitpvp.ui

import JsonFile.JsonFile
import awitmc.plugins.awitpvp.Awitpvp
import awitmc.plugins.awitpvp.commands.cooldown
import awitmc.plugins.awitpvp.economy.Bank
import awitmc.plugins.awitpvp.functions.getKitLevel
import awitmc.plugins.awitpvp.functions.msg
import awitmc.plugins.awitpvp.functions.tac
import awitmc.plugins.awitpvp.functions.toInt
import awitmc.plugins.awitpvp.models.Kit
import awitmc.plugins.awitpvp.utils.Serialize
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

class KitShop :Listener{
    private val config = Awitpvp.config
    private val shopConfig = config["shop"] as Map<String, Any>
    private val kitConfig = config["kits"] as Map<String, Any>

    companion object {
        private var currentPage = 1
        private val blacklistNums = listOf<Int>(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34)

        private val config = Awitpvp.config
        private val shopConfig = config["shop"] as Map<String, Any>
        private val kitConfig = config["kits"] as Map<String, Any>

        fun openShopUI(player: Player, page:Int = 1) {
            val shop = Bukkit.createInventory(null, 9*5, shopConfig["title"].toString().tac())
            currentPage = page
            for(i in 0 until 9*5) {
                if(!blacklistNums.contains(i) && i != 39 && i != 40 && i != 41) {
                    shop.setItem(i, ItemStack(Material.STAINED_GLASS, 1, 2.toShort()))
                }
            }
            shop.setItem(39, ItemStack(Material.STAINED_GLASS, 1, 14.toShort()))
            shop.setItem(40, ItemStack(Material.PAPER, page))
            shop.setItem(41, ItemStack(Material.STAINED_GLASS, 1, 13.toShort()))

            val kits = Awitpvp.getKits()

            val pages = hashMapOf<Int, List<Kit>>()
            var i = 1
            var p = 1
            var list = mutableListOf<Kit>()
            kits.forEach {
                if(it.price == -1.0)
                    it.price = null
                if(it.levelToUnlock == -1)
                    it.levelToUnlock = null
                list.add(it)
                if(i == 21 || i == kits.size){
                    println(p)
                    pages[p] = list;p++
                    list = mutableListOf()
                }
                i++
            }
            pages[page]?.forEach { kit ->
                val item = ItemStack(Material.OBSIDIAN, 1)
                val meta = item.itemMeta
                var owns = false

                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                if(kit.owners.contains(player.uniqueId.toString())) {
                    meta.addEnchant(Enchantment.ARROW_FIRE, 1,true)
                    owns = true
                }

                meta.displayName = kit.name
                val items = mutableListOf<ItemStack>()
                kit.items.forEach { items.add(Serialize.itemFrom64(it) ?: ItemStack(Material.AIR)) }
                val lore = listOf<String>(
                    if(owns) kitConfig["already purchased"]
                        .toString()
                        .replace("%discount%", kitConfig["discount purchased"]?.toInt().toString() ?: "0")
                        .tac()
                    else "",
                    kitConfig["price"]
                        .toString()
                        .replace("%price%",kit.price.toString())
                        .tac(),
                    kitConfig["level"]
                        .toString()
                        .replace("%level%",kit.levelToUnlock.toString())
                        .tac(),
                    kitConfig["level"]
                        .toString()
                        .replace("%boolean%",if(kit.vipAccess == null) "no" else "si")
                        .tac(),
                )
                meta.lore = lore

                item.itemMeta = meta

                shop.addItem(item)
            }

            player.openInventory(shop)
        }
    }



    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {

        if(event.clickedInventory == null || event.clickedInventory.title != shopConfig["title"].toString().tac())
            return
        if(event.clickedInventory.type == InventoryType.PLAYER)
            return
        val player = event.whoClicked as Player
        event.isCancelled = true



        if(event.slot == 41) {
            if(Awitpvp.getKits().size >= 21) {
                player.closeInventory()
                openShopUI(player, currentPage++)
            }else player.msg(shopConfig["max page"].toString().tac())
        }
        if(event.slot == 39) {
            if (currentPage == 1) {
                player.msg(shopConfig["min page"].toString().tac())
            }else{
                player.closeInventory()
                println("clicked ${currentPage--}")
                openShopUI(player, currentPage--)
            }
        }

        val inv = event.clickedInventory

        if(blacklistNums.contains(event.slot)) {
            val kit = Awitpvp.getKit(inv.getItem(event.slot).itemMeta.displayName) ?: return
            val config = JsonFile["config"]
            var cd = true
            if(config != null) {
                cd = config.getBoolean("cool-down")
            }

            if(kit.price == null)
                kit.price = 0.0
            else if(kit.owners.contains(player.uniqueId.toString())) {
                kit.price = kit.price!! / 2
            }
            if(Bank.getCash(player) < kit.price!!) {
                player.msg(shopConfig["low cash"].toString().tac())
                return
            }
            if(kit.vipAccess != null) {
                println("require vip")
                val playerVips = Awitpvp.getPlayerVips(player)
                if(playerVips.isEmpty()){
                    player.msg(shopConfig["no vip"].toString().tac())
                    return
                }
                playerVips.forEach {
                    if(Awitpvp.kitIncludesVip(kit, it)) {
                        // Check if the cool down is over (cooldown = 5min)
                        if(cd || (!cooldown.containsKey(player.uniqueId) || System.currentTimeMillis() - cooldown[player.getUniqueId()]!! > 60000)) {
                            cooldown[player.uniqueId] = System.currentTimeMillis()
                            Awitpvp.giveKitToPlayer(kit, player)
                            player.msg(shopConfig["kit bought"].toString().tac())
                            player.closeInventory()
                        }else {
                            player.msg(
                                shopConfig["cooldown"]
                                    .toString()
                                    .replace(
                                        "%time%",
                                        (60 - ((System.currentTimeMillis() - cooldown[player.uniqueId]!!) / 1000))
                                            .toString()
                                    )
                                    .tac()
                            )
                        }
                    }
                }
            }

            if(kit.levelToUnlock != null && kit.levelToUnlock!! > player.getKitLevel()) {
                println("low level")
                player.msg(shopConfig["low level"].toString().tac())
                return
            }


            if(cd || (!cooldown.containsKey(player.uniqueId) || System.currentTimeMillis() - cooldown[player.getUniqueId()]!! > 60000)) {
                cooldown[player.uniqueId] = System.currentTimeMillis()
                var playerMoney = Bank.getCash(player)
                playerMoney -= if(kit.price!!.toInt() == -1) 0 else kit.price!!.toInt()
                Bank.setCash(player, playerMoney.toDouble())
                Awitpvp.giveKitToPlayer(kit, player)
                player.msg(shopConfig["kit bought"].toString().tac())
                player.closeInventory()
            }else {
                player.msg(
                    shopConfig["cooldown"]
                        .toString()
                        .replace(
                            "%time%",
                            (60 - ((System.currentTimeMillis() - cooldown[player.uniqueId]!!) / 1000))
                                .toString()
                        )
                        .tac()
                )
            }
        }
    }
}