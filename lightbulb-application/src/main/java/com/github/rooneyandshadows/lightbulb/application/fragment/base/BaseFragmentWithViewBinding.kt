package com.github.rooneyandshadows.lightbulb.application.fragment.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
abstract class BaseFragmentWithViewBinding<VDBType : ViewDataBinding> :
    BaseFragment() {
    protected lateinit var viewBinding: VDBType

    protected open fun doOnViewBound(viewBinding: VDBType, savedInstanceState: Bundle?) {
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