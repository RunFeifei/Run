package com.uestc.run.basebase

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer


/**
 * Created by PengFeifei on 2018/10/27.
 */

abstract class BaseFragment<T : BaseViewModel> : Fragment(), IBaseView {
    protected lateinit var viewModel: T
    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val result = super.onCreateView(inflater, container, savedInstanceState)

        viewModel = initViewModel()
        progressDialog = initProgressDialog()
        viewModel.apiLoading.observe(this, Observer<Boolean> {
            if (it == null) {
                return@Observer
            }
            if (it) showLoading() else hideLoading()
        })

        viewModel.toastLiveData.observe(this, Observer<String> {
            it?.apply {
                showToast(it)
            }
        })

        viewModel.apiException.observe(this, Observer<Throwable> { throwable ->
            if (throwable == null) {
                return@Observer
            }
            if (handleException(throwable)) {
                return@Observer
            }
            //TODO
        })
        return result
    }

    private fun initProgressDialog(): ProgressDialog {
        val progressDialog = ProgressDialog(context)
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(false)
        return progressDialog
    }

    protected abstract fun initViewModel(): T

    /** */

    override fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    override fun hideLoading() {
        (activity as BaseActivity<*>).hideLoading()
    }

    override fun showLoading() {
        (activity as BaseActivity<*>).showLoading()
    }

    override fun handleLogin() {
        (activity as BaseActivity<*>).handleLogin()
    }

    override fun handleException(throwable: Throwable): Boolean {
        Log.e("BaseViewModel--> ", throwable?.toString() ?: "did not get detail exception")
        return false
    }


}
