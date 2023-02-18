package awitmc.plugins.awitpvp

import JsonFile.JsonFile
import awitmc.plugins.awitpvp.commands.AdminCommands
import awitmc.plugins.awitpvp.commands.cooldown
import awitmc.plugins.awitpvp.database.DataBase
import awitmc.plugins.awitpvp.events.PlayerListener
import awitmc.plugins.awitpvp.functions.toInt
import awitmc.plugins.awitpvp.models.Kit
import awitmc.plugins.awitpvp.models.Vip
import awitmc.plugins.awitpvp.ui.KitShop
import awitmc.plugins.awitpvp.utils.Serialize
import jdk.jfr.internal.LogLevel
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.properties.Delegates

class Awitpvp : JavaPlugin() {
    companion object {
        private val vips: MutableList<Vip> = mutableListOf()
        private val kits: MutableList<Kit> = mutableListOf()
        private val deletedFiles:MutableList<String> = mutableListOf("mia")
        private val bounties: HashMap<Player, Double> = hashMapOf()
        private val killCounter: HashMap<UUID, Int> = hashMapOf()
        private var plugin: Awitpvp by Delegates.notNull()
        private var f = System.getProperty("file.separator")

        fun get(): Awitpvp = plugin

        fun getKits() = kits

        private val suffixes: NavigableMap<Long, String> = TreeMap()

        fun addKillCount(player: Player, kill: Int) {
            killCounter[player.uniqueId] = (killCounter[player.uniqueId] ?: 0)+ kill
        }

        fun setKillCount(player: Player, kill: Int) {
            killCounter[player.uniqueId] = kill
        }

        fun addDeletedFile(name:String) {
            deletedFiles.add(name)
        }

        fun getKillCount(player: Player) = killCounter[player.uniqueId]?: 0

        fun format(value: Long): String {
            //Long.MIN_VALUE == -Long.MIN_VALUE, so we need an adjustment here
            if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1)
            if (value < 0) return "-" + format(-value)
            if (value < 1000) return value.toString() //deal with easy case
            val (divideBy, suffix) = suffixes.floorEntry(value)
            val truncated = value / (divideBy / 10) //the number part of the output times 10
            val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
            return if (hasDecimal) (truncated / 10.0).toString() + suffix else (truncated / 10).toString() + suffix
        }

        fun setBounty(player: Player, value: Double) {
            bounties[player] = value
        }

        fun increaseBounty(player: Player, value: Double) {
            bounties[player] = (bounties[player] ?: 0.0) + value
            val data = DataBase()
            data.setBounty(player, bounties[player]!!)
        }

        fun removeBounty(player: Player) {
            bounties.remove(player)
            val data = DataBase()
            data.setBounty(player, 0.0)
        }

        fun getBounty(player: Player): Double {
            return bounties[player]?: 0.0
        }

        fun hasBounty(player: Player): Boolean {
            return bounties.containsKey(player)
        }

        fun getVips(): List<Vip> {
            return vips
        }

