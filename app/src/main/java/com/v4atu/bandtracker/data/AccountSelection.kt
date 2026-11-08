package com.v4atu.bandtracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Cuenta seleccionada (SPEC §5.1): la recordada si todavía existe; si no, la primera por nombre;
 * sin cuentas, ninguna. Es una función pura para poder probarla sin base de datos.
 */
fun resolveSelectedAccount(accounts: List<Account>, preferredId: Long?): Account? =
    accounts.firstOrNull { it.id == preferredId } ?: accounts.firstOrNull()

/** Todas las cuentas más la seleccionada, ya resuelta. */
data class AccountsState(val accounts: List<Account>, val selected: Account?)

/**
 * Une las cuentas observables con la selección recordada. «Todas las acciones operan sobre la
 * cuenta seleccionada» (SPEC §5.1), así que las pantallas comparten esta misma instancia.
 */
class AccountSelection(accounts: AccountRepository, private val store: SelectedAccountStore) {

    val state: Flow<AccountsState> = combine(accounts.accounts, store.id) { list, id ->
        AccountsState(list, resolveSelectedAccount(list, id))
    }.distinctUntilChanged()

    val selected: Flow<Account?> = state.map { it.selected }.distinctUntilChanged()

    fun select(accountId: Long) = store.set(accountId)
}
