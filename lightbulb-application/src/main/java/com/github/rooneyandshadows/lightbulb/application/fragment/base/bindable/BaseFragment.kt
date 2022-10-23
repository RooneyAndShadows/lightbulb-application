package com.github.rooneyandshadows.lightbulb.application.fragment.base.bindable

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.github.rooneyandshadows.lightbulb.application.fragment.base.BaseFragment

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
abstract class BaseFragment<VDBType : ViewDataBinding> :
    BaseFragment() {
    protected lateinit var viewBinding: VDBType

    protected open fun onViewBound(viewBinding: VDBType) {
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