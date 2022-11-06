package com.github.rooneyandshadows.lightbulb.application.fragment.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
abstract class BaseFragmentWithViewModelAndDataBinding<VDBType : ViewDataBinding, VMType : ViewModel> :
    BaseFragment() {
    protected lateinit var viewModel: VMType
    protected lateinit var viewBinding: VDBType

    abstract fun getViewModelClass(): Class<VMType>

    protected open fun doOnCreate(savedInstanceState: Bundle?, viewModel: VMType) {
    }

    protected open fun handleArguments(arguments: Bundle?, viewModel: VMType) {
    }

    protected open fun initializeViewModel(viewModel: VMType) {
    }

    protected open fun doOnViewBound(viewBinding: VDBType) {
    }

    @Override
    final override fun handleArguments(arguments: Bundle?) {
        super.handleArguments(arguments)
        handleArguments(arguments, viewModel)
    }

    @Override
    final override fun doOnCreate(savedInstanceState: Bundle?) {
        val vmclass = getViewModelClass()
        viewModel = ViewModelProvider(this)[vmclass]
        initializeViewModel(viewModel)
        doOnCreate(savedInstanceState, viewModel)
    }

    @Override
    final override fun doOnCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = DataBindingUtil.inflate(
            inflater,
            layoutIdentifier,
            container,
            false
        )
        viewBinding.lifecycleOwner = viewLifecycleOwner
        doOnViewBound(viewBinding)
        return viewBinding.root
    }
}