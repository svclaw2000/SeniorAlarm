package ant.swcapp.data

class AlarmTime(val hour: Int, val minute: Int) {
    fun getString() : String {
        return String.format("%02d:%02d", hour, minute)
    }
}