package awitmc.plugins.awitpvp.models

data class Kit(val name:String, var price:Double?,var levelToUnlock: Int?,var canBeUnlockedByLevel: Boolean, var items: List<String>,var vipAccess: List<String>?, var owners: List<String>) {
}