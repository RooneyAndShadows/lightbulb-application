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

    protected open fun create(savedInstanceState: Bundle?, viewModel: VMType) {
    }

    protected open fun handleArguments(arguments: Bundle?, viewModel: VMType) {
    }

    protected open fun onViewBound(viewBinding: VDBType) {
    }

    @Override
    final override fun handleArguments(arguments: Bundle?) {
        super.handleArguments(arguments)
        handleArguments(arguments, viewModel)
    }

    @Override
    final override fun create(savedInstanceState: Bundle?) {
        val vmclass = getViewModelClass()
        viewModel = ViewModelProvider(contextActivity)[vmclass]
        create(savedInstanceState, viewModel)
    }

    @Override
    final override fun createView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = DataBindingUtil.inflate(
            inflater,
            getLayoutId(),
            container,
            false
        )
        viewBinding.lifecycleOwner = viewLifecycleOwner
        onViewBound(viewBinding)
        return viewBinding.root
    }
}