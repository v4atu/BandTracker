package com.v4atu.bandtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.v4atu.bandtracker.AppContainer
import com.v4atu.bandtracker.domain.EntryType
import com.v4atu.bandtracker.ui.accounts.AccountsRoute
import com.v4atu.bandtracker.ui.accounts.CreateAccountRoute
import com.v4atu.bandtracker.ui.entry.EntryFormMode
import com.v4atu.bandtracker.ui.entry.EntryFormRoute
import com.v4atu.bandtracker.ui.help.HelpRoute
import com.v4atu.bandtracker.ui.help.LicensesRoute
import com.v4atu.bandtracker.ui.history.HistoryScreen
import com.v4atu.bandtracker.ui.history.HistoryViewModel
import com.v4atu.bandtracker.ui.home.HomeScreen
import com.v4atu.bandtracker.ui.home.HomeViewModel
import com.v4atu.bandtracker.ui.home.SavedNotice

/** Grafo de navegación de la app. Las pantallas que aún no existen se conectan en su propia parte. */
@Composable
fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Home) {
        composable<Home> { entry ->
            val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
            val state by viewModel.state.collectAsStateWithLifecycle()
            val saved by entry.savedStateHandle
                .getStateFlow<String?>(ResultKeys.SAVED_NOTICE, null)
                .collectAsStateWithLifecycle()

            HomeScreen(
                state = state,
                notice = saved?.let { name -> SavedNotice.entries.firstOrNull { it.name == name } },
                onNoticeShown = { entry.savedStateHandle[ResultKeys.SAVED_NOTICE] = null },
                onSelectAccount = viewModel::select,
                onIncome = { navController.navigate(Capture(EntryType.INCOME)) },
                onExpense = { navController.navigate(Capture(EntryType.EXPENSE)) },
                onHistory = { navController.navigate(History) },
                onAccounts = { navController.navigate(Accounts) },
                onCreateAccount = { navController.navigate(CreateAccount) },
                onHelp = { navController.navigate(Help) },
            )
        }

        composable<Capture> { entry ->
            val type = entry.toRoute<Capture>().type
            EntryFormRoute(container, EntryFormMode.Create(type)) {
                // Al guardar se vuelve a Inicio, que muestra el aviso breve (SPEC §5.2).
                val notice = if (type == EntryType.INCOME) SavedNotice.Income else SavedNotice.Expense
                navController.previousBackStackEntry?.savedStateHandle?.set(ResultKeys.SAVED_NOTICE, notice.name)
                navController.popBackStack()
            }
        }

        composable<History> {
            val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
            val state by viewModel.state.collectAsStateWithLifecycle()
            // Al volver de editar o borrar un registro la lista se vuelve a leer.
            LifecycleResumeEffect(Unit) {
                viewModel.reload()
                onPauseOrDispose {}
            }
            HistoryScreen(
                state = state,
                onEntryClick = { navController.navigate(EditEntry(it)) },
                onLoadMore = viewModel::loadMore,
            )
        }

        composable<Help> {
            HelpRoute(onOpenLicenses = { navController.navigate(Licenses) })
        }

        composable<Licenses> {
            LicensesRoute()
        }

        composable<Accounts> {
            AccountsRoute(container, onCreate = { navController.navigate(CreateAccount) })
        }

        composable<CreateAccount> {
            CreateAccountRoute(container, onFinished = { navController.popBackStack() })
        }

        composable<EditEntry> { entry ->
            EntryFormRoute(container, EntryFormMode.Edit(entry.toRoute<EditEntry>().entryId)) {
                navController.popBackStack()
            }
        }
    }
}
