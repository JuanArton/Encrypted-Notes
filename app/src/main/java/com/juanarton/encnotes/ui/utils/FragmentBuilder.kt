package com.juanarton.encnotes.ui.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction

object FragmentBuilder {
    fun build(activity: FragmentActivity, fragment: Fragment, holder: Int) {
        activity.supportFragmentManager
            .beginTransaction()
            .replace(holder, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }
}