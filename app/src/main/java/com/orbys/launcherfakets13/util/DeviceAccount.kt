package com.orbys.launcherfakets13.util

import android.accounts.AccountManager
import android.content.Context

data class DeviceAccount(
    val name: String,
    val type: String
)

object DeviceAccountUtil {

    fun getDeviceAccounts(context: Context): List<DeviceAccount> {
        val accountManager = AccountManager.get(context)
        return accountManager.accounts
            .map { account -> DeviceAccount(name = account.name, type = account.type) }
    }

    // Si quieres filtrar por tipo (ej: solo Google)
    fun getAccountsByType(context: Context, type: String): List<DeviceAccount> {
        val accountManager = AccountManager.get(context)
        return accountManager.getAccountsByType(type)
            .map { account -> DeviceAccount(name = account.name, type = account.type) }
    }

    fun hasMicrosoftAccount(context: Context): Boolean {
        val accountManager = AccountManager.get(context)
        val microsoftTypes = listOf(
            "com.microsoft.workaccount",       // Office 365 / Entra ID (Teams, OneDrive, Outlook corp)
            "com.microsoft.exchangeactivesync",// Exchange ActiveSync
            "com.microsoft.outlook.email",     // Outlook personal
            "live.com",                        // Microsoft personal (Hotmail, Live)
            "com.microsoft"                    // Genérico Microsoft
        )
        return microsoftTypes.any { type ->
            accountManager.getAccountsByType(type).isNotEmpty()
        }
    }
}