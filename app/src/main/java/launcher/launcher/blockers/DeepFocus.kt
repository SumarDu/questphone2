package launcher.launcher.blockers

class DeepFocus {
    var exceptionApps: HashSet<String> = hashSetOf()
    var isRunning: Boolean = false

    fun doesAppNeedToBeBlocked(packageName: String): Boolean {
        return if(isRunning){
            !exceptionApps.contains(packageName)
        }else{
            false
        }
    }
}