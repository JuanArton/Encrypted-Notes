package com.juanarton.privynote.ui.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction

object FragmentBuilder {
    fun build(activity: FragmentActivity, fragment: Fragment, holder: Int) {
        activity.supportFragmentManager
            .beginTransaction()
            .add(holder, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    fun destroyFragment(activity: FragmentActivity, fragment: Fragment) {
        val fragmentManager = activity.supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        if (fragment.isAdded) {
            transaction.remove(fragment)
                .commit()
        }
    }
}