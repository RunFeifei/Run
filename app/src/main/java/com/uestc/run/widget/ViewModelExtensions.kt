package com.uestc.run.widget

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders


fun <T : ViewModel> Fragment.get(clazz: Class<T>): T {
    return ViewModelProviders.of(this).get(clazz)
}

fun <T : ViewModel> FragmentActivity.get(clazz: Class<T>): T {
    return ViewModelProviders.of(this).get(clazz)
}
