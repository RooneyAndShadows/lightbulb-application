package com.github.rooneyandshadows.lightbulb.application.activity.routing

import android.os.Parcel
import android.os.Parcelable
import com.github.rooneyandshadows.lightbulb.commons.utils.ParcelUtils

class ActivityRouterBackStack() : Parcelable {
    private var stack: MutableList<String>

    init {
        stack = mutableListOf()
    }

    constructor(parcel: Parcel) : this() {
        stack = ParcelUtils.readStringList(parcel)!!
    }

    fun getEntriesCount(): Int {
        return stack.size
    }

    fun add(screenName: String) {
        stack.add(screenName)
    }

    fun pop(): String? {
        if (stack.isEmpty())
            return null
        return stack.removeLast()
    }

    fun getCurrent(): String? {
        val last = stack.size - 1
        return getAt(last)
    }

    fun getAt(position: Int): String? {
        return stack.getOrNull(position)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        ParcelUtils.writeStringArrayList(parcel, ArrayList(stack))
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ActivityRouterBackStack> {
        override fun createFromParcel(parcel: Parcel): ActivityRouterBackStack {
            return ActivityRouterBackStack(parcel)
        }

        override fun newArray(size: Int): Array<ActivityRouterBackStack?> {
            return arrayOfNulls(size)
        }
    }
}