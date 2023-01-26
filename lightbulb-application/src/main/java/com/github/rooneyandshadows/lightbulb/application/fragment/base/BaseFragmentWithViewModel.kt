package com.github.rooneyandshadows.lightbulb.application.fragment.base

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
abstract class BaseFragmentWithViewModel<VMType : ViewModel> : BaseFragment() {
    protected lateinit var viewModel: VMType
    protected abstract val viewModelClass: Class<VMType>

    protected open fun doOnCreate(savedInstanceState: Bundle?, viewModel: VMType) {
    }

    protected open fun handleArguments(arguments: Bundle?, viewModel: VMType) {
    }

    protected open fun initializeViewModel(viewModel: VMType, savedInstanceState: Bundle?) {
    }

    @Override
    final override fun doOnCreate(savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[viewModelClass]
        initializeViewModel(viewModel, savedInstanceState)
        doOnCreate(savedInstanceState, viewModel)
    }

    @Override
    final override fun handleArguments(arguments: Bundle?) {
        super.handleArguments(arguments)
        handleArguments(arguments, viewModel)
    }
}