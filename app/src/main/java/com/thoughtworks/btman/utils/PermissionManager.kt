package com.thoughtworks.btman.utils

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionManager {
    private val PERMISSION_MANAGER_PERMISSIONS = 10009

    private val permissionList = mutableListOf<String>()
    private val requestCallbackList = mutableListOf<() -> Unit>()
    private var requestFailCallback: (() -> Unit?)? = null

    fun addRequestPermission(permission: String) {
        permissionList.add(permission)
    }

    fun addRequestPermissions(permissions: List<String>) {
        permissionList.addAll(permissions)
    }

    fun addRequestCallback(requestCallback: () -> Unit) {
        requestCallbackList.add(requestCallback)
    }

    fun removeRequestCallback(requestCallback: () -> Unit) {
        requestCallbackList.remove(requestCallback)
    }

    fun setRequestFailCallback(requestFailCallback: () -> Unit) {
        PermissionManager.requestFailCallback = requestFailCallback
    }

    fun checkPermissionsAlreadyGot(activity: Activity): Boolean {
        return permissionList.firstOrNull { ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED } == null
    }

    fun requestPermissions(activity: Activity) {
        if (permissionList.firstOrNull {
                ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            } != null) {

            if (permissionList.firstOrNull {
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
                } != null) {
            } else {
                ActivityCompat.requestPermissions(activity, permissionList.toTypedArray(),
                    PERMISSION_MANAGER_PERMISSIONS
                )
            }
        } else {
            requestCallback()
        }
    }

    private fun requestCallback() {
        requestCallbackList.forEach { it() }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == PERMISSION_MANAGER_PERMISSIONS && grantResults.isNotEmpty()) {
            if (grantResults.firstOrNull { it != PackageManager.PERMISSION_GRANTED } != null) {
                requestFailCallback?.let { it() }
            } else {
                requestCallback()
            }
        } else {
            requestFailCallback?.let { it() }
        }
    }
}