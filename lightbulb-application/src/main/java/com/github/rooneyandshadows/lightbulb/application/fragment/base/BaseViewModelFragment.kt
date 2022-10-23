package com.github.rooneyandshadows.lightbulb.application.fragment.base

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
abstract class BaseViewModelFragment<VMType : ViewModel> :
    BaseFragment() {
    protected lateinit var viewModel: VMType

    protected open fun create(savedInstanceState: Bundle?, viewModel: VMType) {
    }

    protected open fun handleArguments(arguments: Bundle?, viewModel: VMType) {
    }

    abstract fun getViewModelClass(): Class<VMType>

    @Override
    final override fun create(savedInstanceState: Bundle?) {
        val vmclass = getViewModelClass()
        viewModel = ViewModelProvider(contextActivity)[vmclass]
        create(savedInstanceState, viewModel)
    }

    final override fun handleArguments(arguments: Bundle?) {
        super.handleArguments(arguments)
        handleArguments(arguments, viewModel)
    }
}