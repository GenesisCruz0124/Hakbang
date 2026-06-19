package ph.hakbang.app.ui.navigation

sealed class HakbangDestination(val route: String) {
    data object Home : HakbangDestination("home")
    data object History : HakbangDestination("history")
    data object Badges : HakbangDestination("badges")
    data object Settings : HakbangDestination("settings")
    data object Permission : HakbangDestination("permission")
}
