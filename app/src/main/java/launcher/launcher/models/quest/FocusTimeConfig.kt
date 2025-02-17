package launcher.launcher.models.quest

data class FocusTimeConfig(
    var initialTime: String = "1",
    var finalTime: String = "5",
    var incrementTime: String = "15",
    var initialUnit: String = "h",
    var finalUnit: String = "h",
    var incrementUnit: String = "m"
)