        fun registerVip(vip: Vip):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}vips", vip.name)
            if(!file.exists()) {
                file.create()
                file["id"] = vip.id
                file["name"] = vip.name
                file["uuids"] = vip.uuids
                file["kits"] = vip.kits

                file.save()
                vips.add(vip)
                return true
            }
            return false
        }

        fun playerHasVip(player: Player, vip: Vip): Boolean {
            return vip.uuids.any { it == player.uniqueId.toString() }
        }

        fun getPlayerVip(player: Player): Vip? {
            return vips.firstOrNull { it.uuids.contains(player.uniqueId.toString()) }
        }

        fun getPlayerVips(player: Player): List<Vip> {
            return vips.filter { it.uuids.contains(player.uniqueId.toString()) }
        }

        fun addPlayerToVip(player: Player, vip: Vip):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}vips", vip.name)
            if(!file.exists())
                return false

            val uuids = file["uuids"] as List<String>
            if(uuids.contains(player.uniqueId.toString()))
                return false
            val newUuids = uuids + player.uniqueId.toString()
            file["uuids"] = newUuids
            vip.uuids = newUuids

            return true
        }

        fun removePlayerFromVip(player: Player, vip: Vip):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}vips", vip.name)
            if(!file.exists())
                return false
            val uuids = file["uuids"] as List<String>
            if(!uuids.contains(player.uniqueId.toString()))
                return false
            val newUuids = uuids - player.uniqueId.toString()
            file["uuids"] = newUuids
            vip.uuids = newUuids

            return true
        }

        fun unregisterVip(vip: Vip):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}vips", vip.name)
            if(file.exists()) {
                file.delete()
                vips.remove(vip)
                return true
            }
            return false
        }

        fun getVip(name: String): Vip? {
            return vips.firstOrNull { it.name == name }
        }

        fun getVip(id: Int): Vip? {
            return vips.firstOrNull { it.id == id }
        }

        fun registerKit(kit: Kit):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(file.exists())
                return false
            file.create()
            file["name"] = kit.name
            file["price"] = (kit.price ?: -1)
            file["levelToUnlock"] = (kit.levelToUnlock ?: -1)
            file["canBeUnlockedByLevel"] = kit.canBeUnlockedByLevel
            file["items"] = kit.items
            file["vipAccess"] = (kit.vipAccess)
            file["owners"] = kit.owners

            kits.add(kit)
            file.save()
            return true
        }

        fun unregisterKit(kit: Kit):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false

            kits.remove(kit)
            return file.delete()
        }

        fun getKit(name: String): Kit? {
            return kits.firstOrNull { it.name == name }
        }

        fun playerHasKit(player: Player, kit: Kit) : Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false

            val owners = file["owners"] as List<String>
            return owners.contains(player.uniqueId.toString())
        }

        fun saveKit(kit: Kit) : Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false

            file["name"] = kit.name
            file["price"] = kit.price
            file["levelToUnlock"] = kit.levelToUnlock
            file["canBeUnlockedByLevel"] = kit.canBeUnlockedByLevel
            file["items"] = kit.items
            file["vipAccess"] = kit.vipAccess
            file["owners"] = kit.owners

            file.save()
            return true
        }

        fun forceDeleteKit(kitName: String) : Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kitName)
            if(!file.exists())
                return false
            file.delete()
            return true
        }

        fun kitIncludesVip(kit: Kit, vip: Vip):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false
            val vips = file["vipAccess"] as List<String>
            if(vips.contains(vip.name))
                return true
            return false
        }

        fun playerClaimedKit(player: Player, kit: Kit):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false
            val owners = file["owners"] as List<String>
            if(owners.contains(player.uniqueId.toString()))
                return true
            return false
        }

        fun giveKitToPlayer(kit: Kit, player: Player):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false
            val owners = (file["owners"] as List<String>).toMutableList()
            if(!owners.contains(player.uniqueId.toString()))
                owners.add(player.uniqueId.toString())
            file["owners"] = owners.toList()
            kit.owners = owners.toList()
            file.save()
            val items = file["items"] as List<String>
            val armorsType = hashMapOf<String, List<Material>>(
                "helmet" to mutableListOf(Material.DIAMOND_HELMET, Material.GOLD_HELMET,Material.IRON_HELMET,Material.LEATHER_HELMET,Material.CHAINMAIL_HELMET),
                "chestplate" to mutableListOf(Material.DIAMOND_CHESTPLATE, Material.GOLD_CHESTPLATE, Material.IRON_CHESTPLATE, Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE,),
                "leggings" to mutableListOf(Material.DIAMOND_LEGGINGS, Material.GOLD_LEGGINGS,Material.IRON_LEGGINGS,Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS),
                "boots" to mutableListOf(Material.DIAMOND_BOOTS, Material.GOLD_BOOTS, Material.IRON_BOOTS, Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS)
            )
            val armors = listOf<Material>(

            )
            val kitArmor = mutableListOf<ItemStack>()
            items.forEach {
                val item = Serialize.itemFrom64(it) ?: ItemStack(Material.AIR)
                if(!armors.contains(item.type)) {
                    player.inventory.addItem(item)
                }else kitArmor.add(item)
            }
            if(player.inventory.helmet == null || player.inventory.helmet.type == Material.AIR) {
                player
            }
            return true
        }

        fun addVipToKit(vip: Vip, kit: Kit):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false
            val vips = ((file["vipAccess"]  ?: listOf<String>()) as List<String>).toMutableList()
            if(vips.contains(vip.name))
                return false
            vips.add(vip.name)
            file["vipAccess"] = vips.toList()
            kit.vipAccess = vips.toList()
            return true
        }

        fun removeVipFromKit(vip: Vip, kit: Kit):Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false
            val vips = ((file["vipAccess"] ?: listOf<String>()) as List<String>).toMutableList()
            if(!vips.contains(vip.name))
                return false
            vips.remove(vip.name)
            file["vipAccess"] = vips.toList()
            kit.vipAccess = vips.toList()
            return true
        }

        fun removeItemFromKit(kit: Kit, item: ItemStack) : Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false
            val serialized = Serialize.itemTo64(item) ?: return false
            val items = (file["items"] as List<String>).toMutableList()
            items.remove(serialized)
            println(items)
            file["items"] = items
            kit.items = items
            return true
        }

        fun addItemToKit(kit: Kit, item: ItemStack) : Boolean {
            val file = JsonFile("${plugin.dataFolder.path}${f}kits", kit.name)
            if(!file.exists())
                return false
            val serialized = Serialize.itemTo64(item) ?: return false
            val list = (file["items"] as List<String>).toMutableList()
            if(list.contains(serialized))
                return false
            list.add(serialized)
            println(list)
            file["items"] = list.toList()
            file.save()
            kit.items = list.toList()
            return true
        }

        var config:JsonFile by Delegates.notNull()
    }

    override fun onEnable() {
        // Plugin startup logic
        initializeVariables()
        registerCommands()
        registerListeners()

        logger.info("**************************************************")
        logger.info("\tLoading configurations...")
        val dataBaseConfigFile = JsonFile(dataFolder.path, "database")
        logger.info("${dataBaseConfigFile.exists()}")
        logger.info("${dataBaseConfigFile.getFile()?.path}")
        if(!dataBaseConfigFile.exists()) {
            dataBaseConfigFile.create(hashMapOf("host" to "45.88.108.162", "name" to "db_65365", "username" to "db_65365", "password" to "DB76beOknbD6DVJgkKNL"))
            dataBaseConfigFile.save()
            logger.info("Database file created at: ${dataFolder.path}")
        }

        Awitpvp.config = JsonFile(dataFolder.path, "config")
        if(!Awitpvp.config.exists()) {
            Awitpvp.config.create( hashMapOf(
                "cool-down" to true,
                "shop" to mapOf<String, Any>(
                    "title" to "&l&cKit &l&fShop",
                    "max page" to "&cNon c'è pagina successiva",
                    "min page" to "&cNon c'è pagina precedente",
                    "low cash" to "&cNon hai abbastanza soldi!",
                    "low level" to "&cSei troppo basso di livello!",
                    "no vip" to "&cNon hai il vip per comprare questo kit",
                    "kit bought" to "&aKit ottenuto!",
                    "cooldown" to "&c[!] &fManca ancora %time% secondi"
                ),
                "kits" to mapOf<String, Any>(
                    "discount purchased" to 50,
                    "discount" to "&c%discount%&f di sconto su questo kit",
                    "already purchased" to "&c%discount%&f di sconto su questo perché è di tuo possesso",
                    "price" to "&fPrezzo: &a%price%",
                    "level" to "&fLivello per sbloccarlo: &a%level%",
                    "vip" to "&fRichiede vip: &a%boolean%"
                )
            )
            )
            Awitpvp.config.save()
        }

        DataBase.InitializeDatabase()

        logger.info("Loading kits and vips...")
        object : BukkitRunnable() {
            override fun run() {
                // Load vips
                val vipsDirectory = File("${dataFolder.path}${f}vips")
                val listOfVipFiles = vipsDirectory.listFiles() ?: arrayOf()
                for(vip in listOfVipFiles) {
                    val file = JsonFile("${dataFolder.path}${f}vips${f}${vip.nameWithoutExtension}")
                    if(file.exists()) {
                        logger.info("\tLoaded vip file ${file.getName()}")
                        file["id"]?.let { Vip(it.toInt(), file["name"] as String, file["uuids"] as List<String>, file["kits"] as List<String>) }
                            ?.let { vips.add(it) }
                    }
                }
                logger.info("\tLoaded ${vips.size} vips")

                // Load kits
                val kitsDirectory = File("${dataFolder.path}/kits")
                val listOfKitFiles = kitsDirectory.listFiles() ?: arrayOf()
                listOfKitFiles.forEach { kit ->
                    val file = JsonFile("${dataFolder.path}/kits/${kit.nameWithoutExtension}")
                    if(file.exists()) {
                        logger.info("\tLoaded kit file ${file.getName()}")
                        kits.add(Kit(
                            file["name"].toString(),
                            if(file["price"] != null) file["price"]?.toInt()?.toDouble() else null,
                            file["levelToUnlock"]?.toInt() ?: -1,
                            file["canBeUnlockedByLevel"].toString().toBoolean(),
                            if(file["items"] != null) file["items"] as List<String> else listOf(),
                            if(file["vipAccess"] != null) file["vipAccess"] as List<String> else null,
                            if(file["owners"] != null) file["owners"] as List<String> else listOf()
                        ))
                    }
                }
                logger.info("\tLoaded ${kits.size} kits")
            }
        }.runTask(this)

        logger.info("\tPlugin loaded successfully")
        logger.info("**************************************************")
    }

    private fun initializeVariables() {
        plugin = this

        suffixes[1_000L] = "k"
        suffixes[1_000_000L] = "M"
        suffixes[1_000_000_000L] = "G"
        suffixes[1_000_000_000_000L] = "T"
        suffixes[1_000_000_000_000_000L] = "P"
        suffixes[1_000_000_000_000_000_000L] = "E"
    }

    private fun registerListeners() {
        Bukkit.getPluginManager().registerEvents(PlayerListener(), this)
        Bukkit.getPluginManager().registerEvents(KitShop(), this)
    }

    private fun registerCommands() {
        getCommand("awitpvp").executor = AdminCommands()
    }

    override fun onDisable() {
        // Plugin shutdown logic
        if(DataBase.connection != null) {
            DataBase().closeConnection()
        }
        JsonFile.getAllFiles().forEach {
            if(!deletedFiles.contains(it.getFile()?.nameWithoutExtension) )
                it.save()
        }
    }
}