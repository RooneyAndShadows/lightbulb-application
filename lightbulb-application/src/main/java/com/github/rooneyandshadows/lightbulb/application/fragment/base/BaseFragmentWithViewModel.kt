package com.github.rooneyandshadows.lightbulb.application.fragment.base

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
abstract class BaseFragmentWithViewModel<VMType : ViewModel> :
    BaseFragment() {
    protected lateinit var viewModel: VMType

    protected open fun doOnCreate(savedInstanceState: Bundle?, viewModel: VMType) {
    }

    protected open fun handleArguments(arguments: Bundle?, viewModel: VMType) {
    }

    protected open fun initializeViewModel(viewModel: VMType) {
    }

    abstract fun getViewModelClass(): Class<VMType>

    @Override
    final override fun doOnCreate(savedInstanceState: Bundle?) {
        val vmclass = getViewModelClass()
        viewModel = ViewModelProvider(this)[vmclass]
        initializeViewModel(viewModel)
        doOnCreate(savedInstanceState, viewModel)
    }

    final override fun handleArguments(arguments: Bundle?) {
        super.handleArguments(arguments)
        handleArguments(arguments, viewModel)
    }
}