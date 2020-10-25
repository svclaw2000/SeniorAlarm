package ant.swcapp.data

class AlarmRepeat(
    var sun : Boolean = false,
    var mon : Boolean = false,
    var tue : Boolean = false,
    var wed : Boolean = false,
    var thu : Boolean = false,
    var fri : Boolean = false,
    var sat : Boolean = false
) {
    fun getString() : String {
        val repeat = arrayListOf<String>()
        if (sun) repeat.add("일")
        if (mon) repeat.add("월")
        if (tue) repeat.add("화")
        if (wed) repeat.add("수")
        if (thu) repeat.add("목")
        if (fri) repeat.add("금")
        if (sat) repeat.add("토")

        if (repeat.size == 7) {
            return "매일"
        } else if (repeat.size == 0) {
            return "반복 없음"
        }

        return repeat.joinToString(", ")
    }

    fun getBooleanArray() : BooleanArray {
        return booleanArrayOf(sun, mon, tue, wed, thu, fri, sat)
    }

    fun getCount() : Int {
        var count = 0
        if (sun) count += 1
        if (mon) count += 1
        if (tue) count += 1
        if (wed) count += 1
        if (thu) count += 1
        if (fri) count += 1
        if (sat) count += 1
        return count
    }

    fun setBooleanArray(boolArray: BooleanArray) {
        if (boolArray.size != 7) return
        sun = boolArray[0]
        mon = boolArray[1]
        tue = boolArray[2]
        wed = boolArray[3]
        thu = boolArray[4]
        fri = boolArray[5]
        sat = boolArray[6]
    }

    fun copy() : AlarmRepeat {
        return AlarmRepeat(sun, mon, tue, wed, thu, fri, sat)
    }
}