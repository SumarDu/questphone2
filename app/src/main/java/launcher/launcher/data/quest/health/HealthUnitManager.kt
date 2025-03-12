package launcher.launcher.data.quest.health

// Unit mapping for display
fun getUnitForType(type: HealthTaskType): String = when (type) {
    HealthTaskType.STEPS -> "steps"
    HealthTaskType.CALORIES -> "cal"
    HealthTaskType.DISTANCE -> "km"
    HealthTaskType.SLEEP -> "hrs"
    HealthTaskType.WATER_INTAKE -> "l"
}


fun formatHealthData(type: HealthTaskType, value: Double): String {
    val unit = getUnitForType(type)
    return "${value.toInt()} $unit"
}