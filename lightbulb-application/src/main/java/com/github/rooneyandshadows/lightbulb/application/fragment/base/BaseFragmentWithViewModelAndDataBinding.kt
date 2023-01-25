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

    protected open fun doOnCreate(savedInstanceState: Bundle?, viewModel: VMType) {
    }

    protected open fun handleArguments(arguments: Bundle?, viewModel: VMType) {
    }

    protected open fun initializeViewModel(viewModel: VMType, savedInstanceState: Bundle?) {
    }

    protected open fun doOnViewBound(viewBinding: VDBType, savedInstanceState: Bundle?) {
    }

    @Override
    final override fun handleArguments(arguments: Bundle?) {
        super.handleArguments(arguments)
        handleArguments(arguments, viewModel)
    }

    @Override
    final override fun doOnCreate(savedInstanceState: Bundle?) {
        val obj = Object()
        val clz = obj::class.java as Class<VMType>
        viewModel = ViewModelProvider(this)[clz]
        initializeViewModel(viewModel, savedInstanceState)
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
        doOnViewBound(viewBinding, savedInstanceState)
        return viewBinding.root
    }
}